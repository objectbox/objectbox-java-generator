/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.generator;

import org.greenrobot.essentials.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.Filer;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import io.objectbox.generator.model.Entity;
import io.objectbox.generator.model.InternalAccess;
import io.objectbox.generator.model.PropertyType;
import io.objectbox.generator.model.Schema;

/**
 * Once you have your model created, use this class to generate box cursors as required by ObjectBox.
 *
 * @author Markus
 */
public class BoxGenerator {

    public static final String MYOBJECTBOX_FTL = "myobjectbox.ftl";
    public static final String BASE_PACKAGE_PATH = "/io/objectbox/generator/";

    private final Pattern patternKeepIncludes;
    private final Pattern patternKeepFields;
    private final Pattern patternKeepMethods;

    private final Template templateMyObjectBox;
    private final Template templateCursor;
    private final Template templateDao;
    private final Template templateDaoSession;
    private final Template templateEntity;
    private final Template templateEntityInfo;
    private final Template templateBoxUnitTest;

    private boolean daoCompat;

    public BoxGenerator() throws IOException {
        this(false);
    }

    public BoxGenerator(boolean daoCompat) throws IOException {
        System.out.println("ObjectBox Generator");
        System.out.println("Copyright 2017 Markus Junginger, objectbox.io. Licensed under GPL V3.");
        System.out.println("This program comes with ABSOLUTELY NO WARRANTY");

        this.daoCompat = daoCompat;

        patternKeepIncludes = compilePattern("INCLUDES");
        patternKeepFields = compilePattern("FIELDS");
        patternKeepMethods = compilePattern("METHODS");

        Configuration config = getConfiguration(MYOBJECTBOX_FTL);
        templateMyObjectBox = config.getTemplate(MYOBJECTBOX_FTL);
        templateCursor = config.getTemplate("cursor.ftl");
        templateDao = config.getTemplate("dao.ftl");
        templateDaoSession = config.getTemplate("dao-session.ftl");
        templateEntity = config.getTemplate("entity.ftl");
        templateEntityInfo = config.getTemplate("entity-info.ftl");
        templateBoxUnitTest = config.getTemplate("box-unit-test.ftl");
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

    private Pattern compilePattern(String sectionName) {
        int flags = Pattern.DOTALL | Pattern.MULTILINE;
        return Pattern.compile(".*^\\s*?//\\s*?KEEP " + sectionName + ".*?\n(.*?)^\\s*// KEEP " + sectionName
                + " END.*?\n", flags);
    }

    /** Generates all entities and DAOs for the given schema. */
    public void generateAll(Schema schema, String outDir) throws Exception {
        generateAll(schema, new GeneratorOutput(outDir), null);
    }

    /** Generates all entities and DAOs for the given schema. */
    public void generateAll(Schema schema, GeneratorOutput output, GeneratorOutput outputTest) throws Exception {
        long start = System.currentTimeMillis();

        List<Entity> entities = schema.getEntities();
        for (Entity entity : entities) {
            if (entity.getClassNameDao() == null) {
                entity.setClassNameDao(entity.getClassName() + "Cursor");
            }
        }

        InternalAccess.setPropertyToDbType(schema, propertyToDbTypes());
        InternalAccess.init2ndAnd3rdPass(schema);

        System.out.println("Processing schema version " + schema.getVersion() + "...");

        for (Entity entity : entities) {
            Map<String, Object> additionalData = createAdditionalDataForCursor(schema, entity);
            generate(templateCursor, output, entity.getJavaPackageDao(), entity.getClassNameDao(), schema, entity,
                    additionalData);
            if (!entity.isProtobuf() && !entity.isSkipGeneration()) {
                generate(templateEntity, output, entity.getJavaPackage(), entity.getClassName(), schema, entity);
            }
            generate(templateEntityInfo, output, entity.getJavaPackageDao(), entity.getClassName() + "_",
                    schema, entity);
            if (outputTest != null && !entity.isSkipGenerationTest()) {
                String javaPackageTest = entity.getJavaPackageTest();
                String classNameTest = entity.getClassNameTest();
                File testFile = outputTest.getFileOrNull(javaPackageTest, classNameTest);
                if (testFile != null && !testFile.exists()) {
                    generate(templateBoxUnitTest, outputTest, javaPackageTest, classNameTest, schema, entity);
                } else {
                    System.out.println("Skipped " + (testFile != null ? testFile.getCanonicalPath() : classNameTest));
                }
            }
        }
        generate(templateMyObjectBox, output, schema.getDefaultJavaPackageDao(),
                "My" + schema.getPrefix() + "ObjectBox", schema, null);

        if (daoCompat) {
            // generate DAO classes
            for (Entity entity : entities) {
                // change Dao class name
                entity.setClassNameDao(entity.getClassName() + "Dao");

                generate(templateDao, output, entity.getJavaPackageDao(), entity.getClassNameDao(), schema, entity);
            }
            generate(templateDaoSession, output, schema.getDefaultJavaPackageDao(),
                    schema.getPrefix() + "DaoSession", schema, null);
        }

        long time = System.currentTimeMillis() - start;
        System.out.println("Processed " + entities.size() + " entities in " + time + "ms");
    }

    public void generateAll(Schema schema, Filer filer) throws Exception {
        generateAll(schema, new GeneratorOutput(filer), null);
    }

    private Map<PropertyType, String> propertyToDbTypes() {
        Map<PropertyType, String> map = new EnumMap<>(PropertyType.class);
        map.put(PropertyType.Boolean, "Bool");
        map.put(PropertyType.Byte, "Byte");
        map.put(PropertyType.Short, "Short");
        map.put(PropertyType.Int, "Int");
        map.put(PropertyType.Long, "Long");
        map.put(PropertyType.Float, "Float");
        map.put(PropertyType.Double, "Double");
        map.put(PropertyType.String, "String");
        map.put(PropertyType.ByteArray, "ByteVector");
        map.put(PropertyType.Date, "Date");
        map.put(PropertyType.RelationId, "Relation");
        return map;
    }

    private Map<String, Object> createAdditionalDataForCursor(Schema schema, Entity entity) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("propertyCollector", new PropertyCollector(entity).createPropertyCollector());
        return map;
    }


