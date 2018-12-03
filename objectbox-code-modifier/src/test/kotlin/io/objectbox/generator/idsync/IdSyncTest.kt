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

package io.objectbox.generator.idsync

import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class IdSyncTest {

    val file: File = File.createTempFile("idsync-test", ".json")
    var idSync: IdSync? = null

    @Before
    fun initIdSync() {
        file.delete()
        idSync = IdSync(file)
    }

    @After
    fun deleteFiles() {
        file.delete()
        idSync?.backupFile?.delete()
    }

    @Test
    fun testModelUid() {
        val modelUid = UidHelper()
        val unique = mutableSetOf<Long>()
        for (i in 0..100) {
            val x = modelUid.create()
            modelUid.verify(x)
            assertTrue(unique.add(x))
            try {
                val shifted = 1L.shl(i)
                modelUid.verify(x xor shifted)
            } catch (e: RuntimeException) {
                // Expected
            }
            try {
                modelUid.verify(x xor (1 shl 60))
            } catch (e: RuntimeException) {
                // Expected
            }
        }
    }

    @Test
    fun testIdSyncBasics() {
        val model = syncBasicModel()
        assertEquals(1, model.lastEntityId.id)

        assertEquals(1, model.entities.size)
        val entity = model.entities.first()
        assertEquals("Entity1", entity.name)
        assertEquals(1, entity.modelId)
        assertTrue(entity.uid > 1)

        assertEquals(2, entity.properties.size)
        assertEquals("foo", entity.properties[0].name)
        assertEquals(1, entity.properties[0].modelId)
        assertTrue(entity.properties[0].uid > 1)
        assertEquals(2, entity.properties[1].modelId)
        assertEquals("bar", entity.properties[1].name)
        assertTrue(entity.properties[1].uid > 1)
    }

    @Test
    fun testKeepParsedUid() {
        val model1 = syncBasicModel()
        val entityRefId = model1.entities.first().uid
        val propertyRefId = model1.entities.first().properties.first().uid

        val schema = basicSchema()
        val entity1 = addEntityTo(schema, "Entity1A", entityRefId)
        addPropertyTo(entity1, "bla", propertyRefId)
        idSync = IdSync(file)
        idSync!!.sync(schema)

        val model2 = idSync!!.justRead()!!
        assertEquals(1, model2.entities.size)
        val entity = model2.entities.first()
        assertEquals(entityRefId, entity.uid)
        assertEquals(1, entity.properties.size)
        assertEquals(propertyRefId, entity.properties.first().uid)
    }

    @Test
    fun testKeepIdsForMatchingNames() {
        val schema = basicSchema()
        addBasicPropertiesTo(addEntityTo(schema, "Entity1"))

        testKeepIdsForMatchingNames(schema)
    }

    @Test
    fun testKeepIdsForMatchingNames_differentCase() {
        val schema = basicSchema()
        val entity = addEntityTo(schema, "ENTITY1")
        addPropertyTo(entity, "FOO")
        addPropertyTo(entity, "bAr")

        testKeepIdsForMatchingNames(schema)
    }

    private fun testKeepIdsForMatchingNames(schema: Schema) {
        val model1 = syncBasicModel()
        val entity1 = model1.entities.first()

        idSync = IdSync(file)
        idSync!!.sync(schema)

        val model2 = idSync!!.justRead()!!
        assertEquals(1, model2.entities.size)
        val entity2 = model2.entities.first()
        assertEquals(entity1.id, entity2.id)
        assertEquals(entity1.uid, entity2.uid)

        assertEquals(2, entity2.properties.size)
        assertEquals(entity1.properties.first().id, entity2.properties.first().id)
        assertEquals(entity1.properties.first().uid, entity2.properties.first().uid)
        assertEquals(entity1.properties.last().id, entity2.properties.last().id)
        assertEquals(entity1.properties.last().uid, entity2.properties.last().uid)
    }

    @Test
    fun testAddEntity() {
        val model1 = syncBasicModel()
        val entityRefId = model1.entities.first().uid

        val schema = basicSchema()
        addBasicPropertiesTo(addEntityTo(schema, "Entity1A", entityRefId))
        addBasicPropertiesTo(addEntityTo(schema, "Entity2"))
        idSync = IdSync(file)
        idSync!!.sync(schema)

        val model2 = idSync!!.justRead()!!
        assertEquals(2, model2.lastEntityId.id)
        assertEquals(2, model2.entities.size)
        val entity = model2.entities.last()
        assertEquals(2, entity.modelId)
        assertTrue(entity.uid > 1)
    }

    @Test
    fun testAddProperties() {
        val model1 = syncBasicModel()
        assertEquals(2, model1.entities.first().lastPropertyId.id)

        val schema = basicSchema()
        val entity = addEntityTo(schema, "Entity1")
        addPropertyTo(entity, "newOne")
        addPropertyTo(entity, "newTwo")
        idSync = IdSync(file)
        idSync!!.sync(schema)

        val model2 = idSync!!.justRead()!!
        val entity2 = model2.entities.first()
        assertEquals(4, entity2.lastPropertyId.id)
        assertEquals(3, entity2.properties.first().modelId)
        assertEquals(4, entity2.properties.last().modelId)
    }

    @Test(expected = IdSyncException::class)
    fun testAddEntityWithDuplicateRefId() {
        val model1 = syncBasicModel()
        val entityRefId = model1.entities.first().uid
        val schema = basicSchema()
        addBasicPropertiesTo(addEntityTo(schema, "Entity1A", entityRefId))
        addBasicPropertiesTo(addEntityTo(schema, "Entity2", entityRefId))
        idSync = IdSync(file)
        idSync!!.sync(schema)
    }

    @Test(expected = IdSyncException::class)
    fun testAddEntityPropertyWithDuplicateRefId() {
        val model1 = syncBasicModel()
        val propertyRefId = model1.entities.first().properties.first().uid
        val entityRefId = model1.entities.first().uid
        val schema = basicSchema()
        val entity = addEntityTo(schema, "Entity1", entityRefId)
        addBasicPropertiesTo(entity, propertyRefId)
        idSync = IdSync(file)
        idSync!!.sync(schema)
    }

    @Test(expected = IdSyncException::class)
    fun testEntityPropertyRefIdCollision() {
        val model1 = syncBasicModel()
        val entityRefId = model1.entities.first().uid
        val schema = basicSchema()
        val entity = addEntityTo(schema, "Entity1", entityRefId)
        addBasicPropertiesTo(entity, entityRefId, true)
        idSync = IdSync(file)
        idSync!!.sync(schema)
    }

    @Test
    fun testAddIndexId() {
        val model1 = syncBasicModel()
        assertEquals(2, model1.entities.first().lastPropertyId.id)

        val schema = basicSchema()
        val entity = addEntityTo(schema, "Entity1")
        addPropertyTo(entity, "foo")
        addPropertyTo(entity, "bar", null, true)
        addPropertyTo(entity, "newAndIndexed", null, true)
        idSync = IdSync(file)
        idSync!!.sync(schema)

        val model2 = idSync!!.justRead()!!
        assertEquals(2, model2.lastIndexId.id)
        val entity2 = model2.entities.first()
        assertEquals(3, entity2.properties.size)
        assertNull(entity2.properties.first().indexId)
        assertEquals(1, entity2.properties[1].indexId!!.id)
        assertEquals(2, entity2.properties[2].indexId!!.id)
    }

    @Test
    fun testRemoveEntity() {
        val model1 = syncBasicModel()
        val entity1 = model1.entities.first()

        val schema = basicSchema()
        addBasicPropertiesTo(addEntityTo(schema, "Entity2"))
        idSync = IdSync(file)
        idSync!!.sync(schema)

        val model2 = idSync!!.justRead()!!
        assertEquals(1, model2.retiredEntityUids!!.size)
        val entityRefIdDeleted = model2.retiredEntityUids!!.first()
        assertEquals(entity1.uid, entityRefIdDeleted)

        assertEquals(2, model2.retiredPropertyUids!!.size)
        assertEquals(entity1.properties[0].uid, model2.retiredPropertyUids!![0])
        assertEquals(entity1.properties[1].uid, model2.retiredPropertyUids!![1])
    }

    @Test
    fun testPropertiesWithSameNameIn2Entities() {
        val schema = basicSchema()
        val entity1 = addEntityTo(schema, "Entity1")
        val entity2 = addEntityTo(schema, "Entity2")
        addBasicPropertiesTo(entity1)
        addBasicPropertiesTo(entity2)
        val schemaProperty1 = entity1.properties[0]
        val schemaProperty2 = entity2.properties[0]
        idSync!!.sync(schema)

        val model = idSync!!.justRead()
        assertNotNull(model)
        assertNotSame(schemaProperty1, schemaProperty2)

        val property1 = idSync!!.get(schemaProperty1)
        val property2 = idSync!!.get(schemaProperty2)
        assertNotSame(property1, property2)
    }

    private fun basicSchema() = Schema(Schema.DEFAULT_NAME, 1, "pac.me")

    private fun syncBasicModel(): IdSyncModel {
        val schema = basicSchema()
        val entity1 = addEntityTo(schema, "Entity1")
        addBasicPropertiesTo(entity1)
        idSync!!.sync(schema)

        val model = idSync!!.justRead()!!
        return model
    }

    private fun addBasicPropertiesTo(entity: Entity, uid: Long? = null, onlyUidForLast: Boolean = false) {
        addPropertyTo(entity, "foo", if (onlyUidForLast) null else uid)
        addPropertyTo(entity, "bar", uid)
    }

    private fun addPropertyTo(entity: Entity, name: String, uid: Long? = null, indexed: Boolean = false) {
        val builder = entity.addProperty(PropertyType.String, name)
        if (uid != null) {
            builder.modelId(IdUid(0, uid))
        }
        if (indexed) {
            builder.indexAsc(null, false)
        }
    }

    private fun addEntityTo(schema: Schema, name: String, uid: Long? = null): Entity {
        val entity = schema.addEntity(name)
        entity.modelUid = uid
        return entity
    }
}