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

import org.greenrobot.greendao.generator.DaoUtil;
import org.greenrobot.greendao.generator.Entity;
import org.greenrobot.greendao.generator.Schema;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import org.greenrobot.essentials.io.FileUtils;

import io.objectbox.generator.BoxGenerator;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

        new BoxGenerator().generateAll(schema, outputDir.getPath());

        // Assert Cursor file
        assertTrue(cursorFile.toString(), cursorFile.exists());
        final String cursorContent = FileUtils.readUtf8(cursorFile);
        assertTrue(cursorContent.contains(miniEntity.getClassName() + "Cursor"));
        assertTrue(cursorContent.contains("long put(" + miniEntity.getClassName() + " entity)"));
        assertTrue(cursorContent.contains("PUT_FLAG_FIRST | PUT_FLAG_COMPLETE"));
        final String cursorContentLower = cursorContent.toLowerCase();
        //assertEquals(-1, cursorContentLower.indexOf("greendao"));
        assertEquals(-1, cursorContentLower.indexOf("table"));
        assertEquals(-1, cursorContentLower.indexOf("sql"));

        // Assert Properties file
        assertTrue(propertiesFile.toString(), propertiesFile.exists());
        final String propertiesContent = FileUtils.readUtf8(propertiesFile);
        assertTrue(propertiesContent.contains("class " + miniEntity.getClassName() + "_"));
        assertTrue(propertiesContent.contains("__ALL_PROPERTIES"));
        assertTrue(propertiesContent.contains("getAllProperties()"));
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

        new BoxGenerator().generateAll(schema, outputDir.getPath());

        assertTrue(cursorFile.toString(), cursorFile.exists());
        final String cursorContent = FileUtils.readUtf8(cursorFile);
        assertTrue(cursorContent.contains("        collect002033(cursor, 0, PUT_FLAG_FIRST,\n"));
        assertTrue(cursorContent.contains("        long __assignedId = collect002033(cursor, entity.getId(), PUT_FLAG_COMPLETE,\n"));
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

        new BoxGenerator().generateAll(schema, outputDir.getPath());

        assertTrue(cursorFile.toString(), cursorFile.exists());
        final String cursorContent = FileUtils.readUtf8(cursorFile);
        assertTrue(cursorContent.contains("collect400000(cursor, 0, PUT_FLAG_FIRST,\n" +
                "                _id1, string1, _id2, string2,\n" +
                "                _id3, string3, _id4, string4);\n"
        ));

        assertTrue(cursorContent.contains("        long __assignedId = " +
                "collect004000(cursor, entity.getId(), PUT_FLAG_COMPLETE,\n" +
                "                _primitiveId, entity.getPrimitive(), 0, 0,\n"
        ));
    }

    @Test
    public void testDbName() {
        Assert.assertEquals("normal", DaoUtil.dbName("normal"));
        assertEquals("Normal", DaoUtil.dbName("Normal"));
        assertEquals("CamelCase", DaoUtil.dbName("CamelCase"));
        assertEquals("CamelCaseThree", DaoUtil.dbName("CamelCaseThree"));
        assertEquals("CamelCaseXXXX", DaoUtil.dbName("CamelCaseXXXX"));
    }

    @Test(expected = RuntimeException.class)
    public void testInterfacesError() throws Exception {
        Schema schema = new Schema(1, "io.objectbox.test");
        Entity addressTable = schema.addEntity("FooBar");
        addressTable.implementsInterface("Dummy");
        addressTable.implementsInterface("Dummy");
    }
}