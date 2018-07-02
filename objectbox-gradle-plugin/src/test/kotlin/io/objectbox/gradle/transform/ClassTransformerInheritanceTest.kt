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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException


class ClassTransformerInheritanceTest : AbstractTransformTest() {

    @Rule
    @JvmField
    val thrown : ExpectedException = ExpectedException.none()

    @Test
    fun testTransformEntityInheritance() {
        val classes = listOf(EntitySub::class, EntitySub_::class, EntityBase::class, EntitySubCursor::class,
                EntityInterface::class)
        val (stats) = testTransformOrCopy(classes, 2, 3)
        Assert.assertEquals(1, stats.toManyFound)
        Assert.assertEquals(1, stats.toOnesFound)
        Assert.assertEquals(1, stats.toOnesInitializerAdded)
    }

    @Test
    fun testTransformEntityRelationsInBaseEntity() {
        thrown.expectMessage("Relations in an entity super class are not supported")
        // relations in super classes are (currently) not supported
        val classes = listOf(EntityRelationsInSuperBase::class, EntityBaseWithRelations::class)
        testTransformOrCopy(classes, 0, 0)
    }

    @Test
    fun testTransformEntityRelationsInSuperEntity() {
        thrown.expectMessage("Relations in an entity super class are not supported")
        // relations in super classes are (currently) not supported
        val classes = listOf(EntityRelationsInSuperEntity::class, EntitySub::class)
        testTransformOrCopy(classes, 0, 0)
    }

}