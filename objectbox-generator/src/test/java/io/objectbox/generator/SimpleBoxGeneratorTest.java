/*
 * Copyright (C) 2016 Markus Junginger, greenrobot (http://greenrobot.org)
 *
 * This file is part of greenDAO Generator.
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

import io.objectbox.generator.model.Entity;
import io.objectbox.generator.model.Property;
import io.objectbox.generator.model.Schema;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import org.greenrobot.essentials.io.FileUtils;

import static org.junit.Assert.*;

public class SimpleBoxGeneratorTest {

    @Test
    public void testMinimalSchema() throws Exception {
        Schema schema = new Schema(1, "io.objectbox.test.minimalbox");
        Entity miniEntity = schema.addEntity("MiniBox");
        miniEntity.addIdProperty().getProperty();
        miniEntity.addIntProperty("count").index();
        miniEntity.addIntProperty("dummy").notNull();
        assertEquals(1, schema.getEntities().size());
        assertEquals(3, miniEntity.getProperties().size());

        File outputDir = new File("build/test-out");
        outputDir.mkdirs();

        String baseName = "io/objectbox/test/minimalbox/" + miniEntity.getClassName();
        File cursorFile = new File(outputDir, baseName + "Cursor.java");
        cursorFile.delete();
        assertFalse(cursorFile.exists());
        File propertiesFile = new File(outputDir, baseName + "_.java");
        propertiesFile.delete();
        assertFalse(propertiesFile.exists());

        assignIdsUids(schema);
        new BoxGenerator().generateAll(schema, outputDir.getPath());

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
        assertTrue(propertiesFile.toString(), propertiesFile.exists());
        final String propertiesContent = FileUtils.readUtf8(propertiesFile);
        assertContains(propertiesContent, "class " + miniEntity.getClassName() + "_");
        assertContains(propertiesContent, "__ALL_PROPERTIES");
        assertContains(propertiesContent, "getAllProperties()");
    }

    private void assignIdsUids(Schema schema) {
        int id = 1;
        long uid = 1000;
        for (Entity entity : schema.getEntities()) {
            entity.setModelId(id++);
            entity.setModelUid(uid++);
        }
    }

    @Test
    public void testSchemaWithTwoCollects() throws Exception {
        Schema schema = new Schema(1, "io.objectbox.test.multicollect");
        Entity multiCollectEntity = schema.addEntity("MultiCollectBox");
        multiCollectEntity.addIdProperty().getProperty();
        multiCollectEntity.addFloatProperty("foo").index();
        multiCollectEntity.addFloatProperty("bar").notNull();
        multiCollectEntity.addFloatProperty("box");
        multiCollectEntity.addFloatProperty("in2ndCall");
        multiCollectEntity.addFloatProperty("in2ndCall2");

        File outputDir = new File("build/test-out");
        outputDir.mkdirs();

        File cursorFile = new File(outputDir, "io/objectbox/test/multicollect/" + multiCollectEntity.getClassName() + "Cursor.java");
        cursorFile.delete();
        assertFalse(cursorFile.exists());

        assignIdsUids(schema);
        new BoxGenerator().generateAll(schema, outputDir.getPath());

        assertTrue(cursorFile.toString(), cursorFile.exists());
        final String cursorContent = FileUtils.readUtf8(cursorFile);
        assertContains(cursorContent, "        collect002033(cursor, 0, PUT_FLAG_FIRST,\n");
        assertContains(cursorContent, "        long __assignedId = collect002033(cursor, entity.getId(), PUT_FLAG_COMPLETE,\n");
    }

    @Test
    public void testSchemaWithTwoCollects_StringsBeforePrimitives() throws Exception {
        Schema schema = new Schema(1, "io.objectbox.test.multicollect");
        Entity multiCollectEntity = schema.addEntity("MultiCollectBox_StringsBeforePrimitives");
        multiCollectEntity.addIdProperty().getProperty();
        multiCollectEntity.addStringProperty("string1");
        multiCollectEntity.addStringProperty("string2");
        multiCollectEntity.addStringProperty("string3");
        multiCollectEntity.addStringProperty("string4");
        multiCollectEntity.addIntProperty("primitive");

        File outputDir = new File("build/test-out");
        outputDir.mkdirs();

        File cursorFile = new File(outputDir, "io/objectbox/test/multicollect/" + multiCollectEntity.getClassName() + "Cursor.java");
        cursorFile.delete();
        assertFalse(cursorFile.exists());

        assignIdsUids(schema);
        new BoxGenerator().generateAll(schema, outputDir.getPath());

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

    @Test(expected = RuntimeException.class)
    public void testInterfacesError() throws Exception {
        Schema schema = new Schema(1, "io.objectbox.test");
        Entity addressTable = schema.addEntity("FooBar");
        addressTable.implementsInterface("Dummy");
        addressTable.implementsInterface("Dummy");
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
        Property customerId = order.addLongProperty("customerId").modelId(new IdUid(2, 1005))
                .modelIndexId(new IdUid(1, 1100)).getProperty();
        order.addToOne(customer, customerId, "customer", null, false);

        File outputDir = new File("build/test-out");
        outputDir.mkdirs();

        String baseDir = "io/objectbox/test/relationbox/";
        File cursorFile = new File(outputDir, baseDir + order.getClassName() + "Cursor.java");
        cursorFile.delete();
        assertFalse(cursorFile.exists());
        File myObjectBoxFile = new File(outputDir, baseDir + "MyObjectBox.java");
        myObjectBoxFile.delete();
        assertFalse(myObjectBoxFile.exists());

        assignIdsUids(schema);
        new BoxGenerator().generateAll(schema, outputDir.getPath());

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

}
