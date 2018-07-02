/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

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