package io.objectbox.gradle.transform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.reflect.KClass

class ClassTransformerTest : AbstractTransformTest() {
    val transformer = ClassTransformer(true)

    @Test
    fun testClassInPool() {
        val classPool = ClassTransformer.Context(emptyList(), File(".")).classPool

        // Ensure we have the real java.lang.Object (a fake would have itself as superclass)
        assertNull(classPool.get("java.lang.Object").superclass)

        val toOne = classPool.get(ClassConst.toOne)
        val constructorSignature = toOne.constructors.single().signature
        // Verify its not the test fake
        assertEquals("(Ljava/lang/Object;Lio/objectbox/relation/RelationInfo;)V", constructorSignature)
        assertTrue(toOne.declaredFields.size > 0)
        assertTrue(toOne.declaredMethods.size > 0)
    }

    @Test
    fun testTransformEntity_toOne() {
        val (stats) = testTransformOrCopy(EntityToOne::class, 1, 0)
        assertEquals(0, stats.toManyFound)
        assertEquals(1, stats.toOnesFound)
        assertEquals(0, stats.toOnesInitializerAdded)
    }

    @Test
    fun testTransformEntity_toOneLateInit() {
        val classes = listOf(EntityToOneLateInit::class, EntityToOneLateInit_::class)
        val (stats) = testTransformOrCopy(classes, 1, 1)
        assertEquals(0, stats.toManyFound)
        assertEquals(1, stats.toOnesFound)
        assertEquals(1, stats.toOnesInitializerAdded)
    }

    @Test
    fun testTransformEntity_toMany() {
        val classes = listOf(EntityToMany::class, EntityEmpty::class)
        val (stats) = testTransformOrCopy(classes, 1, 1)
        assertEquals(1, stats.boxStoreFieldsAdded)
        assertEquals(2, stats.toManyFound)
        assertEquals(0, stats.toManyInitializerAdded)
    }

    @Test
    fun testTransformEntity_toManyLateInit() {
        val classes = listOf(EntityToManyLateInit::class, EntityToManyLateInit_::class)
        val (stats) = testTransformOrCopy(classes, 1, 1)
        assertEquals(1, stats.boxStoreFieldsAdded)
        assertEquals(0, stats.toOnesFound)
        assertEquals(1, stats.toManyFound)
        assertEquals(1, stats.toManyInitializerAdded)
    }

    @Test
    fun testTransformEntity_toManyListLateInit() {
        val classes = listOf(EntityToManyListLateInit::class, EntityToManyListLateInit_::class, EntityEmpty::class)
        val (stats) = testTransformOrCopy(classes, 1, 2)
        assertEquals(1, stats.boxStoreFieldsAdded)
        assertEquals(0, stats.toOnesFound)
        assertEquals(1, stats.toManyFound)
        assertEquals(1, stats.toManyInitializerAdded)
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