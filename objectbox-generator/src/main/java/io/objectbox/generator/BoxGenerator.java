/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.objectbox.generator;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import io.objectbox.generator.model.Entity;
import io.objectbox.generator.model.Property;
import io.objectbox.generator.model.Schema;
import io.objectbox.generator.model.ToManyBase;
import io.objectbox.generator.model.ToManyStandalone;
import io.objectbox.generator.model.ToOne;

/**
 * Once you have your model created, use this class to generate box cursors as required by ObjectBox.
 */
public class BoxGenerator {

    public static final String MYOBJECTBOX_FTL = "myobjectbox.ftl";
    public static final String BASE_PACKAGE_PATH = "/io/objectbox/generator/";

    private final Template templateMyObjectBox;
    private final Template templateCursor;
    private final Template templateEntityInfo;
    private final Template templateFlatbuffersSchema;
    // For DAOcompat
    private final Template templateDao;
    private final Template templateDaoSession;

    public BoxGenerator() throws IOException {
        log("ObjectBox Generator");
        log("Copyright 2017-2025 ObjectBox Ltd, objectbox.io. Licensed under GNU Affero General Public License, Version 3.");
        log("This program comes with ABSOLUTELY NO WARRANTY");

        Configuration config = getConfiguration(MYOBJECTBOX_FTL);
        templateMyObjectBox = config.getTemplate(MYOBJECTBOX_FTL);
        templateCursor = config.getTemplate("cursor.ftl");
        templateEntityInfo = config.getTemplate("entity-info.ftl");
        templateFlatbuffersSchema = config.getTemplate("flatbuffers-schema.ftl");
        // For DAOcompat
        templateDao = config.getTemplate("dao.ftl");
        templateDaoSession = config.getTemplate("dao-session.ftl");
    }

    private Configuration getConfiguration(String probingTemplate) throws IOException {
        Configuration config = new Configuration(Configuration.VERSION_2_3_25);
        config.setClassForTemplateLoading(getClass(), BASE_PACKAGE_PATH);

        try {
            config.getTemplate(probingTemplate);
        } catch (TemplateNotFoundException e) {
            // When running from an IDE like IntelliJ, class loading resources may fail for some reason (Gradle is OK)

            // Working dir is module dir
            File dir = new File("../../objectbox-generator/src/main/resources/" + BASE_PACKAGE_PATH);
            if (!dir.exists()) {
                // Working dir is base module dir
                dir = new File("objectbox-generator/src/main/resources/" + BASE_PACKAGE_PATH);
            }
            if (dir.exists() && new File(dir, probingTemplate).exists()) {
                config.setDirectoryForTemplateLoading(dir);
                config.getTemplate(probingTemplate);
            } else {
                throw e;
            }
        }
        return config;
    }

    /** Generates all classes and other artifacts for the given job. Assumes the given schema is finished. */
    public void generateAll(GeneratorJob job) throws Exception {
        long start = System.currentTimeMillis();

        Schema schema = job.getSchema();
        if (!schema.isFinished()) {
            throw new IllegalStateException("Must call schema.finish() first");
        }

        log("Processing schema version " + schema.getVersion() + "...");

        List<Entity> entities = schema.getEntities();
        for (Entity entity : entities) {
            Map<String, Object> extras = createExtrasForCursor(entity);
            generate(templateCursor, job, entity.getJavaPackageDao(), entity.getClassNameDao(), entity, extras);
            generate(templateEntityInfo, job, entity.getJavaPackageDao(), entity.getClassName() + "_",
                    entity, createExtrasForEntityInfo(entity));
        }
        if (job.getOutputFlatbuffersSchema() != null) {
            generate(templateFlatbuffersSchema, job.getOutputFlatbuffersSchema(), "", "flatbuffers", ".fbs",
                    job.getSchema(), null, null);
        }
        generate(templateMyObjectBox, job, schema.getDefaultJavaPackageDao(),
                "My" + schema.getPrefix() + "ObjectBox", null, createExtrasForMyObjectBox(schema));

        if (job.isDaoCompat()) {
            // generate DAO classes
            for (Entity entity : entities) {
                // change Dao class name
                entity.setClassNameDao(entity.getClassName() + "Dao");

                generate(templateDao, job, entity.getJavaPackageDao(), entity.getClassNameDao(), entity);
            }
            generate(templateDaoSession, job, schema.getDefaultJavaPackageDao(), schema.getPrefix() + "DaoSession",
                    null);
        }

        long time = System.currentTimeMillis() - start;
        log("Processed " + entities.size() + " entities in " + time + "ms");
    }

