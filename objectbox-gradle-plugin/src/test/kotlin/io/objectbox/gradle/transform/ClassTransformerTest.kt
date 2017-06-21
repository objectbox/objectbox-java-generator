package io.objectbox.gradle.transform

import io.objectbox.Cursor
import io.objectbox.annotation.Entity
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

@Entity
class EntityToOne {
    val entityEmpty = ToOne<EntityEmpty>()
}

@Entity
class EntityToMany {
    val entityEmpty = ToMany<EntityEmpty>()
}

class TestCursor : Cursor() {
    private fun attachEntity(entity: EntityBoxStoreField) {}
}

class CursorWithExistingImpl : Cursor() {
    private fun attachEntity(entity: EntityBoxStoreField) {
        System.out.println(entity)
    }
}

class ClassTransformerTest {
    val transformer = ClassTransformer()
    val classDir1 = File("build/classes/test")
    val classDir2 = File("objectbox-gradle-plugin/${classDir1.path}")
    val classDir = if (classDir1.exists()) classDir1 else classDir2

    @Test
    fun testClassDir() {
        assertTrue(classDir.exists())
    }

    @Test
    fun testProbeNoEntity() {
        assertFalse(probeClass(this.javaClass.kotlin).isEntity)
    }

    @Test
    fun testProbeEntity() {
        val entity = probeClass(EntityEmpty::class)
        assertNotNull(entity)
        assertTrue(entity.isEntity)
        assertFalse(entity.hasToMany)
        assertFalse(entity.hasToOne)
        assertFalse(entity.hasBoxStoreField)
        assertEquals(EntityEmpty::class.java.`package`.name, entity.javaPackage)
        assertEquals(EntityEmpty::class.java.name, entity.name)
    }

    @Test
    fun testProbeEntityBoxStoreField() {
        val entity = probeClass(EntityBoxStoreField::class)
        assertTrue(entity.hasBoxStoreField)
    }

    @Test
    fun testProbeEntityToOne() {
        val entity = probeClass(EntityToOne::class)
        assertTrue(entity.hasToOne)
    }

    @Test
    fun testProbeEntityToMany() {
        val entity = probeClass(EntityToMany::class)
        assertTrue(entity.hasToMany)
    }

    @Test
    fun testProbeCursor() {
        val probed = probeClass(TestCursor::class)
        assertTrue(probed.isCursor)
    }

    @Test
    fun testTransformEntity() {
        testTransformClass(EntityToOne::class)
    }

    @Test
    fun testTransformCursor() {
        testTransformClass(TestCursor::class)
    }

    @Test(expected = TransformException::class)
    fun testTransformCursorWithExistingImpl() {
        testTransformClass(CursorWithExistingImpl::class)
    }

    fun testTransformClass(kClass: KClass<*>) {
        val probedClass = probeClass(kClass)
        val tempDir = File.createTempFile(this.javaClass.name, "")
        tempDir.delete()
        assertTrue(tempDir.mkdir())
        try {
            transformer.transformOrCopyClasses(listOf(probedClass), tempDir)
            assertEquals(1, transformer.totalCountTransformed)
            assertEquals(0, transformer.totalCountCopied)
            val createdFiles = tempDir.walkBottomUp().toList()
            assertEquals(1, createdFiles.filter { it.isFile }.size)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun probeClass(kclass: KClass<*>): ProbedClass {
        val file = File(classDir, kclass.qualifiedName!!.replace('.', '/') + ".class")
        assertTrue(file.exists())
        return transformer.probeClass(file)
    }

}