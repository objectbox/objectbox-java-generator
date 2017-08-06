package io.objectbox.generator.idsync

import org.junit.Assert.*
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
        var idSync = IdSync(file)
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
    fun testBadFiles() {
        val badFiles = dir.listFiles().filter { it.name != "all-ok.json" }
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
        assertTrue(badFiles.size > 0)
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