    /**
     * Builds a sorted set of imports, returns it mapped as 'imports'.
     */
    private Map<String, Object> createExtrasForMyObjectBox(Schema schema) {
        Set<String> imports = new TreeSet<>(); // instead of HashSet + then sorting that

        imports.add("io.objectbox.BoxStore");
        imports.add("io.objectbox.BoxStoreBuilder");
        imports.add("io.objectbox.ModelBuilder");
        imports.add("io.objectbox.ModelBuilder.EntityBuilder");
        imports.add("io.objectbox.model.PropertyFlags");
        imports.add("io.objectbox.model.PropertyType");
        // External types are optional, so only import classes if necessary
        if (hasExternalTypes(schema)) {
            imports.add("io.objectbox.model.ExternalPropertyType");
        }
        // HNSW params are optional, so only import classes if necessary
        if (hasHnswParams(schema)) {
            imports.add("io.objectbox.model.HnswFlags");
            imports.add("io.objectbox.model.HnswDistanceType");
        }

        for (Entity entity : schema.getEntities()) {
            String javaPackage = entity.getJavaPackage();
            if (!javaPackage.equals(schema.getDefaultJavaPackage())) {
                imports.add(String.format("%s.%s", javaPackage, entity.getClassName()));
            }
            String javaPackageDao = entity.getJavaPackageDao();
            if (!javaPackageDao.equals(schema.getDefaultJavaPackageDao())) {
                imports.add(String.format("%s.%s", javaPackageDao, entity.getClassNameDao()));
                imports.add(String.format("%s.%s_", javaPackageDao, entity.getClassName()));
            }
        }

        Map<String, Object> extras = new HashMap<>();
        extras.put("imports", imports);
        return extras;
    }

