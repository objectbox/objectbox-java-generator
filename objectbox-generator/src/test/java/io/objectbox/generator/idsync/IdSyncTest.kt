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

package io.objectbox.generator.idsync

import com.google.common.truth.Truth.assertThat
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
import io.objectbox.generator.model.ToManyStandalone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class IdSyncTest {

    private val file: File = File.createTempFile("idsync-test", ".json")
    private var idSync: IdSync? = null

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
//            val shifted = 1L.shl(i)
//            assertThrows(IdSyncException::class.java) {
//                modelUid.verify(x xor shifted)
//            }
//            assertThrows(IdSyncException::class.java) {
//                modelUid.verify(x xor (1 shl 60))
//            }
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
        schema.finish()
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
        schema.finish()

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
        schema.finish()

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
        schema.finish()

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
        schema.finish()

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
        schema.finish()

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
        schema.finish()

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
        schema.finish()

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
    fun removeLastEntity_keepsLastUids() {
        // Create a model with 2 entities
        val modelBefore = syncTestModel {
            addTestEntity("Entity1")
                .addTestProperty("foo")
                .addTestProperty("bar", indexed = true)
                .addTestToMany("oneToMany")
            addTestEntity("Entity2")
                .addTestProperty("foo", indexed = true)
                .addTestProperty("bar")
                .addTestToMany("twoToMany")
        }

        // Sync with model where last entity is removed
        val modelAfter = syncTestModel {
            addTestEntity("Entity1", uid = modelBefore.findEntity("Entity1").uid)
                .addTestProperty("foo")
                .addTestProperty("bar", indexed = true)
                .addTestToMany("oneToMany")
        }

        // Even though entity was removed, its UIDs should be last as no new entity was added
        assertEquals(modelBefore.lastEntityId, modelAfter.lastEntityId)
        assertEquals(modelBefore.lastIndexId, modelAfter.lastIndexId)
        assertEquals(modelBefore.lastRelationId, modelAfter.lastRelationId)
    }

    @Test
    fun removeEntityAndAddEntity_uidsRetired() {
        val model1 = syncTestModel {
            addTestEntity("Entity1")
                .addTestProperty("foo")
                .addTestProperty("bar", indexed = true)
                .addTestToMany("oneToMany")
        }
        val entity1 = model1.entities.first()

        val model2 = syncTestModel {
            addTestEntity("Entity2")
                .addTestProperty("foo", indexed = true)
                .addTestProperty("bar")
                .addTestToMany("twoToMany")
        }

        // Entity1 UID is retired
        model2.retiredEntityUids!!.let {
            assertEquals(1, it.size)
            val entityRefIdDeleted = it.first()
            assertEquals(entity1.uid, entityRefIdDeleted)
        }
        // Entity1 property UIDs retired
        model2.retiredPropertyUids!!.let {
            assertEquals(2, it.size)
            assertEquals(entity1.properties[0].uid, it[0])
            assertEquals(entity1.properties[1].uid, it[1])
        }
        // Entity1 index UIDs retired
        model2.retiredIndexUids!!.let {
            assertEquals(1, it.size)
            assertEquals(entity1.properties[1].indexId!!.uid, it[0])
        }
        // Entity1 relation UIDs retired
        model2.retiredRelationUids!!.let {
            assertEquals(1, it.size)
            assertEquals(entity1.relations!![0].uid, it[0])
        }
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
        schema.finish()

        idSync!!.sync(schema)

        val model = idSync!!.justRead()
        assertNotNull(model)
        assertNotSame(schemaProperty1, schemaProperty2)

        val property1 = idSync!!.get(schemaProperty1)
        val property2 = idSync!!.get(schemaProperty2)
        assertNotSame(property1, property2)
    }

    @Test
    fun syncEnabled_addToExistingEntity_fails() {
        val model1 = syncTestModel {
            addTestEntity("Entity1")
        }

        val exception = assertThrows(
            IdSyncException::class.java
        ) {
            syncTestModel {
                addTestEntity("Entity1", uid = model1.findEntity("Entity1").uid)
                    .apply { isSyncEnabled = true }
            }
        }
        assertThat(exception.message).contains("Can't change Sync annotation for existing entity 'Entity1'.")
    }

    @Test
    fun syncEnabled_removeFromExistingEntity_fails() {
        val model1 = syncTestModel {
            addTestEntity("Entity1")
                .apply { isSyncEnabled = true }
        }

        val exception = assertThrows(
            IdSyncException::class.java
        ) {
            syncTestModel {
                addTestEntity("Entity1", uid = model1.findEntity("Entity1").uid)
            }
        }
        assertThat(exception.message).contains("Can't change Sync annotation for existing entity 'Entity1'.")
    }

    @Test
    fun syncSharedGlobalIds_enableExistingEntity_fails() {
        val model1 = syncTestModel {
            addTestEntity("Entity1")
                .apply {
                    isSyncEnabled = true
                    isSyncSharedGlobalIds = false
                }
        }

        assertThrows(
            IdSyncException::class.java
        ) {
            syncTestModel {
                addTestEntity("Entity1", uid = model1.findEntity("Entity1").uid)
                    .apply {
                        isSyncEnabled = true
                        isSyncSharedGlobalIds = true
                    }
            }
        }
    }

    @Test
    fun syncSharedGlobalIds_disableExistingEntity_fails() {
        val model1 = syncTestModel {
            addTestEntity("Entity1")
                .apply {
                    isSyncEnabled = true
                    isSyncSharedGlobalIds = true
                }
        }

        assertThrows(
            IdSyncException::class.java
        ) {
            syncTestModel {
                addTestEntity("Entity1", uid = model1.findEntity("Entity1").uid)
                    .apply {
                        isSyncEnabled = true
                        isSyncSharedGlobalIds = false
                    }
            }
        }
    }

    private fun basicSchema() = Schema(Schema.DEFAULT_NAME, 1, "pac.me")

    /**
     * Creates basic schema, applies the given function, syncs schema with test model and returns it.
     */
    private fun syncTestModel(block: Schema.() -> Unit): IdSyncModel {
        val schema = basicSchema()
        block(schema)
        schema.finish()
        val idSync = IdSync(file)
        idSync.sync(schema)
        return idSync.justRead()!!
    }

    private fun syncBasicModel(): IdSyncModel {
        val schema = basicSchema()
        val entity1 = addEntityTo(schema, "Entity1")
        addBasicPropertiesTo(entity1)
        schema.finish()

        idSync!!.sync(schema)

        return idSync!!.justRead()!!
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
            builder.index()
        }
    }

    private fun Entity.addTestProperty(name: String, uid: Long? = null, indexed: Boolean = false): Entity {
        val builder = addProperty(PropertyType.String, name)
        if (uid != null) {
            builder.modelId(IdUid(0, uid))
        }
        if (indexed) {
            builder.index()
        }
        return this
    }

    /**
     * Adds to-many relation to itself.
     */
    private fun Entity.addTestToMany(name: String): Entity {
        addToMany(
            ToManyStandalone(
                name = name,
                dbName = null,
                targetEntityName = this.className,
                isFieldAccessible = true,
                uid = null,
                externalName = null,
                externalTypeId = null,
                externalTypeExpression = null
            ), this
        )
        return this
    }

    private fun addEntityTo(schema: Schema, name: String, uid: Long? = null): Entity {
        val entity = schema.addEntity(name)
        entity.modelUid = uid
        return entity
    }

    private fun Schema.addTestEntity(name: String, uid: Long? = null, addTestProperties: Boolean = false): Entity {
        val entity = addEntity(name)
        entity.modelUid = uid
        if (addTestProperties) addBasicPropertiesTo(entity)
        return entity
    }

    private fun IdSyncModel.findEntity(name: String): io.objectbox.generator.idsync.Entity {
        return entities.find { it.name == name }!!
    }
}