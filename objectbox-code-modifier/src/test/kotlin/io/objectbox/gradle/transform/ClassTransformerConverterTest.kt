/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle.transform

import org.junit.Assert
import org.junit.Test


class ClassTransformerConverterTest : AbstractTransformTest() {

    @Test
    fun testTransformEntity_converter_shouldNotTransform() {
        val classes = listOf(
            EntityConverterList::class,
            EntityEmpty::class, EntityEmptyConverter::class
        )
        val (stats) = testTransformOrCopy(classes, 0, 3)
        Assert.assertEquals(0, stats.boxStoreFieldsAdded)
        Assert.assertEquals(0, stats.toOnesFound)
        Assert.assertEquals(0, stats.toManyFound)
        Assert.assertEquals(0, stats.toManyInitializerAdded)
    }

    @Test
    fun testTransformEntity_converterAndList_shouldTransformOnlyList() {
        val classes = listOf(
            EntityConverterListAndList::class, EntityConverterListAndList_::class,
            EntityEmpty::class, EntityEmptyConverter::class
        )
        val (stats) = testTransformOrCopy(classes, 1, 3)
        Assert.assertEquals(1, stats.boxStoreFieldsAdded)
        Assert.assertEquals(0, stats.toOnesFound)
        Assert.assertEquals(1, stats.toManyFound)
        Assert.assertEquals(1, stats.toManyInitializerAdded)
    }

    @Test
    fun testTransformEntity_converterAndToMany_shouldTransformOnlyToMany() {
        val classes = listOf(EntityConverterAndToMany::class, EntityConverterAndToMany_::class, EntityEmpty::class)
        val (stats) = testTransformOrCopy(classes, 1, 2)
        Assert.assertEquals(1, stats.boxStoreFieldsAdded)
        Assert.assertEquals(0, stats.toOnesFound)
        Assert.assertEquals(1, stats.toManyFound)
        Assert.assertEquals(1, stats.toManyInitializerAdded)
    }

    @Test
    fun testTransformEntity_converterAndToOne_shouldTransformOnlyToOne() {
        val classes = listOf(EntityConverterAndToOne::class, EntityConverterAndToOne_::class, EntityEmpty::class)
        val (stats) = testTransformOrCopy(classes, 1, 2)
        Assert.assertEquals(1, stats.boxStoreFieldsAdded)
        Assert.assertEquals(1, stats.toOnesFound)
        Assert.assertEquals(0, stats.toManyFound)
        Assert.assertTrue(stats.constructorsCheckedForTransform >= 1)
        Assert.assertEquals(stats.constructorsCheckedForTransform, stats.toOnesInitializerAdded)
    }

}