    /**
     * Returns if at least one property or standalone to-many relation has an external type.
     *
     * @param schema the schema to search.
     * @return {@code true} if at least one property or standalone to-many relation with a non-null
     * {@link Property#getExternalTypeExpression()} or {@link ToManyStandalone#getExternalTypeExpression()} exists.
     */
    private boolean hasExternalTypes(Schema schema) {
        for (Entity entity : schema.getEntities()) {
            for (Property property : entity.getProperties()) {
                if (property.getExternalTypeExpression() != null) return true;
            }
            for (ToManyBase toManyBase : entity.getToManyRelations()) {
                if (toManyBase instanceof ToManyStandalone) {
                    if (((ToManyStandalone) toManyBase).getExternalTypeExpression() != null) return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns if at least one property has HNSW params.
     *
     * @param schema the schema to search.
     * @return {@code true} if at least one property with HNSW params exists.
     */
    private boolean hasHnswParams(Schema schema) {
        for (Entity entity : schema.getEntities()) {
            for (Property property : entity.getProperties()) {
                if (property.hasHnswParams()) return true;
            }
        }
        return false;
    }

    /**
     * Builds a sorted set of imports, returns it mapped as 'imports'.
     * And builds collect method code, returns it mapped as 'propertyCollector'.
     */
    private Map<String, Object> createExtrasForCursor(Entity entity) {
        Set<String> imports = new TreeSet<>(); // instead of HashSet + then sorting that

        /*
        Note: Some ObjectBox classes which names are likely to conflict
        with user-defined entity classes are imported as fully qualified
        imports instead. See cursor.ftl.
        */

        imports.add("io.objectbox.BoxStore");
        imports.add("io.objectbox.Cursor");
        imports.add("io.objectbox.annotation.apihint.Internal");
        imports.add("io.objectbox.internal.CursorFactory");

        if (isNotEmpty(entity.getIncomingToManyRelations()) || isNotEmpty(entity.getToManyRelations())) {
            imports.add("java.util.List");
        }
        if (isNotEmpty(entity.getToManyRelations())) {
            imports.add("io.objectbox.relation.ToMany");
        }
        if (isNotEmpty(entity.getToOneRelations())) {
            imports.add("io.objectbox.relation.ToOne");
        }

        String javaPackage = entity.getJavaPackage();
        if (isNotEmpty(javaPackage) && !javaPackage.equals(entity.getJavaPackageDao())) {
            imports.add(String.format("%s.%s", javaPackage, entity.getClassName()));
        }

        imports.addAll(entity.getAdditionalImportsDao());

        final HashMap<String, Object> map = new HashMap<>();
        map.put("imports", imports);
        map.put("propertyCollector", new PropertyCollector(entity).createPropertyCollector());
        return map;
    }

    /**
     * Builds a sorted set of imports, returns it mapped as 'imports'.
     */
    private Map<String, Object> createExtrasForEntityInfo(Entity entity) {
        Set<String> imports = new TreeSet<>(); // instead of HashSet + then sorting that

        /*
        Note: Some ObjectBox classes which names are likely to conflict
        with user-defined entity classes are imported as fully qualified
        imports instead. See entity-info.ftl.
        */

        // note: need to check package, could be unnamed package
        String javaPackageDao = entity.getJavaPackageDao();
        if (isNotEmpty(javaPackageDao)) {
            imports.add(String.format("%s.%s.Factory", javaPackageDao, entity.getClassNameDao()));
        }

        imports.add("io.objectbox.EntityInfo");
        imports.add("io.objectbox.annotation.apihint.Internal");
        imports.add("io.objectbox.internal.CursorFactory");
        imports.add("io.objectbox.internal.IdGetter");

        if (entity.hasRelations()) {
            imports.add("io.objectbox.relation.RelationInfo");
            imports.add("io.objectbox.relation.ToOne");
            imports.add("io.objectbox.internal.ToOneGetter");
            List<ToManyBase> toManyRelations = entity.getToManyRelations();
            if (isNotEmpty(toManyRelations)) {
                imports.add("io.objectbox.internal.ToManyGetter");
                imports.add("java.util.List");
            }
            for (ToOne toOne : entity.getToOneRelations()) {
                addImportIfPackageDiffers(imports, entity, toOne.getTargetEntity());
            }
            for (ToManyBase toMany : toManyRelations) {
                addImportIfPackageDiffers(imports, entity, toMany.getTargetEntity());
            }
        }

        // for custom types only
        imports.addAll(entity.getAdditionalImportsDao());

        Map<String, Object> extras = new HashMap<>();
        extras.put("imports", imports);
        return extras;
    }

    /**
     * Adds _-class import if target entity has a package name set
     * and it is not equal to that of the entity.
     */
    private void addImportIfPackageDiffers(Set<String> imports, Entity entity, Entity targetEntity) {
        String targetPackageDao = targetEntity.getJavaPackageDao();
        if (isNotEmpty(targetPackageDao) && !targetPackageDao.equals(entity.getJavaPackageDao())) {
            imports.add(String.format("%s.%s_", targetPackageDao, targetEntity.getClassName()));
        }
    }

    private void generate(Template template, GeneratorJob job, String javaPackage, String javaClassName, Entity entity)
            throws Exception {
        generate(template, job.getOutput(), javaPackage, javaClassName, ".java", job.getSchema(), entity, null);
    }

    private void generate(Template template, GeneratorJob job, String javaPackage, String javaClassName, Entity entity,
            Map<String, Object> extrasForTemplate) throws Exception {
        generate(template, job.getOutput(), javaPackage, javaClassName, ".java", job.getSchema(), entity,
                extrasForTemplate);
    }

    private void generate(Template template, GeneratorOutput output,
            String javaPackage, String fileName, String fileExtension,
            Schema schema, Entity entity, Map<String, Object> extrasForTemplate)
            throws Exception {
        Map<String, Object> root = new HashMap<>();
        root.put("schema", schema);
        root.put("entity", entity);
        if (extrasForTemplate != null) {
            root.putAll(extrasForTemplate);
        }
        String filePath = javaPackage + "." + fileName + fileExtension;
        try {
            try (Writer writer = output.createWriter(javaPackage, fileName, fileExtension)) {
                template.process(root, writer);
                writer.flush();
            }
            log("Written " + filePath);
        } catch (Exception ex) {
            System.err.println("Data map for template: " + root);
            System.err.println("Error while generating " + filePath);
            throw ex;
        }
    }

    private boolean isNotEmpty(String value) {
        return value != null && value.length() > 0;
    }

    private boolean isNotEmpty(List list) {
        return list != null && !list.isEmpty();
    }

    private static void log(String message) {
        System.out.println("[ObjectBox] " + message);
    }
}