    private void generate(Template template, GeneratorOutput output, String javaPackage, String javaClassName, Schema schema,
                          Entity entity) throws Exception {
        generate(template, output, javaPackage, javaClassName, schema, entity, null);
    }

    private void generate(Template template, GeneratorOutput output, String javaPackage, String javaClassName, Schema schema,
                          Entity entity, Map<String, Object> additionalObjectsForTemplate) throws Exception {
        Map<String, Object> root = new HashMap<>();
        root.put("schema", schema);
        root.put("entity", entity);
        if (additionalObjectsForTemplate != null) {
            root.putAll(additionalObjectsForTemplate);
        }
        String filePath = javaPackage + "." + javaClassName;
        try {
            File file = output.getFileOrNull(javaPackage, javaClassName);
            if (file != null) {
                filePath = file.getCanonicalPath();
                if (entity != null && entity.getHasKeepSections()) {
                    checkKeepSections(file, root);
                }
            }

            Writer writer = output.createWriter(javaPackage, javaClassName);
            try {
                template.process(root, writer);
                writer.flush();
            } finally {
                writer.close();
            }
            System.out.println("Written " + filePath);
        } catch (Exception ex) {
            System.err.println("Data map for template: " + root);
            System.err.println("Error while generating " + filePath);
            throw ex;
        }
    }

    /** Not really useful at the moment, future version may drop this completely. */
    private void checkKeepSections(File file, Map<String, Object> root) {
        if (file.exists()) {
            try {
                String contents = FileUtils.readUtf8(file);
                Matcher matcher;
                matcher = patternKeepIncludes.matcher(contents);
                if (matcher.matches()) {
                    root.put("keepIncludes", matcher.group(1));
                }

                matcher = patternKeepFields.matcher(contents);
                if (matcher.matches()) {
                    root.put("keepFields", matcher.group(1));
                }

                matcher = patternKeepMethods.matcher(contents);
                if (matcher.matches()) {
                    root.put("keepMethods", matcher.group(1));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
