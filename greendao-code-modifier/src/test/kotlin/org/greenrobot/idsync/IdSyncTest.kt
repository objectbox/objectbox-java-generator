package org.greenrobot.idsync

import org.eclipse.jdt.core.dom.TypeDeclaration
import org.greenrobot.greendao.codemodifier.ParsedEntity
import org.greenrobot.greendao.codemodifier.ParsedProperty
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import java.util.*

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
    fun testIdSync() {
        idSync!!
        val parsedEntities = ArrayList<ParsedEntity>()
        val properties = ArrayList<ParsedProperty>()
        val entity1 = createEntity(properties, "Entity1")
        parsedEntities.add(entity1)
        idSync!!.sync(parsedEntities)

        val model = idSync!!.justRead()!!
        assertEquals(1, model.entities.size);
        val entity = model.entities.first()
        assertEquals("Entity1", entity.name);
        assertEquals(1, entity.id);
        assertTrue(entity.refId > 0);
        assertEquals(1, model.lastEntityId);
    }

    private fun createEntity(properties: ArrayList<ParsedProperty>, name: String): ParsedEntity {
        val typeDec = Mockito.mock(TypeDeclaration::class.java)
        return ParsedEntity(
                name = name,
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
                dbName = null,
                refId = null,
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