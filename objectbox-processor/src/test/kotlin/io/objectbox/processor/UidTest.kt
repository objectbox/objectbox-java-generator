package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationSubject
import org.junit.Assert
import org.junit.Test


/**
 * Tests @Uid annotations.
 */
class UidTest {

    /**
     * Test that @Uid values are picked up.
     */
    @Test
    fun testUid() {
        val className = "UidEntity"

        val environment = TestEnvironment("uid.json")

        val compilation = environment.compile(className)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val entity = environment.schema.entities[0]
        assertThat(entity.modelUid).isEqualTo(2361091532752425885)

        val property = entity.properties.single { it.propertyName == "uidProperty" }
        assertThat(property.modelId.uid).isEqualTo(7287685531948841886)
    }

    @Test
    fun testUidEmpty() {
        val environment = TestEnvironment("uid.json", copyModelFile = true)
        val compilation = environment.compile("UidEmptyEntity")
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Uid(2361091532752425885L)")
    }

    @Test
    fun testUidNew() {
        val environment = TestEnvironment("uid-new-uid-pool.json", copyModelFile = true)
        val modelBefore = environment.readModel()
        Assert.assertEquals(1, modelBefore.newUidPool.size)

        val compilation = environment.compile("UidNewEntity")
        CompilationSubject.assertThat(compilation).succeeded()
        val model = environment.readModel()

        val newUid = modelBefore.newUidPool.single()
        val entity = model.findEntity("UidEntity", null)!!
        Assert.assertEquals(newUid, entity.uid)
        Assert.assertEquals(modelBefore.lastEntityId.id + 1, model.lastEntityId.id)
        Assert.assertEquals(newUid, model.lastEntityId.uid)
    }

    @Test
    fun testPropertyUidEmpty() {
        val environment = TestEnvironment("uid.json", copyModelFile = true)
        val compilation = environment.compile("UidPropertyEmptyEntity")
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Uid(7287685531948841886L)")
    }

    @Test
    fun testPropertyUidNew() {
        val environment = TestEnvironment("uid-new-uid-pool.json", copyModelFile = true)
        val entityName = "UidPropertyNewEntity"
        val modelBefore = environment.readModel()
        Assert.assertEquals(1, modelBefore.newUidPool.size)
        val entityBefore = modelBefore.findEntity("UidEntity", null)!!

        val compilation = environment.compile(entityName)
        CompilationSubject.assertThat(compilation).succeeded()
        val model = environment.readModel()
        val entity = model.findEntity("UidEntity", null)!!

        val property = entity.properties.single { it.name == "uidProperty" }
        val newUid = modelBefore.newUidPool.single()
        Assert.assertEquals(newUid, property.uid)
        Assert.assertEquals(entityBefore.lastPropertyId.id + 1, entity.lastPropertyId.id)
        Assert.assertEquals(newUid, entity.lastPropertyId.uid)

        Assert.assertEquals(0, model.newUidPool.size)
    }

    @Test
    fun testToOneUidEmpty() {
        val environment = TestEnvironment("uid-relation.json", copyModelFile = true)
        val compilation = environment.compile("UidToOneEmptyEntity")
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Uid(4055646088440538446L)")
    }

    @Test
    fun testToOneUidNew() {
        val environment = TestEnvironment("uid-relation-new-uid-pool.json", copyModelFile = true)
        val entityName = "UidToOneNewEntity"
        val modelBefore = environment.readModel()
        Assert.assertEquals(1, modelBefore.newUidPool.size)
        val entityBefore = modelBefore.findEntity("UidRelationNewEntity", null)!!

        val compilation = environment.compile(entityName)
        CompilationSubject.assertThat(compilation).succeeded()
        val model = environment.readModel()
        val entity = model.findEntity("UidRelationNewEntity", null)!!

        val property = entity.properties.single { it.name == "toOneId" }
        val newUid = modelBefore.newUidPool.single()
        Assert.assertEquals(newUid, property.uid)
        Assert.assertEquals(entityBefore.lastPropertyId.id + 1, entity.lastPropertyId.id)
        Assert.assertEquals(newUid, entity.lastPropertyId.uid)

        Assert.assertEquals(0, model.newUidPool.size)
    }

    @Test
    fun testToManyUidNew() {
        val environment = TestEnvironment("uid-relation-new-uid-pool.json", copyModelFile = true)
        val entityName = "UidToManyNewEntity"
        val modelBefore = environment.readModel()
        Assert.assertEquals(1, modelBefore.newUidPool.size)

        val compilation = environment.compile(entityName)
        CompilationSubject.assertThat(compilation).succeeded()
        val model = environment.readModel()
        val entity = model.findEntity("UidRelationNewEntity", null)!!

        val relation = entity.relations.single { it.name == "toManyStandalone" }
        val newUid = modelBefore.newUidPool.single()
        Assert.assertEquals(newUid, relation.uid)
        Assert.assertEquals(modelBefore.lastRelationId.id + 1, model.lastRelationId.id)
        Assert.assertEquals(newUid, model.lastRelationId.uid)

        Assert.assertEquals(0, model.newUidPool.size)
    }

    @Test
    fun testToManyUidEmpty() {
        val environment = TestEnvironment("uid-relation.json", copyModelFile = true)
        val compilation = environment.compile("UidToManyEmptyEntity")
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Uid(823077930327936262L)")
    }

}