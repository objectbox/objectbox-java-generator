/*
 * ObjectBox Build Tools
 * Copyright (C) 2018-2024 ObjectBox Ltd.
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

package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
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

        environment.compile(className)
            .assertThatIt { succeededWithoutWarnings() }

        val entity = environment.schema.entities[0]
        assertThat(entity.modelUid).isEqualTo(2361091532752425885)

        val property = entity.properties.single { it.propertyName == "uidProperty" }
        assertThat(property.modelId.uid).isEqualTo(7287685531948841886)
    }

    @Test
    fun testUidEmpty() {
        val environment = TestEnvironment("uid.json")
        // Note: suggested UID added to newUidPool in model file.
        environment.compile("UidEmptyEntity", modelExpectedToChange = true)
            .assertThatIt {
                failed()
                hadErrorContaining("@Uid(2361091532752425885L)")
            }
    }

    @Test
    fun testUidNew() {
        val environment = TestEnvironment("uid-new-uid-pool.json")
        val modelBefore = environment.readModel()
        Assert.assertEquals(1, modelBefore.newUidPool.size)

        environment.compile("UidNewEntity", modelExpectedToChange = true)
            .assertThatIt { succeededWithoutWarnings() }
        val model = environment.readModel()

        val newUid = modelBefore.newUidPool.single()
        val entity = model.findEntity("UidEntity", null)!!
        Assert.assertEquals(newUid, entity.uid)
        Assert.assertEquals(modelBefore.lastEntityId.id + 1, model.lastEntityId.id)
        Assert.assertEquals(newUid, model.lastEntityId.uid)
    }

    @Test
    fun testPropertyUidEmpty() {
        // Note: suggested UID added to newUidPool in model file.
        TestEnvironment("uid.json")
            .compile("UidPropertyEmptyEntity", modelExpectedToChange = true)
            .assertThatIt {
                failed()
                hadErrorContaining("@Uid(7287685531948841886L)")
            }
    }

    @Test
    fun testPropertyUidNew() {
        val environment = TestEnvironment("uid-new-uid-pool.json")
        val entityName = "UidPropertyNewEntity"
        val modelBefore = environment.readModel()
        Assert.assertEquals(1, modelBefore.newUidPool.size)
        val entityBefore = modelBefore.findEntity("UidEntity", null)!!

        environment.compile(entityName, modelExpectedToChange = true)
            .assertThatIt { succeededWithoutWarnings() }
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
        // Note: suggested UID added to newUidPool in model file.
        TestEnvironment("uid-relation.json")
            .compile("UidToOneEmptyEntity", modelExpectedToChange = true)
            .assertThatIt {
                failed()
                hadErrorContaining("@Uid(4055646088440538446L)")
            }
    }

    @Test
    fun testToOneUidNew() {
        val environment = TestEnvironment("uid-relation-new-uid-pool.json")
        val entityName = "UidToOneNewEntity"
        val modelBefore = environment.readModel()
        Assert.assertEquals(1, modelBefore.newUidPool.size)
        val entityBefore = modelBefore.findEntity("UidRelationNewEntity", null)!!

        environment.compile(entityName, modelExpectedToChange = true)
            .assertThatIt { succeededWithoutWarnings() }
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
        val environment = TestEnvironment("uid-relation-new-uid-pool.json")
        val entityName = "UidToManyNewEntity"
        val modelBefore = environment.readModel()
        Assert.assertEquals(1, modelBefore.newUidPool.size)

        environment.compile(entityName, modelExpectedToChange = true)
            .assertThatIt { succeededWithoutWarnings() }
        val model = environment.readModel()
        val entity = model.findEntity("UidRelationNewEntity", null)!!

        val relation = entity.relations!!.single { it.name == "toManyStandalone" }
        val newUid = modelBefore.newUidPool.single()
        Assert.assertEquals(newUid, relation.uid)
        Assert.assertEquals(modelBefore.lastRelationId.id + 1, model.lastRelationId.id)
        Assert.assertEquals(newUid, model.lastRelationId.uid)

        Assert.assertEquals(0, model.newUidPool.size)
    }

    @Test
    fun testToManyUidEmpty() {
        TestEnvironment("uid-relation.json")
            .compile("UidToManyEmptyEntity", modelExpectedToChange = true)
            .assertThatIt {
                failed()
                hadErrorContaining("@Uid(823077930327936262L)")
            }
    }

}