package io.objectbox.gradle.transform

import io.objectbox.Cursor
import io.objectbox.annotation.Entity
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
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
    private fun attachEntity(@Suppress("UNUSED_PARAMETER") entity: EntityBoxStoreField) {}
}

class CursorWithExistingImpl : Cursor() {
    private fun attachEntity(entity: EntityBoxStoreField) {
        System.out.println(entity)
    }
}

class JustCopyMe

class ClassTransformerTest {
    val transformer = ClassTransformer(true)
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
        testTransformOrCopy(EntityToOne::class, 1, 0)
    }

    @Test
    fun testTransformCursor() {
        testTransformOrCopy(TestCursor::class, 1, 0)
    }

    @Test(expected = TransformException::class)
    fun testTransformCursorWithExistingImpl() {
        testTransformOrCopy(CursorWithExistingImpl::class, 1, 0)
    }

    @Test
    fun testCopy() {
        val copiedFile = testTransformOrCopy(JustCopyMe::class, 0, 1).single()
        val expectedPath = '/' + JustCopyMe::class.qualifiedName!!.replace('.', '/') + ".class"
        val actualPath = copiedFile.absolutePath.replace('\\', '/')
        assertTrue(actualPath, actualPath.endsWith(expectedPath))
    }

    fun testTransformOrCopy(kClass: KClass<*>, expectedTransformed: Int, expectedCopied: Int): List<File> {
        val probedClass = probeClass(kClass)
        val tempDir = File.createTempFile(this.javaClass.name, "")
        tempDir.delete()
        assertTrue(tempDir.mkdir())
        try {
            transformer.transformOrCopyClasses(listOf(probedClass), tempDir)
            assertEquals(expectedTransformed, transformer.totalCountTransformed)
            assertEquals(expectedCopied, transformer.totalCountCopied)
            val createdFiles = tempDir.walkBottomUp().toList().filter { it.isFile }
            assertEquals(expectedTransformed + expectedCopied, createdFiles.size)
            return createdFiles
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