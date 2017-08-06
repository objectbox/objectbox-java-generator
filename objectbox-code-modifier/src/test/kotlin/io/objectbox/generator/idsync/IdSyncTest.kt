package io.objectbox.generator.idsync

import io.objectbox.codemodifier.*
import org.greenrobot.eclipse.jdt.core.dom.TypeDeclaration
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
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

        val properties = listOf(
                createParsedProperty(name = "bla", refId = propertyRefId)
        )
        val entity1 = createParsedEntity("Entity1A", properties, entityRefId)
        idSync = IdSync(file)
        idSync!!.sync(listOf(entity1))

        val model2 = idSync!!.justRead()!!
        assertEquals(1, model2.entities.size)
        val entity = model2.entities.first()
        assertEquals(entityRefId, entity.uid)
        assertEquals(1, entity.properties.size)
        assertEquals(propertyRefId, entity.properties.first().uid)
    }

    @Test
    fun testKeepIdsForMatchingNames() {
        val entityParsed = createParsedEntity("Entity1", basicProperties())
        testKeepIdsForMatchingNames(entityParsed)
    }

    @Test
    fun testKeepIdsForMatchingNames_differentCase() {
        val properties = mutableListOf<ParsedProperty>(
                createParsedProperty("FOO"),
                createParsedProperty("bAr")
        )
        val entityParsed = createParsedEntity("ENTITY1", properties)

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
        idSync = IdSync(file)
        val parsedEntities = listOf(
                createParsedEntity("Entity1A", basicProperties(), entityRefId),
                createParsedEntity("Entity2", basicProperties()))
        idSync!!.sync(parsedEntities)

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

        idSync = IdSync(file)
        val properties = listOf(createParsedProperty("newOne"), createParsedProperty("newTwo"))
        val parsedEntities = listOf(
                createParsedEntity("Entity1", properties)
        )
        idSync!!.sync(parsedEntities)

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
        val parsedEntities = listOf(
                createParsedEntity("Entity1A", basicProperties(), entityRefId),
                createParsedEntity("Entity2", basicProperties(), entityRefId))
        idSync = IdSync(file)
        idSync!!.sync(parsedEntities)
    }

    @Test(expected = IdSyncException::class)
    fun testAddEntityPropertyWithDuplicateRefId() {
        val model1 = syncBasicModel()
        val propertyRefId = model1.entities.first().properties.first().uid
        val entityRefId = model1.entities.first().uid
        val properties = basicProperties()
        properties.forEach { it.uid = propertyRefId }
        val parsedEntities = listOf(createParsedEntity("Entity1", properties, entityRefId))
        idSync = IdSync(file)
        idSync!!.sync(parsedEntities)
    }

    @Test(expected = IdSyncException::class)
    fun testEntityPropertyRefIdCollision() {
        val model1 = syncBasicModel()
        val entityRefId = model1.entities.first().uid
        val properties = basicProperties()
        properties.last().uid = entityRefId
        val parsedEntities = listOf(createParsedEntity("Entity1", properties, entityRefId))
        idSync = IdSync(file)
        idSync!!.sync(parsedEntities)
    }

    @Test
    fun testAddIndexId() {
        val model1 = syncBasicModel()
        assertEquals(2, model1.entities.first().lastPropertyId.id)

        idSync = IdSync(file)
        val properties = listOf<ParsedProperty>(
                createParsedProperty("foo"),
                createParsedProperty("bar", null, true),
                createParsedProperty("newAndIndexed", null, true)
        )
        val parsedEntities = listOf(
                createParsedEntity("Entity1", properties)
        )
        idSync!!.sync(parsedEntities)

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

        val parsedEntities = listOf(
                createParsedEntity("Entity2", basicProperties())
        )
        idSync = IdSync(file)
        idSync!!.sync(parsedEntities)
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
        val entity1 = createParsedEntity("Entity1", basicProperties())
        val entity2 = createParsedEntity("Entity2", basicProperties())
        val parsedProperty1 = entity1.properties[0]
        val parsedProperty2 = entity2.properties[0]
        idSync!!.sync(listOf(entity1, entity2))

        val model = idSync!!.justRead()
        assertNotNull(model)
        assertNotSame(parsedProperty1, parsedProperty2)

        val property1 = idSync!!.get(parsedProperty1)
        val property2 = idSync!!.get(parsedProperty2)
        assertNotSame(property1, property2)
    }

    private fun syncBasicModel(): IdSyncModel {
        val entity1 = createParsedEntity("Entity1", basicProperties())
        idSync!!.sync(listOf(entity1))

        val model = idSync!!.justRead()!!
        return model
    }

    private fun basicProperties(): MutableList<ParsedProperty> {
        val properties = mutableListOf<ParsedProperty>(
                createParsedProperty("foo"),
                createParsedProperty("bar")
        )
        return properties
    }

    private fun createParsedProperty(name: String, refId: Long? = null, indexed: Boolean = false): ParsedProperty {
        return ParsedProperty(
                variable = Variable(VariableType("java.lang.String", false, "String"), name + "_"),
                dbName = name,
                index = if (indexed) PropertyIndex("dummyname", false) else null,
                uid = refId
        )
    }

    private fun createParsedEntity(name: String, properties: List<ParsedProperty>, refId: Long? = null): ParsedEntity {
        val typeDec = Mockito.mock(TypeDeclaration::class.java)
        return ParsedEntity(
                name = name + "_",
                schema = "default",
                properties = properties.toMutableList(),
                transientFields = emptyList(),
                constructors = emptyList(),
                methods = emptyList(),
                node = typeDec,
                imports = emptyList(),
                packageName = "pac.me",
                dbName = name,
                uid = refId,
                toOneRelations = emptyList(),
                toManyRelations = emptyList(),
                sourceFile = File("dummy-src-$name"),
                source = "dummy-src-$name",
                keepSource = false,
                createInDb = true,
                generateConstructors = true,
                protobufClassName = null,
                notNullAnnotation = null,
                lastFieldDeclaration = null
        )
    }
}