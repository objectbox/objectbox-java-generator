package org.greenrobot.idsync

import org.eclipse.jdt.core.dom.TypeDeclaration
import org.greenrobot.greendao.codemodifier.ParsedEntity
import org.greenrobot.greendao.codemodifier.ParsedProperty
import org.greenrobot.greendao.codemodifier.Variable
import org.greenrobot.greendao.codemodifier.VariableType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File

class IdSyncTest {

    val file = File.createTempFile("idsync-test", "json")
    var idSync: IdSync? = null

    @Before
    fun initIdSync() {
        file.delete()
        idSync = IdSync(file)
    }

    @After
    fun deleteFile() {
        file.delete()
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
    fun testKeepRefId() {
        val model1 = syncBasicModel()
        val entityRefId = model1.entities.first().refId
        val propertyRefId = model1.entities.first().properties.first().refId

        val properties = listOf<ParsedProperty>(
                createProperty(name = "bla", refId = propertyRefId)
        )
        val entity1 = createEntity("Entity1A", properties, entityRefId)
        idSync = IdSync(file)
        idSync!!.sync(listOf<ParsedEntity>(entity1))

        val model2 = idSync!!.justRead()!!
        assertEquals(1, model2.entities.size)
        val entity = model2.entities.first()
        assertEquals(entityRefId, entity.refId)
        assertEquals(1, entity.properties.size)
        assertEquals(propertyRefId, entity.properties.first().refId)
    }

    @Test
    fun testAddEntity() {
        val model1 = syncBasicModel()
        val entityRefId = model1.entities.first().refId
        val properties = listOf<ParsedProperty>(
                createProperty("foo", null),
                createProperty("bar", null)
        )
        idSync = IdSync(file)
        val parsedEntities = listOf<ParsedEntity>(
                createEntity("Entity1A", properties, entityRefId),
                createEntity("Entity2", properties))
        idSync!!.sync(parsedEntities)

        val model2 = idSync!!.justRead()!!
        assertEquals(2, model2.lastEntityId)
        assertEquals(2, model2.entities.size)
        val entity = model2.entities.last()
        assertEquals(2, entity.id)
        assertTrue(entity.refId > 1)
    }

    private fun syncBasicModel(): IdSyncModel {
        val properties = listOf<ParsedProperty>(
                createProperty("foo", null),
                createProperty("bar", null)
        )
        val entity1 = createEntity("Entity1", properties)
        idSync!!.sync(listOf<ParsedEntity>(entity1))

        val model = idSync!!.justRead()!!
        return model
    }

    private fun createProperty(name: String, refId: Long? = null): ParsedProperty {
        return ParsedProperty(
                variable = Variable(VariableType("java.lang.String", false, "String"), name + "_"),
                dbName = name,
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