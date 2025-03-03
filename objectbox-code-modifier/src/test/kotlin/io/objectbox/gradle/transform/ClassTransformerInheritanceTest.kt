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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test


class ClassTransformerInheritanceTest : AbstractTransformTest() {

    @Test
    fun testTransformEntityInheritance() {
        val classes = listOf(
            EntitySub::class, EntitySub_::class, EntityBase::class, EntitySubCursor::class, EntityInterface::class,
            EntityEmpty::class
        )
        val (stats) = testTransformOrCopy(classes, 2, 4)
        assertEquals(2, stats.toManyFound)
        assertEquals(1, stats.toOnesFound)
        assertEquals(2, stats.toManyInitializerAdded)
        assertEquals(1, stats.toOnesInitializerAdded)
    }

    @Test
    fun testTransformEntityRelationsInBaseEntity() {
        // relations in super classes are (currently) not supported
        val classes = listOf(EntityRelationsInSuperBase::class, EntityBaseWithRelations::class)
        assertThrows(TransformException::class.java) {
            testTransformOrCopy(classes, 0, 0)
        }.also {
            assertEquals(
                "Relations in an entity super class are not supported," +
                        " but 'io.objectbox.gradle.transform.EntityBaseWithRelations' is super of entity" +
                        " 'io.objectbox.gradle.transform.EntityRelationsInSuperBase' and has relations", it.message
            )
        }
    }

    @Test
    fun testTransformEntityRelationsInSuperEntity() {
        // relations in super classes are (currently) not supported
        val classes = listOf(EntityRelationsInSuperEntity::class, EntitySub::class)
        assertThrows(TransformException::class.java) {
            testTransformOrCopy(classes, 0, 0)
        }.also {
            assertEquals(
                "Relations in an entity super class are not supported," +
                        " but 'io.objectbox.gradle.transform.EntitySub' is super of entity" +
                        " 'io.objectbox.gradle.transform.EntityRelationsInSuperEntity' and has relations", it.message
            )
        }
    }

}