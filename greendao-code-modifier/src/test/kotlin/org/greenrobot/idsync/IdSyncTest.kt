package org.greenrobot.idsync

import org.eclipse.jdt.core.dom.TypeDeclaration
import org.greenrobot.greendao.codemodifier.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File

class IdSyncTest {

    val file = File.createTempFile("idsync-test", ".json")
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
    fun testModelRefId() {
        val modelRefId = ModelRefId()
        for (i in 0..100) {
            val x = modelRefId.create()
            modelRefId.verify(x)
            try {
                modelRefId.verify(x xor 1)
            } catch (e: RuntimeException) {
                // Expected
            }
            try {
                modelRefId.verify(x xor (1 shl 60))
            } catch (e: RuntimeException) {
                // Expected
            }
        }
    }

    @Test
    fun testIdSyncBasics() {
        val model = syncBasicModel()
        assertEquals(1, model.lastEntityId)

        assertEquals(1, model.entities.size)
        val entity = model.entities.first()
        assertEquals("Entity1", entity.name)
        assertEquals(1, entity.id)
        assertTrue(entity.refId > 1)

        assertEquals(2, entity.properties.size)
        assertEquals("foo", entity.properties[0].name)
        assertEquals(1, entity.properties[0].id)
        assertTrue(entity.properties[0].refId > 1)
        assertEquals(2, entity.properties[1].id)
        assertEquals("bar", entity.properties[1].name)
        assertTrue(entity.properties[1].refId > 1)
    }

    @Test
    fun testKeepParsedRefId() {
        val model1 = syncBasicModel()
        val entityRefId = model1.entities.first().refId
        val propertyRefId = model1.entities.first().properties.first().refId

        val properties = listOf(
                createProperty(name = "bla", refId = propertyRefId)
        )
        val entity1 = createEntity("Entity1A", properties, entityRefId)
        idSync = IdSync(file)
        idSync!!.sync(listOf(entity1))

        val model2 = idSync!!.justRead()!!
        assertEquals(1, model2.entities.size)
        val entity = model2.entities.first()
        assertEquals(entityRefId, entity.refId)
        assertEquals(1, entity.properties.size)
        assertEquals(propertyRefId, entity.properties.first().refId)
    }

    @Test
    fun testKeepIdsForMatchingNames() {
        val entityParsed = createEntity("Entity1", basicProperties())
        testKeepIdsForMatchingNames(entityParsed)
    }

    @Test
    fun testKeepIdsForMatchingNames_differentCase() {
        val properties = mutableListOf<ParsedProperty>(
                createProperty("FOO"),
                createProperty("bAr")
        )
        val entityParsed = createEntity("ENTITY1", properties)

        testKeepIdsForMatchingNames(entityParsed)
    }

    private fun testKeepIdsForMatchingNames(entityParsed: ParsedEntity) {
        val model1 = syncBasicModel()
        val entity1 = model1.entities.first()

        idSync = IdSync(file)
        idSync!!.sync(listOf(entityParsed))

        val model2 = idSync!!.justRead()!!
        assertEquals(1, model2.entities.size)
        val entity2 = model2.entities.first()
        assertEquals(entity1.id, entity2.id)
        assertEquals(entity1.refId, entity2.refId)

        assertEquals(2, entity2.properties.size)
        assertEquals(entity1.properties.first().id, entity2.properties.first().id)
        assertEquals(entity1.properties.first().refId, entity2.properties.first().refId)
        assertEquals(entity1.properties.last().id, entity2.properties.last().id)
        assertEquals(entity1.properties.last().refId, entity2.properties.last().refId)
    }

    @Test
    fun testAddEntity() {
        val model1 = syncBasicModel()
        val entityRefId = model1.entities.first().refId
        idSync = IdSync(file)
        val parsedEntities = listOf(
                createEntity("Entity1A", basicProperties(), entityRefId),
                createEntity("Entity2", basicProperties()))
        idSync!!.sync(parsedEntities)

        val model2 = idSync!!.justRead()!!
        assertEquals(2, model2.lastEntityId)
        assertEquals(2, model2.entities.size)
        val entity = model2.entities.last()
        assertEquals(2, entity.id)
        assertTrue(entity.refId > 1)
    }

    @Test
    fun testAddProperties() {
        val model1 = syncBasicModel()
        assertEquals(2, model1.entities.first().lastPropertyId)

        idSync = IdSync(file)
        val properties = listOf(createProperty("newOne"), createProperty("newTwo"))
        val parsedEntities = listOf(
                createEntity("Entity1", properties)
        )
        idSync!!.sync(parsedEntities)

        val model2 = idSync!!.justRead()!!
        val entity2 = model2.entities.first()
        assertEquals(4, entity2.lastPropertyId)
        assertEquals(3, entity2.properties.first().id)
        assertEquals(4, entity2.properties.last().id)
    }

    @Test(expected = IdSyncException::class)
    fun testAddEntityWithDuplicateRefId() {
        val model1 = syncBasicModel()
        val entityRefId = model1.entities.first().refId
        val parsedEntities = listOf(
                createEntity("Entity1A", basicProperties(), entityRefId),
                createEntity("Entity2", basicProperties(), entityRefId))
        idSync = IdSync(file)
        idSync!!.sync(parsedEntities)
    }

