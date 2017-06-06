package io.objectbox.gradle

import io.objectbox.annotation.Entity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.reflect.KClass

@Entity
class EntityEmpty

@Entity
class EntityBoxStoreField {
    val __boxStore = Object()
}

class EntityTransformerTest {
    val transformer = EntityTransformer()
    val classDir1 = File("build/classes/test")
    val classDir2 = File("objectbox-gradle-plugin/${classDir1.path}")
    val classDir = if (classDir1.exists()) classDir1 else classDir2

    @Test
    fun testClassDir() {
        assertTrue(classDir.exists())
    }

    @Test
    fun testProbeNoEntity() {
        assertNull(probeClass(this.javaClass.kotlin))
    }

    @Test
    fun testProbeEntity() {
        val entity = probeClass(EntityEmpty::class)!!
        assertNotNull(entity)
        assertFalse(entity.hasToMany)
        assertFalse(entity.hasToOne)
        assertFalse(entity.hasBoxStoreField)
    }

    @Test
    fun testProbeEntityBoxStoreField() {
        val entity = probeClass(EntityBoxStoreField::class)!!
        assertTrue(entity.hasBoxStoreField)
    }

    @Test
    fun testTransform() {
        val entity = probeClass(EntityEmpty::class)!!
        val tempDir = File.createTempFile(this.javaClass.name, "")
        tempDir.delete()
        assertTrue(tempDir.mkdir())
        try {
            transformer.transformEntities(listOf(entity), tempDir)
            val createdFiles = tempDir.walkBottomUp().toList()
            assertEquals(1, createdFiles.filter { it.isFile }.size)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun probeClass(kclass: KClass<*>): ProbedEntity? {
        val file = File(classDir, kclass.qualifiedName!!.replace('.', '/') + ".class")
        assertTrue(file.exists())
        return transformer.probeClassAsEntity(file)
    }

}