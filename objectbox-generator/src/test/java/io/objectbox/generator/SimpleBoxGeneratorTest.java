/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2024 ObjectBox Ltd.
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

import org.greenrobot.essentials.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import io.objectbox.generator.model.Entity;
import io.objectbox.generator.model.Index;
import io.objectbox.generator.model.Property;
import io.objectbox.generator.model.PropertyType;
import io.objectbox.generator.model.Schema;
import io.objectbox.generator.model.ToOne;


import static org.junit.Assert.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class SimpleBoxGeneratorTest {

    @Test
    public void testMinimalSchema() throws Exception {
        Schema schema = new Schema(1, "io.objectbox.test.minimalbox");
        Entity miniEntity = schema.addEntity("MiniBox");
        miniEntity.addIdProperty();
        miniEntity.addProperty(PropertyType.Int, "count").index();
        miniEntity.addProperty(PropertyType.Int, "dummy");
        assertEquals(1, schema.getEntities().size());
        assertEquals(3, miniEntity.getProperties().size());

        File outputDir = new File("build/test-out");
        outputDir.mkdirs();

        String baseName = "io/objectbox/test/minimalbox/" + miniEntity.getClassName();
        File cursorFile = fileDeleteIfExists(outputDir, baseName + "Cursor.java");
        File entityInfoFile = fileDeleteIfExists(outputDir, baseName + "_.java");

        assignMissingIdsUids(schema);
        schema.finish();
        new BoxGenerator().generateAll(jobForFileForceExists(schema, outputDir));

        // Assert Cursor file
        assertTrue(cursorFile.toString(), cursorFile.exists());
        final String cursorContent = FileUtils.readUtf8(cursorFile);
        assertContains(cursorContent, miniEntity.getClassName() + "Cursor");
        assertContains(cursorContent, "long put(" + miniEntity.getClassName() + " entity)");
        assertContains(cursorContent, "PUT_FLAG_FIRST | PUT_FLAG_COMPLETE");
        final String cursorContentLower = cursorContent.toLowerCase();
        //assertEquals(-1, cursorContentLower.indexOf("greendao"));
        assertEquals(-1, cursorContentLower.indexOf("table"));
        assertEquals(-1, cursorContentLower.indexOf("sql"));

        // Assert Properties file
        assertTrue(entityInfoFile.toString(), entityInfoFile.exists());
        final String propertiesContent = FileUtils.readUtf8(entityInfoFile);
        assertContains(propertiesContent, "class " + miniEntity.getClassName() + "_");
        assertContains(propertiesContent, "__ALL_PROPERTIES");
        assertContains(propertiesContent, "getAllProperties()");
    }

    private File fileDeleteIfExists(File outputDir, String fileName) {
        File file = new File(outputDir, fileName);
        file.delete();
        assertFalse(file.exists());
        return file;
    }

    private GeneratorJob jobForFileForceExists(Schema schema, File outDir) throws Exception {
        return new GeneratorJob(schema, GeneratorOutput.create(outDir.getPath()));
    }

    /**
     * Assigns required IDs and UIDs that were not explicitly assigned when building the test schema.
     */
    private void assignMissingIdsUids(Schema schema) {
        int id = 1;
        long uid = 1000;
        for (Entity entity : schema.getEntities()) {
            if (entity.getModelId() == null) {
                entity.setModelId(id++);
            }
            if (entity.getModelUid() == null) {
                entity.setModelUid(uid++);
            }
            for (Property property : entity.getProperties()) {
                if (property.getModelId() == null) {
                    property.setModelId(new IdUid(id++, uid++));
                }
                if (property.getModelIndexId() == null) {
                    for (Index index : entity.getIndexes()) {
                        if (index.getProperties().contains(property)) {
                            property.setModelIndexId(new IdUid(id++, uid++));
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testSchemaWithTwoCollects() throws Exception {
        Schema schema = new Schema(1, "io.objectbox.test.multicollect");
        Entity multiCollectEntity = schema.addEntity("MultiCollectBox");
        multiCollectEntity.addIdProperty().typeNotNull();
        multiCollectEntity.addProperty(PropertyType.Float, "foo").index();
        multiCollectEntity.addProperty(PropertyType.Float, "bar");
        multiCollectEntity.addProperty(PropertyType.Float, "box");
        multiCollectEntity.addProperty(PropertyType.Float, "in2ndCall");
        multiCollectEntity.addProperty(PropertyType.Float, "in2ndCall2");

        File outputDir = new File("build/test-out");
        outputDir.mkdirs();

        String fileNameCursor = "io/objectbox/test/multicollect/" + multiCollectEntity.getClassName() + "Cursor.java";
        File cursorFile = fileDeleteIfExists(outputDir, fileNameCursor);

        assignMissingIdsUids(schema);
        schema.finish();
        new BoxGenerator().generateAll(jobForFileForceExists(schema, outputDir));

        assertTrue(cursorFile.toString(), cursorFile.exists());
        final String cursorContent = FileUtils.readUtf8(cursorFile);
        assertContains(cursorContent, "        collect002033(cursor, 0, PUT_FLAG_FIRST,\n");
        assertContains(cursorContent, "        long __assignedId = collect002033(cursor, entity.getId(), PUT_FLAG_COMPLETE,\n");
    }

    @Test
    public void testSchemaWithTwoCollects_StringsBeforePrimitives() throws Exception {
        Schema schema = new Schema(1, "io.objectbox.test.multicollect");
        Entity multiCollectEntity = schema.addEntity("MultiCollectBox_StringsBeforePrimitives");
        multiCollectEntity.addIdProperty().typeNotNull();
        multiCollectEntity.addProperty(PropertyType.String, "string1");
        multiCollectEntity.addProperty(PropertyType.String, "string2");
        multiCollectEntity.addProperty(PropertyType.String, "string3");
        multiCollectEntity.addProperty(PropertyType.String, "string4");
        multiCollectEntity.addProperty(PropertyType.Int, "primitive").typeNotNull();

        File outputDir = new File("build/test-out");
        outputDir.mkdirs();

        String fileNameCursor = "io/objectbox/test/multicollect/" + multiCollectEntity.getClassName() + "Cursor.java";
        File cursorFile = fileDeleteIfExists(outputDir, fileNameCursor);

        assignMissingIdsUids(schema);
        schema.finish();
        new BoxGenerator().generateAll(jobForFileForceExists(schema, outputDir));

        assertTrue(cursorFile.toString(), cursorFile.exists());
        final String cursorContent = FileUtils.readUtf8(cursorFile);
        assertContains(cursorContent, "collect400000(cursor, 0, PUT_FLAG_FIRST,\n" +
                "                __id1, string1, __id2, string2,\n" +
                "                __id3, string3, __id4, string4);\n");

        assertContains(cursorContent, "        long __assignedId = " +
                "collect004000(cursor, entity.getId(), PUT_FLAG_COMPLETE,\n" +
                "                __ID_primitive, entity.getPrimitive(), 0, 0,\n");
    }

    private void assertContains(String full, String expectedPart) {
        if (!full.contains(expectedPart)) {
            String subPart = expectedPart;
            while (subPart.length() > 0) {
                subPart = subPart.substring(0, subPart.length() - 1);
                int idx = full.indexOf(subPart);
                if (idx >= 0) {
                    int endIndex = Math.min(idx + subPart.length() + 20, full.length());
                    String withFirstDiffs = full.substring(idx, endIndex);
                    assertEquals("String does not contain chars, showing with first diffs: ", expectedPart, withFirstDiffs);
                }
            }
            fail("Does not contain any starting chars of expected string");
        }
        assertTrue(full.contains(expectedPart));
    }

    @Test
    public void testDbName() {
        Assert.assertEquals("normal", TextUtil.dbName("normal"));
        assertEquals("Normal", TextUtil.dbName("Normal"));
        assertEquals("CamelCase", TextUtil.dbName("CamelCase"));
        assertEquals("CamelCaseThree", TextUtil.dbName("CamelCaseThree"));
        assertEquals("CamelCaseXXXX", TextUtil.dbName("CamelCaseXXXX"));
    }

    @Test
    public void testRelation() throws Exception {
        Schema schema = new Schema(1, "io.objectbox.test.relationbox");
        schema.setLastEntityId(new IdUid(2, 1003L));
        schema.setLastIndexId(new IdUid(1, 1100));
        Entity customer = schema.addEntity("Customer");
        customer.setModelId(1).setModelUid(1001L).setLastPropertyId(new IdUid(1, 501));
        customer.addIdProperty().modelId(new IdUid(1, 1002)).getProperty();
        Entity order = schema.addEntity("Order");
        order.setModelId(2).setModelUid(1003L).setLastPropertyId(new IdUid(2, 502));
        order.addIdProperty().modelId(new IdUid(1, 1004)).getProperty();
        Property customerId = order.addProperty(PropertyType.Long, "customerId").modelId(new IdUid(2, 1005))
                .modelIndexId(new IdUid(1, 1100)).typeNotNull().getProperty();
        ToOne toOne = new ToOne(
                "customer",
                false,
                null,
                null,
                null,
                "Customer"
        );
        toOne.setIdRefProperty(customerId);
        order.addToOne(toOne, customer);

        File outputDir = new File("build/test-out");
        outputDir.mkdirs();

        String baseDir = "io/objectbox/test/relationbox/";
        File cursorFile = new File(outputDir, baseDir + order.getClassName() + "Cursor.java");
        cursorFile.delete();
        assertFalse(cursorFile.exists());
        File myObjectBoxFile = new File(outputDir, baseDir + "MyObjectBox.java");
        myObjectBoxFile.delete();
        assertFalse(myObjectBoxFile.exists());

        assignMissingIdsUids(schema);
        schema.finish();
        new BoxGenerator().generateAll(jobForFileForceExists(schema, outputDir));

        // Assert Cursor file
        assertTrue(cursorFile.toString(), cursorFile.exists());
        final String cursorContent = FileUtils.readUtf8(cursorFile);
        assertContains(cursorContent, "__ID_customerId, entity.getCustomerId()");

        // Assert MyObjectBox file
        assertTrue(myObjectBoxFile.toString(), myObjectBoxFile.exists());
        final String myBoxContent = FileUtils.readUtf8(myObjectBoxFile);
        assertContains(myBoxContent, ".property(\"customerId\", \"Customer\", PropertyType.Relation)");
        assertContains(myBoxContent, ".flags(PropertyFlags.INDEXED | PropertyFlags.INDEX_PARTIAL_SKIP_ZERO)");
        assertContains(myBoxContent, ".indexId(1, 1100L)");
    }

    @Test
    public void testFbs() throws Exception {
        Schema schema = new Schema(1, "io.objectbox.test");
        Entity entity = schema.addEntity("Flaty");
        entity.addIdProperty();
        entity.addProperty(PropertyType.Int, "inty");
        entity.addProperty(PropertyType.String, "stringy");
        entity.addProperty(PropertyType.ByteArray, "bytearrayly");
        entity.addProperty(PropertyType.Date, "datey");

        File outputDir = new File("build/test-out");
        outputDir.mkdirs();

        assignMissingIdsUids(schema);
        schema.finish();
        GeneratorJob job = new GeneratorJob(schema, GeneratorOutput.create(outputDir));
        File outputDirFbs = new File(outputDir, "fbs-src");
        job.setOutputFlatbuffersSchema(GeneratorOutput.create(outputDirFbs));
        new BoxGenerator().generateAll(job);
    }


}
