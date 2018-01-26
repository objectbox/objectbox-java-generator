<#--

Copyright (C) 2017-2018 ObjectBox Ltd.

This file is part of ObjectBox Build Tools.

ObjectBox Build Tools is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
ObjectBox Build Tools is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.

-->
package ${entity.javaPackageTest};

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Random;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.ModelBuilder.EntityBuilder;
import io.objectbox.model.PropertyFlags;
import io.objectbox.model.PropertyType;

<#if entity.javaPackageTest != schema.defaultJavaPackageDao>
import ${schema.defaultJavaPackageDao}.MyObjectBox;
</#if>

<#if entity.javaPackageTest != entity.javaPackage>
import ${entity.javaPackage}.${entity.className};
</#if>

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

<#-- TODO use some abstract class as base -->
public class ${entity.classNameTest} {
    protected File boxStoreDir;
    protected BoxStore store;
    protected Box<${entity.className}> box;
    protected Random random = new Random();
    protected boolean runExtensiveTests;

    @Before
    public void setUp() throws Exception {
        // This works with Android without needing any context
        File tempFile = File.createTempFile("object-store-test", "");
        tempFile.delete();
        boxStoreDir = tempFile;
        store = createBoxStore();
        box = store.boxFor(${entity.className}.class);
        runExtensiveTests = System.getProperty("extensive") != null;
    }

    protected BoxStore createBoxStore() {
        return MyObjectBox.builder().directory(boxStoreDir).build();
    }

    @After
    public void tearDown() throws Exception {
        if (store != null) {
            try {
                store.close();
                store.deleteAllFiles();

                File[] files = boxStoreDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        logError("File was not deleted: " + file.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                logError("Could not clean up test", e);
            }
        }
        if (boxStoreDir != null && boxStoreDir.exists()) {
            File[] files = boxStoreDir.listFiles();
            for (File file : files) {
                delete(file);
            }
            delete(boxStoreDir);
        }
    }

    @Test
    public void testBasics() {
        ${entity.className} object = new ${entity.className}();
        long id = box.put(object);
        assertTrue(id > 0);
        assertEquals(id, (long) object.get${entity.pkProperty.propertyName?cap_first}());
        ${entity.className} objectRead = box.get(id);
        assertNotNull(objectRead);
        assertEquals(id, (long) objectRead.get${entity.pkProperty.propertyName?cap_first}());
        assertEquals(1, box.count());

        box.remove(id);
        assertEquals(0, box.count());
        assertNull(box.get(id));
    }

    private boolean delete(File file) {
        boolean deleted = file.delete();
        if (!deleted) {
            file.deleteOnExit();
            logError("Could not delete " + file.getAbsolutePath());
        }
        return deleted;
    }

    protected void log(String text) {
        System.out.println(text);
    }

    protected void logError(String text) {
        System.err.println(text);
    }

    protected void logError(String text, Exception ex) {
        if (text != null) {
            System.err.println(text);
        }
        ex.printStackTrace();
    }

    protected long time() {
        return System.currentTimeMillis();
    }

}