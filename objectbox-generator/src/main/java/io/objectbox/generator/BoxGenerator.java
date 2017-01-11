/*
 * Copyright (C) 2016 Markus Junginger, greenrobot (http://greenrobot.org)
 *
 * This file is part of ObjectBox Generator.
 *
 * ObjectBox Generator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Generator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Generator.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.generator;

import org.greenrobot.greendao.generator.DaoUtil;
import org.greenrobot.greendao.generator.Entity;
import org.greenrobot.greendao.generator.InternalAccess;
import org.greenrobot.greendao.generator.PropertyType;
import org.greenrobot.greendao.generator.Schema;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

/**
 * Once you have your model created, use this class to generate box cursors as required by ObjectBox.
 *
 * @author Markus
 */
public class BoxGenerator {

    public static final String MYOBJECTBOX_FTL = "myobjectbox.ftl";

    private final Pattern patternKeepIncludes;
    private final Pattern patternKeepFields;
    private final Pattern patternKeepMethods;

    private final Template templateMyObjectBox;
    private final Template templateCursor;
    private final Template templateDao;
    private final Template templateDaoSession;
    private final Template templateEntity;
    private final Template templateProperties;
    private final Template templateBoxUnitTest;

    private boolean daoCompat;

    public BoxGenerator() throws IOException {
        this(false);
    }

    public BoxGenerator(boolean daoCompat) throws IOException {
        System.out.println("ObjectBox Generator");
        System.out.println("Copyright 2016 Markus Junginger, greenrobot.org. Licensed under GPL V3.");
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
        templateProperties = config.getTemplate("properties.ftl");
        templateBoxUnitTest = config.getTemplate("box-unit-test.ftl");
    }

    private Configuration getConfiguration(String probingTemplate) throws IOException {
        Configuration config = new Configuration(Configuration.VERSION_2_3_23);
        config.setClassForTemplateLoading(getClass(), "/");

        Template templateMyObjectBoxLocal;
        try {
            templateMyObjectBoxLocal = config.getTemplate(probingTemplate);
        } catch (TemplateNotFoundException e) {
            // When running from an IDE like IntelliJ, class loading resources may fail for some reason (Gradle is OK)

            // Working dir is module dir
            File dir = new File("../../objectbox-generator/src/main/resources/");
            if (!dir.exists()) {
                // Working dir is base module dir
                dir = new File("objectbox-generator/src/main/resources/");
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
        generateAll(schema, outDir, null, null);
    }

    /** Generates all entities and DAOs for the given schema. */
    public void generateAll(Schema schema, String outDir, String outDirEntity, String outDirTest) throws Exception {
        long start = System.currentTimeMillis();

        File outDirFile = toFileForceExists(outDir);
        File outDirEntityFile = outDirEntity != null ? toFileForceExists(outDirEntity) : outDirFile;
        File outDirTestFile = outDirTest != null ? toFileForceExists(outDirTest) : null;

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
            generate(templateCursor, outDirFile, entity.getJavaPackageDao(), entity.getClassNameDao(), schema, entity,
                    additionalData);
            if (!entity.isProtobuf() && !entity.isSkipGeneration()) {
                generate(templateEntity, outDirEntityFile, entity.getJavaPackage(), entity.getClassName(), schema, entity);
            }
            generate(templateProperties, outDirFile, entity.getJavaPackageDao(), entity.getClassName() + "_",
                    schema, entity);
            if (outDirTestFile != null && !entity.isSkipGenerationTest()) {
                String javaPackageTest = entity.getJavaPackageTest();
                String classNameTest = entity.getClassNameTest();
                File javaFilename = toJavaFilename(outDirTestFile, javaPackageTest, classNameTest);
                if (!javaFilename.exists()) {
                    generate(templateBoxUnitTest, outDirTestFile, javaPackageTest, classNameTest, schema, entity);
                } else {
                    System.out.println("Skipped " + javaFilename.getCanonicalPath());
                }
            }
        }
        generate(templateMyObjectBox, outDirFile, schema.getDefaultJavaPackageDao(),
                "My" + schema.getPrefix() + "ObjectBox", schema, null);

        if (daoCompat) {
            // generate DAO classes
            for (Entity entity : entities) {
                // change Dao class name
                entity.setClassNameDao(entity.getClassName() + "Dao");

                generate(templateDao, outDirFile, entity.getJavaPackageDao(), entity.getClassNameDao(), schema, entity);
            }
            generate(templateDaoSession, outDirFile, schema.getDefaultJavaPackageDao(),
                    schema.getPrefix() + "DaoSession", schema, null);
        }

        long time = System.currentTimeMillis() - start;
        System.out.println("Processed " + entities.size() + " entities in " + time + "ms");
    }

    private Map<PropertyType, String> propertyToDbTypes() {
        Map<PropertyType, String> map = new HashMap<>();
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
        return map;
    }

    private Map<String, Object> createAdditionalDataForCursor(Schema schema, Entity entity) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("propertyCollector", new PropertyCollector(entity).createPropertyCollector());
        return map;
    }

    protected File toFileForceExists(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException(String.format("%s does not exist. " +
                            "This check is to prevent accidental file generation into a wrong path. (resolved path=%s)",
                    filename, file.getAbsolutePath()));
        }
        return file;
    }

    private void generate(Template template, File outDirFile, String javaPackage, String javaClassName, Schema schema,
                          Entity entity) throws Exception {
        generate(template, outDirFile, javaPackage, javaClassName, schema, entity, null);
    }

    private void generate(Template template, File outDirFile, String javaPackage, String javaClassName, Schema schema,
                          Entity entity, Map<String, Object> additionalObjectsForTemplate) throws Exception {
        Map<String, Object> root = new HashMap<>();
        root.put("schema", schema);
        root.put("entity", entity);
        if (additionalObjectsForTemplate != null) {
            root.putAll(additionalObjectsForTemplate);
        }
        try {
            File file = toJavaFilename(outDirFile, javaPackage, javaClassName);
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();

            if (entity != null && entity.getHasKeepSections()) {
                checkKeepSections(file, root);
            }

            Writer writer = new FileWriter(file);
            try {
                template.process(root, writer);
                writer.flush();
                System.out.println("Written " + file.getCanonicalPath());
            } finally {
                writer.close();
            }
        } catch (Exception ex) {
            System.err.println("Data map for template: " + root);
            System.err.println("Error while generating " + javaPackage + "." + javaClassName + " ("
                    + outDirFile.getCanonicalPath() + ")");
            throw ex;
        }
    }

    private void checkKeepSections(File file, Map<String, Object> root) {
        if (file.exists()) {
            try {
                String contents = new String(DaoUtil.readAllBytes(file));

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

    protected File toJavaFilename(File outDirFile, String javaPackage, String javaClassName) {
        String packageSubPath = javaPackage.replace('.', '/');
        File packagePath = new File(outDirFile, packageSubPath);
        return new File(packagePath, javaClassName + ".java");
    }

}
