package io.objectbox.gradle.transform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.reflect.KClass

class ClassTransformerTest : AbstractTransformTest() {
    val transformer = ClassTransformer(true)

    @Test
    fun testTransformEntity_toOne() {
        val (stats) = testTransformOrCopy(EntityToOne::class, 1, 0)
        assertEquals(1, stats.toOnesFound)
        assertEquals(0, stats.toOnesInitialized)
    }

    @Test
    fun testTransformEntity_toOneLateInit() {
        val (stats) = testTransformOrCopy(EntityToOneLateInit::class, 1, 0)
        assertEquals(1, stats.toOnesFound)
        assertEquals(1, stats.toOnesInitialized)
    }

    @Test
    fun testTransformCursor() {
        val classes = listOf(TestCursor::class, EntityBoxStoreField::class)
        testTransformOrCopy(classes, 2, 0)
    }

    @Test(expected = TransformException::class)
    fun testTransformCursorWithExistingImpl() {
        testTransformOrCopy(CursorWithExistingImpl::class, 1, 0)
    }

    @Test
    fun testCopy() {
        val result = testTransformOrCopy(JustCopyMe::class, 0, 1)
        val copiedFile = result.second.single()
        val expectedPath = '/' + JustCopyMe::class.qualifiedName!!.replace('.', '/') + ".class"
        val actualPath = copiedFile.absolutePath.replace('\\', '/')
        assertTrue(actualPath, actualPath.endsWith(expectedPath))
    }

    fun testTransformOrCopy(kClass: KClass<*>, expectedTransformed: Int, expectedCopied: Int)
            = testTransformOrCopy(listOf(kClass), expectedTransformed, expectedCopied)

    fun testTransformOrCopy(kClasses: List<KClass<*>>, expectedTransformed: Int, expectedCopied: Int)
            : Pair<ClassTransformerStats, List<File>> {
        val probedClasses = kClasses.map { probeClass(it) }
        val tempDir = File.createTempFile(this.javaClass.name, "")
        tempDir.delete()
        assertTrue(tempDir.mkdir())
        try {
            val stats = transformer.transformOrCopyClasses(probedClasses, tempDir)
            assertEquals(expectedTransformed, stats.countTransformed)
            assertEquals(expectedCopied, stats.countCopied)
            val createdFiles = tempDir.walkBottomUp().toList().filter { it.isFile }
            assertEquals(expectedTransformed + expectedCopied, createdFiles.size)
            return Pair(stats, createdFiles)
        } finally {
            tempDir.deleteRecursively()
        }
    }


}