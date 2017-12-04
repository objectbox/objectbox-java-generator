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
        assertFalse(probeClass(this.javaClass.kotlin).isEntity)
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
        probeClass(EntityBase::class).let {
            assertFalse(it.isEntity)
            assertTrue(it.isBaseEntity)
            assertTrue(it.hasBoxStoreField)
            assertTrue(it.hasToOneRef)
            assertTrue(it.hasToManyRef)
            assertFalse(it.listFieldTypes.isEmpty())
        }
        // ignores fields if non-@BaseEntity
        probeClass(EntityNoBase::class).let {
            assertFalse(it.isEntity)
            assertFalse(it.isBaseEntity)
            assertFalse(it.hasBoxStoreField)
            assertFalse(it.hasToOneRef)
            assertFalse(it.hasToManyRef)
            assertTrue(it.listFieldTypes.isEmpty())
        }
        // sets superclass property
        probeClass(EntitySub::class).let {
            assertNotNull(it.superClass)
            assertEquals(EntityBase::class.java.canonicalName, it.superClass)
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
        assertFalse(ProbedClass(File("."), "", "", listFieldTypes = listOf("Nope")).hasRelation(setOf("Yes")))
        assertTrue(ProbedClass(File("."), "", "", listFieldTypes = listOf("Yes")).hasRelation(setOf("Yes")))
    }

}