package io.objectbox.gradle.transform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.reflect.KClass

class ClassTransformerTest : AbstractTransformTest() {
    val transformer = ClassTransformer(true)

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
            val stats = transformer.transformOrCopyClasses(listOf(probedClass), tempDir)
            assertEquals(expectedTransformed, stats.countTransformed)
            assertEquals(expectedCopied, stats.countCopied)
            val createdFiles = tempDir.walkBottomUp().toList().filter { it.isFile }
            assertEquals(expectedTransformed + expectedCopied, createdFiles.size)
            return createdFiles
        } finally {
            tempDir.deleteRecursively()
        }
    }


}