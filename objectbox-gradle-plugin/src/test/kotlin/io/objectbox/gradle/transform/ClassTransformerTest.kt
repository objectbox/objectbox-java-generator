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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class ClassTransformerTest : AbstractTransformTest() {

    @Rule @JvmField
    val thrown : ExpectedException = ExpectedException.none()

    @Test
    fun testClassInPool() {
        val classPool = ClassTransformer.Context(emptyList()).classPool

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
    fun testTransformEntityInheritance() {
        val classes = listOf(EntitySub::class, EntitySub_::class, EntityBase::class, EntitySubCursor::class,
                EntityInterface::class)
        val (stats) = testTransformOrCopy(classes, 2, 3)
        assertEquals(1, stats.toManyFound)
        assertEquals(1, stats.toOnesFound)
        assertEquals(1, stats.toOnesInitializerAdded)
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

    @Test
    fun testTransformEntity_toOne() {
        val classes = listOf(EntityToOne::class, EntityToOne_::class)
        val (stats) = testTransformOrCopy(classes, 1, 1)
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
    fun testTransformEntity_toOneSuffix() {
        val classes = listOf(EntityToOneSuffix::class, EntityToOneSuffix_::class)
        val (stats) = testTransformOrCopy(classes, 1, 1)
        assertEquals(0, stats.toManyFound)
        assertEquals(1, stats.toOnesFound)
        assertEquals(1, stats.toOnesInitializerAdded)
    }

    @Test
    fun testTransformEntity_toMany() {
        val classes = listOf(EntityToMany::class, EntityToMany_::class, EntityEmpty::class)
        val (stats) = testTransformOrCopy(classes, 1, 2)
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
    fun testTransformEntity_toManySuffix() {
        val classes = listOf(EntityToManySuffix::class, EntityToManySuffix_::class)
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
    fun testTransformEntity_transientList() {
        val classes = listOf(EntityTransientList::class, EntityTransientList_::class, EntityEmpty::class)
        val (stats) = testTransformOrCopy(classes, 1, 2)
        assertEquals(1, stats.boxStoreFieldsAdded)
        assertEquals(0, stats.toOnesFound)
        assertEquals(1, stats.toManyFound)
        assertEquals(1, stats.toManyInitializerAdded)
    }



    @Test
    fun doNotTransform_constructorCallingConstructor() {
        val classes = listOf(EntityMultipleCtors::class, EntityMultipleCtors_::class)
        val (stats) = testTransformOrCopy(classes, 1, 1)
        assertEquals(1, stats.toManyInitializerAdded) // only ctor calling super should be transformed
        assertEquals(1, stats.boxStoreFieldsAdded)
        assertEquals(0, stats.toOnesFound)
        assertEquals(1, stats.toManyFound)
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

}