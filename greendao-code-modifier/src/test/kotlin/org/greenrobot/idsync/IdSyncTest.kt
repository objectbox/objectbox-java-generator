package org.greenrobot.idsync

import org.eclipse.jdt.core.dom.AST
import org.greenrobot.greendao.codemodifier.ParsedEntity
import org.greenrobot.greendao.codemodifier.ParsedProperty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File
import java.util.*

class IdSyncTest {
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
        val file = File.createTempFile("idsync-test", "json")
        file.delete()
        val parsedEntities = ArrayList<ParsedEntity>()
        val properties = ArrayList<ParsedProperty>()
        val entity1 = createEntity(properties, "Entity1")
        parsedEntities.add(entity1)
        val idSync = IdSync(file, parsedEntities)
        idSync.sync()
        val model = idSync.justRead(file)
        assertNotNull(model)
        assertEquals(1, model!!.entities.size);
        assertEquals("Entity1", model!!.entities.first().name);
    }

    private fun createEntity(properties: ArrayList<ParsedProperty>, name: String): ParsedEntity {
        val typeDec = AST.newAST(AST.JLS8).newTypeDeclaration();
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