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

package io.objectbox.generator.idsync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File

class IdSyncFileTest {
    private val dir: File = File("test-files/idsync/")

    @Before
    fun initIdSync() {
        assertTrue(dir.isDirectory)
    }

    @Test
    fun testAllOk() {
        val file = File(dir, "all-ok.json")
        assertTrue(file.exists())
        val idSync = IdSync(file)
        val entity = idSync.findEntity("Note", null)!!
        assertSame(entity, idSync.findEntity("Kumbaya", 4858050548069557694))

        val property = idSync.findProperty(entity, "Kumbaya", 8044051146334126065)!!
        assertSame(property, idSync.findProperty(entity, "integerProperty", null))

        val model = idSync.justRead(file)!!
        assertEquals("1:4858050548069557694", model.lastEntityId.toString())
        assertEquals("Note", entity.name)
        assertEquals("1:4858050548069557694", entity.id.toString())
        assertEquals("7:1224882392647796759", entity.lastPropertyId.toString())
    }

    @Test
    fun testFutureVersionIncompatible() {
        val file = File(dir, "future-version-incompatible.json")
        assertTrue(file.exists())
        try {
            IdSync(file)
            fail("Should have thrown")
        } catch (e: IdSyncException) {
            assertEquals(IdSyncException::class.java, e.cause!!.javaClass)
            val message = e.cause!!.message!!
            assertTrue(message, message.contains("but found 9999999"))
            assertTrue(message, message.contains("maximum supported version is"))
        }
    }

    @Test
    fun testFutureVersionCompatible() {
        val file = File(dir, "future-version-compatible.json")
        assertTrue(file.exists())
        val idSyncModel = IdSync(file).justRead()!!
        assertEquals(3L, idSyncModel.modelVersionParserMinimum)
    }

    @Test
    fun testBadFiles() {
        val badFiles = dir.listFiles().filter { it.name != "all-ok.json" && !it.name.startsWith("future-version") }
        val expectedMessages = mapOf(
            "duplicate-entity-id.json" to "Duplicate ID 1 for entity Note",
            "duplicate-entity-uid.json" to "Duplicate UID 4858050548069557694",
            "duplicate-property-id.json" to "Duplicate ID 1 for property Note.integerProperty",
            "duplicate-property-uid.json" to "Duplicate UID 8303367770402050741",
            "mismatching-last-entity-id.json" to
                    "Entity Note ID 1:4858050548069557694 does not match UID of lastEntityId 1:893473467320234874",
            "mismatching-last-property-id.json" to
                    "Property Note.integerProperty ID 4:8044051146334126065 does not match UID of lastPropertyId 4:8303367770402050741"
        )
        assertTrue(badFiles.isNotEmpty())
        for (file in badFiles) {
            assertTrue(file.exists())
            try {
                IdSync(file)
                fail("Should have failed: " + file.absoluteFile)
            } catch (e: IdSyncException) {
                // OK
                val message = e.message!!
                assertTrue(message, message.contains(file.name))
                val expected = expectedMessages[file.name]
                if (expected != null) {
                    assertTrue("$message did not contain $expected", message.contains(expected))
                }
            }
        }
    }
}