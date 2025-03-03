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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File


class ClassProberTest : AbstractTransformTest() {

    @Test
    fun testProbeNoEntity() {
        assertFalse(probeClass(TestCursor::class).isEntity)
    }

    @Test
    fun testProbeEntity() {
        val entity = probeClass(EntityEmpty::class)
        assertNotNull(entity)
        assertTrue(entity.isEntity)
        assertFalse(entity.hasToManyRef)
        assertFalse(entity.hasToOneRef)
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
        assertTrue(entity.hasToOneRef)
    }

    @Test
    fun testProbeEntityToMany() {
        val entity = probeClass(EntityToMany::class)
        assertTrue(entity.hasToManyRef)
    }

    @Test
    fun testProbeEntityToManyList() {
        val entity = probeClass(EntityToManyListLateInit::class)
        assertEquals(1, entity.listFieldTypes.size)
        assertEquals(EntityEmpty::class.qualifiedName, entity.listFieldTypes[0])
    }

    @Test
    fun testProbeBaseEntity() {
        // detects fields if @BaseEntity
        probeClass(EntityBaseWithRelations::class).let {
            assertFalse(it.isEntity)
            assertTrue(it.isBaseEntity)
            assertFalse(it.hasBoxStoreField)
            assertTrue(it.hasToOneRef)
            assertTrue(it.hasToManyRef)
            assertFalse(it.listFieldTypes.isEmpty())
        }
        // ignores fields if non-@BaseEntity, sets superclass property
        probeClass(EntityBaseNoAnnotation::class).let {
            assertFalse(it.isEntity)
            assertFalse(it.isBaseEntity)
            assertFalse(it.hasBoxStoreField)
            assertFalse(it.hasToOneRef)
            assertFalse(it.hasToManyRef)
            assertTrue(it.listFieldTypes.isEmpty())
            assertEquals(EntityBaseWithRelations::class.java.canonicalName, it.superClass)
        }
        // sets superclass property, adds interfaces if @Entity
        probeClass(EntitySub::class).let {
            assertNotNull(it.superClass)
            assertEquals(EntityBase::class.java.canonicalName, it.superClass)
            assertFalse(it.interfaces.isEmpty())
        }
    }

    @Test
    fun testProbeCursor() {
        val probed = probeClass(TestCursor::class)
        assertTrue(probed.isCursor)
    }

    @Test
    fun testProbeEntityInfo() {
        val probed = probeClass(EntityToOneLateInit_::class)
        assertTrue(probed.isEntityInfo)
    }

    @Test
    fun testProbedClassHasRelation() {
        assertFalse(
            ProbedClass(
                outDir = File("."),
                file = File("."),
                name = "",
                javaPackage = "",
                listFieldTypes = listOf("Nope")
            ).hasRelation(setOf("Yes"))
        )
        assertTrue(
            ProbedClass(
                outDir = File("."),
                file = File("."),
                name = "",
                javaPackage = "",
                listFieldTypes = listOf("Yes")
            ).hasRelation(setOf("Yes"))
        )
    }

}