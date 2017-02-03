package io.objecbox.generator.idsync

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
        assertTrue(badFiles.size > 0)
        for (file in badFiles) {
            assertTrue(file.exists())
            try {
                var idSync = IdSync(file)
                fail("Should have failed: " + file.absoluteFile)
            } catch (e: IdSyncException) {
                // OK
            }
        }
    }
}