    @Test(expected = IdSyncException::class)
    fun testAddEntityPropertyWithDuplicateRefId() {
        val model1 = syncBasicModel()
        val propertyRefId = model1.entities.first().properties.first().refId
        val entityRefId = model1.entities.first().refId
        val properties = basicProperties()
        properties.forEach { it.refId = propertyRefId }
        val parsedEntities = listOf(createEntity("Entity1", properties, entityRefId))
        idSync = IdSync(file)
        idSync!!.sync(parsedEntities)
    }

    @Test(expected = IdSyncException::class)
    fun testEntityPropertyRefIdCollision() {
        val model1 = syncBasicModel()
        val entityRefId = model1.entities.first().refId
        val properties = basicProperties()
        properties.last().refId = entityRefId
        val parsedEntities = listOf(createEntity("Entity1", properties, entityRefId))
        idSync = IdSync(file)
        idSync!!.sync(parsedEntities)
    }

    @Test
    fun testAddIndexId() {
        val model1 = syncBasicModel()
        assertEquals(2, model1.entities.first().lastPropertyId)

        idSync = IdSync(file)
        val properties = listOf<ParsedProperty>(
                createProperty("foo"),
                createProperty("bar", null, true),
                createProperty("newAndIndexed", null, true)
        )
        val parsedEntities = listOf(
                createEntity("Entity1", properties)
        )
        idSync!!.sync(parsedEntities)

        val model2 = idSync!!.justRead()!!
        assertEquals(2, model2.lastIndexId)
        val entity2 = model2.entities.first()
        assertEquals(3, entity2.properties.size)
        assertNull(entity2.properties.first().indexId)
        assertEquals(1, entity2.properties[1].indexId)
        assertEquals(2, entity2.properties[2].indexId)
    }

    @Test
    fun testRemoveEntity() {
        val model1 = syncBasicModel()
        val entity1 = model1.entities.first()

        val parsedEntities = listOf(
                createEntity("Entity2", basicProperties())
        )
        idSync = IdSync(file)
        idSync!!.sync(parsedEntities)
        val model2 = idSync!!.justRead()!!
        assertEquals(1, model2.retiredEntityRefIds!!.size)
        val entityRefIdDeleted = model2.retiredEntityRefIds!!.first()
        assertEquals(entity1.refId, entityRefIdDeleted)

        assertEquals(2, model2.retiredPropertyRefIds!!.size)
        assertEquals(entity1.properties[0].refId, model2.retiredPropertyRefIds!![0])
        assertEquals(entity1.properties[1].refId, model2.retiredPropertyRefIds!![1])
    }

    @Test
    fun testPropertiesWithSameNameIn2Entities() {
        val entity1 = createEntity("Entity1", basicProperties())
        val entity2 = createEntity("Entity2", basicProperties())
        val parsedProperty1 = entity1.properties[0]
        val parsedProperty2 = entity2.properties[0]
        idSync!!.sync(listOf(entity1, entity2))

        val model = idSync!!.justRead()!!
        assertNotSame(parsedProperty1, parsedProperty2)

        val property1 = idSync!!.get(parsedProperty1)
        val property2 = idSync!!.get(parsedProperty2)
        assertNotSame(property1, property2)
    }

    private fun syncBasicModel(): IdSyncModel {
        val entity1 = createEntity("Entity1", basicProperties())
        idSync!!.sync(listOf(entity1))

        val model = idSync!!.justRead()!!
        return model
    }

    private fun basicProperties(): MutableList<ParsedProperty> {
        val properties = mutableListOf<ParsedProperty>(
                createProperty("foo"),
                createProperty("bar")
        )
        return properties
    }

    private fun createProperty(name: String, refId: Long? = null, indexed: Boolean = false): ParsedProperty {
        return ParsedProperty(
                variable = Variable(VariableType("java.lang.String", false, "String"), name + "_"),
                dbName = name,
                index = if (indexed) PropertyIndex("dummyname", false) else null,
                refId = refId
        )
    }

    private fun createEntity(name: String, properties: List<ParsedProperty>, refId: Long? = null): ParsedEntity {
        val typeDec = Mockito.mock(TypeDeclaration::class.java)
        return ParsedEntity(
                name = name + "_",
                schema = "default",
                active = false,
                properties = properties,
                transientFields = emptyList(),
                legacyTransientFields = emptyList(),
                constructors = emptyList(),
                methods = emptyList(),
                node = typeDec,
                imports = emptyList(),
                packageName = "pac.me",
                dbName = name,
                refId = refId,
                oneRelations = emptyList(),
                manyRelations = emptyList(),
                sourceFile = File("dummy-src-$name"),
                source = "dummy-src-$name",
                keepSource = false,
                createInDb = true,
                generateConstructors = true,
                generateGettersSetters = true,
                protobufClassName = null,
                notNullAnnotation = null,
                lastFieldDeclaration = null
        )
    }
}