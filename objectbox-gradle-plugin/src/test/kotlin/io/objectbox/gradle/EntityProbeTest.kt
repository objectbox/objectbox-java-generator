package io.objectbox.gradle

import io.objectbox.annotation.Entity
import org.gradle.api.Project
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import kotlin.reflect.KClass

@Entity
class EntityEmpty

@Entity
class EntityBoxStoreField {
    val __boxStore = Object()
}

class EntityProbeTest {
    val prober = ObjectBoxAndroidTransform(Mockito.mock(Project::class.java))
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

    private fun probeClass(kclass: KClass<*>): ProbedEntity? {
        val file = File(classDir, kclass.qualifiedName!!.replace('.', '/') + ".class")
        assertTrue(file.exists())
        return prober.probeClassAsEntity(file)
    }

}