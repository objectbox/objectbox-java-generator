package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import io.objectbox.generator.model.PropertyType
import org.junit.Assert
import org.junit.Test


/**
 * Tests related to entities in an inheritance chain (@BaseEntity, @Entity, regular classes).
 */
class InheritanceTest : BaseProcessorTest() {

    /**
     * Tests if properties from @BaseEntity class are used in ObjectBox, other super class and interface are ignored.
     */
    @Test
    fun testInheritance() {
        val nameBase = "InheritanceBase"
        val nameNoBase = "InheritanceNoBase"
        val nameSub = "InheritanceSub"
        val nameSubSub = "InheritanceSubSub"
        val nameInterface = "InheritanceInterface"

        val environment = TestEnvironment("inheritance.json")

        environment.compile(nameBase, nameNoBase, nameSub, nameSubSub, nameInterface)
            .assertThatIt { succeededWithoutWarnings() }

        // assert schema
        val schema = environment.schema
        assertThat(schema).isNotNull()
        assertThat(schema.entities).hasSize(2)

        // assert entity
        val schemaEntity = schema.entities.find { it.className == nameSub }
        assertThat(schemaEntity!!.properties.size).isEqualTo(4)
        for (prop in schemaEntity.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertPrimitiveType(prop, PropertyType.Long)
                }
                "baseString" -> assertType(prop, PropertyType.String)
                "subString" -> assertType(prop, PropertyType.String)
                "overriddenString" -> assertType(prop, PropertyType.String)
                "noBaseString" -> Assert.fail("Found non-@BaseEntity field '${prop.propertyName}' in schema.")
                else -> Assert.fail("Found stray field '${prop.propertyName}' in schema.")
            }
        }

        val schemaEntity2 = schema.entities.find { it.className == nameSubSub }
        assertThat(schemaEntity2!!.properties.size).isEqualTo(5)
        for (prop in schemaEntity2.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertPrimitiveType(prop, PropertyType.Long)
                }
                "baseString" -> assertType(prop, PropertyType.String)
                "subString" -> assertType(prop, PropertyType.String)
                "subSubString" -> assertType(prop, PropertyType.String)
                "overriddenString" -> assertType(prop, PropertyType.String)
                "noBaseString" -> Assert.fail("Found non-@BaseEntity field '${prop.propertyName}' in schema.")
                else -> Assert.fail("Found stray field '${prop.propertyName}' in schema.")
            }
        }

        // assert model
        val model = environment.readModel()

        val modelEntity = model.findEntity(nameSub, null)
        assertThat(modelEntity).isNotNull()
        val modelProperties = modelEntity!!.properties
        assertThat(modelProperties.size).isEqualTo(4)
        val modelPropertyNames = listOf(
                "id",
                "baseString",
                "subString",
                "overriddenString"
        )
        modelProperties
                .filterNot { modelPropertyNames.contains(it.name) }
                .forEach { Assert.fail("Found stray property '${it.name}' in model file.") }

        val modelEntity2 = model.findEntity(nameSubSub, null)
        assertThat(modelEntity2).isNotNull()
        val modelProperties2 = modelEntity2!!.properties
        assertThat(modelProperties2.size).isEqualTo(5)
        val modelPropertyNames2 = modelPropertyNames.plus("subSubString")
        modelProperties2
                .filterNot { modelPropertyNames2.contains(it.name) }
                .forEach { Assert.fail("Found stray property '${it.name}' in model file.") }
    }

    @Test
    fun genericBaseEntity() {
        val nameBase = "InheritanceBaseGeneric"
        val nameSub = "InheritanceSubGeneric"

        val environment = TestEnvironment("inheritance-generic.json")

        environment.compile(nameBase, nameSub)
            .assertThatIt { succeededWithoutWarnings() }

        // assert schema
        val schema = environment.schema
        assertThat(schema).isNotNull()
        assertThat(schema.entities).hasSize(1)

        val expectedNumberOfProperties = 3

        // assert schema entity
        val schemaEntity = schema.entities.find { it.className == nameSub }
        assertThat(schemaEntity!!.properties.size).isEqualTo(expectedNumberOfProperties)
        for (prop in schemaEntity.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertPrimitiveType(prop, PropertyType.Long)
                }
                "baseString" -> assertType(prop, PropertyType.String)
                "subString" -> assertType(prop, PropertyType.String)
                else -> Assert.fail("Found stray field '${prop.propertyName}' in schema.")
            }
        }

        // assert model
        val model = environment.readModel()

        val modelEntity = model.findEntity(nameSub, null)
        assertThat(modelEntity).isNotNull()
        val modelProperties = modelEntity!!.properties
        assertThat(modelProperties.size).isEqualTo(expectedNumberOfProperties)
        val modelPropertyNames = listOf(
                "id",
                "baseString",
                "subString"
        )
        modelProperties
                .filterNot { modelPropertyNames.contains(it.name) }
                .forEach { Assert.fail("Found stray property '${it.name}' in model file.") }
    }

    /**
     * Tests if both entities are used, properties from super @Entity class are inherited, the interface is ignored.
     */
    @Test
    fun testInheritanceBetweenEntities() {
        val nameSuper = "InheritanceEntity"
        val nameSub = "InheritanceSubEntity"
        val nameInterface = "InheritanceInterface"

        val environment = TestEnvironment("inheritance-entities.json")

        environment.compile(nameSuper, nameSub, nameInterface)
            .assertThatIt { succeededWithoutWarnings() }

        // assert schema
        val schema = environment.schema
        assertThat(schema).isNotNull()
        assertThat(schema.entities).hasSize(2)

        // assert entity
        val schemaEntity = schema.entities.find { it.className == nameSuper }
        assertThat(schemaEntity!!.properties.size).isEqualTo(2)
        for (prop in schemaEntity.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertPrimitiveType(prop, PropertyType.Long)
                }
                "simpleString" -> assertType(prop, PropertyType.String)
                else -> Assert.fail("Found stray field '${prop.propertyName}' in schema.")
            }
        }

        val schemaEntity2 = schema.entities.find { it.className == nameSub }
        assertThat(schemaEntity2!!.properties.size).isEqualTo(3)
        for (prop in schemaEntity2.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertPrimitiveType(prop, PropertyType.Long)
                }
                "simpleString" -> assertType(prop, PropertyType.String)
                "subString" -> assertType(prop, PropertyType.String)
                else -> Assert.fail("Found stray field '${prop.propertyName}' in schema.")
            }
        }

        // assert model
        val model = environment.readModel()

        val modelEntity = model.findEntity(nameSuper, null)
        assertThat(modelEntity).isNotNull()
        val modelProperties = modelEntity!!.properties
        assertThat(modelProperties.size).isEqualTo(2)
        val modelPropertyNames = listOf(
                "id",
                "simpleString"
        )
        modelProperties
                .filterNot { modelPropertyNames.contains(it.name) }
                .forEach { Assert.fail("Found stray property '${it.name}' in model file.") }

        val modelEntity2 = model.findEntity(nameSub, null)
        assertThat(modelEntity2).isNotNull()
        val modelProperties2 = modelEntity2!!.properties
        assertThat(modelProperties2.size).isEqualTo(3)
        val modelPropertyNames2 = modelPropertyNames.plus("subString")
        modelProperties2
                .filterNot { modelPropertyNames2.contains(it.name) }
                .forEach { Assert.fail("Found stray property '${it.name}' in model file.") }
    }

    /**
     * Tests that adding a duplicate property results in an error (and no crash).
     */
    @Test
    fun testInheritanceOverriddenField() {
        val nameBase = "InheritanceBase"
        val nameSub = "InheritanceSubOverride"

        TestEnvironment("inheritance-overridden.json", useTemporaryModelFile = true)
            .compile(nameBase, nameSub)
            .assertThatIt {
                failed()
                hadErrorContaining("Duplicate name \"overriddenString\"")
            }
    }

}