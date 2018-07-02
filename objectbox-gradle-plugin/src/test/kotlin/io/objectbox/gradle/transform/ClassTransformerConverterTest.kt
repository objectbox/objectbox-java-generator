package io.objectbox.gradle.transform

import org.junit.Assert
import org.junit.Test


class ClassTransformerConverterTest : AbstractTransformTest() {

    @Test
    fun testTransformEntity_converter_shouldNotTransform() {
        val classes = listOf(EntityConverterList::class,
                EntityEmpty::class, EntityEmptyConverter::class)
        val (stats) = testTransformOrCopy(classes, 0, 3)
        Assert.assertEquals(0, stats.boxStoreFieldsAdded)
        Assert.assertEquals(0, stats.toOnesFound)
        Assert.assertEquals(0, stats.toManyFound)
        Assert.assertEquals(0, stats.toManyInitializerAdded)
    }

    @Test
    fun testTransformEntity_converterAndList_shouldTransformOnlyList() {
        val classes = listOf(EntityConverterListAndList::class, EntityConverterListAndList_::class,
                EntityEmpty::class, EntityEmptyConverter::class)
        val (stats) = testTransformOrCopy(classes, 1, 3)
        Assert.assertEquals(1, stats.boxStoreFieldsAdded)
        Assert.assertEquals(0, stats.toOnesFound)
        Assert.assertEquals(1, stats.toManyFound)
        Assert.assertEquals(1, stats.toManyInitializerAdded)
    }

    @Test
    fun testTransformEntity_converterAndToMany_shouldTransformOnlyToMany() {
        val classes = listOf(EntityConverterAndToMany::class, EntityConverterAndToMany_::class)
        val (stats) = testTransformOrCopy(classes, 1, 1)
        Assert.assertEquals(1, stats.boxStoreFieldsAdded)
        Assert.assertEquals(0, stats.toOnesFound)
        Assert.assertEquals(1, stats.toManyFound)
        Assert.assertEquals(1, stats.toManyInitializerAdded)
    }

    @Test
    fun testTransformEntity_converterAndToOne_shouldTransformOnlyToOne() {
        val classes = listOf(EntityConverterAndToOne::class, EntityConverterAndToOne_::class)
        val (stats) = testTransformOrCopy(classes, 1, 1)
        Assert.assertEquals(1, stats.boxStoreFieldsAdded)
        Assert.assertEquals(1, stats.toOnesFound)
        Assert.assertEquals(0, stats.toManyFound)
        Assert.assertTrue(stats.constructorsCheckedForTransform >= 1)
        Assert.assertEquals(stats.constructorsCheckedForTransform, stats.toOnesInitializerAdded)
    }

}