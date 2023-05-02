package io.objectbox.processor

import com.google.common.truth.Truth
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.PropertyType
import io.objectbox.model.PropertyFlags
import org.junit.Assert
import org.junit.Test


/**
 * Tests all supported properties, including basic relations, and the most important annotations
 * with a simple entity class.
 */
class SimpleEntityTest : BaseProcessorTest() {

    @Test
    fun simpleEntity() {
        val className = "SimpleEntity"
        val relatedClassName = "IdEntity"

        // Test generated sources
        // need stable model file + ids to verify sources match
        val envStable = TestEnvironment("default.json")
        envStable.compileDaoCompat(className, relatedClassName)
            .assertThatIt { succeededWithoutWarnings() }
            // assert generated files source trees
            .assertGeneratedSourceMatches("MyObjectBox")
            .assertGeneratedSourceMatches("${className}_")
            .assertGeneratedSourceMatches("${className}Cursor")

        // Test schema and model
        // ensure mode file is re-created on each run
        val envFresh = TestEnvironment("default.json", useTemporaryModelFile = true)

        envFresh.compileDaoCompat(className, relatedClassName)
            .assertThatIt { succeededWithoutWarnings() }

        // assert schema
        val schema = envFresh.schema
        Truth.assertThat(schema).isNotNull()
        Truth.assertThat(schema.version).isEqualTo(1)
        Truth.assertThat(schema.defaultJavaPackage).isEqualTo("io.objectbox.processor.test")
        Truth.assertThat(schema.entities).hasSize(2) /* SimpleEntity and IdEntity */

        // assert entity
        val schemaEntity = schema.entities[0]
        Truth.assertThat(schemaEntity.className).isEqualTo(className)
        val dbName = "A"
        Truth.assertThat(schemaEntity.dbName).isEqualTo(dbName)
        Truth.assertThat(schemaEntity.hasAllArgsConstructor()).isFalse()

        // assert index
        Truth.assertThat(schemaEntity.indexes).hasSize(2) /* @Index and ToOne */
        val index = schemaEntity.indexes[0]
        Truth.assertThat(index.isUnique).isFalse()
        Truth.assertThat(index.indexFlags).isEqualTo(PropertyFlags.INDEXED)
        Truth.assertThat(index.properties).hasSize(1)
        val indexProperty = index.properties[0]

        // assert properties
        Truth.assertThat(schemaEntity.properties.size).isAtLeast(1)
        val schemaProperties = schemaEntity.properties
        for (prop in schemaProperties) {
            when (prop.propertyName) {
                "id" -> {
                    Truth.assertThat(prop.isPrimaryKey).isTrue()
                    Truth.assertThat(prop.isIdAssignable).isTrue()
                    Truth.assertThat(prop.dbName).isEqualTo("id")
                    assertPrimitiveType(prop, PropertyType.Long)
                }

                "simpleShortPrimitive" -> assertPrimitiveType(prop, PropertyType.Short)
                "simpleShort" -> assertType(prop, PropertyType.Short, hasNonPrimitiveFlag = true)
                "simpleIntPrimitive" -> assertPrimitiveType(prop, PropertyType.Int)
                "simpleInt" -> assertType(prop, PropertyType.Int, hasNonPrimitiveFlag = true)
                "simpleLongPrimitive" -> assertPrimitiveType(prop, PropertyType.Long)
                "simpleLong" -> assertType(prop, PropertyType.Long, hasNonPrimitiveFlag = true)
                "simpleFloatPrimitive" -> assertPrimitiveType(prop, PropertyType.Float)
                "simpleFloat" -> assertType(prop, PropertyType.Float, hasNonPrimitiveFlag = true)
                "simpleDoublePrimitive" -> assertPrimitiveType(prop, PropertyType.Double)
                "simpleDouble" -> assertType(prop, PropertyType.Double, hasNonPrimitiveFlag = true)
                "simpleBooleanPrimitive" -> assertPrimitiveType(prop, PropertyType.Boolean)
                "simpleBoolean" -> assertType(prop, PropertyType.Boolean, hasNonPrimitiveFlag = true)
                "simpleBytePrimitive" -> assertPrimitiveType(prop, PropertyType.Byte)
                "simpleByte" -> assertType(prop, PropertyType.Byte, hasNonPrimitiveFlag = true)
                "simpleDate" -> assertType(prop, PropertyType.Date)
                "simpleCharPrimitive" -> assertPrimitiveType(prop, PropertyType.Char)
                "simpleChar" -> assertType(prop, PropertyType.Char, hasNonPrimitiveFlag = true)
                "simpleString" -> assertType(prop, PropertyType.String)
                "simpleByteArray" -> assertType(prop, PropertyType.ByteArray)
                "simpleStringArray" -> assertType(prop, PropertyType.StringArray)
                "simpleStringList" -> assertType(prop, PropertyType.StringArray, hasNonPrimitiveFlag = true)
                "transientField", "transientField2", "transientField3" ->
                    Assert.fail("Transient field should not be added to schema.")

                "indexedProperty" -> {
                    assertType(prop, PropertyType.Int, hasNonPrimitiveFlag = true)
                    Truth.assertThat(prop.index).isEqualTo(index)
                    Truth.assertThat(prop).isEqualTo(indexProperty)
                }

                "namedProperty" -> {
                    Truth.assertThat(prop.dbName).isEqualTo("B")
                    assertType(prop, PropertyType.String)
                }

                "customType" -> {
                    Truth.assertThat(prop.customType).isEqualTo("io.objectbox.processor.test.SimpleEntity.SimpleEnum")
                    Truth.assertThat(prop.converter).isEqualTo("io.objectbox.processor.test.SimpleEntity.SimpleEnumConverter")
                    assertType(prop, PropertyType.Int, hasNonPrimitiveFlag = true)
                }

                "customTypes" -> {
                    Truth.assertThat(prop.customType).isEqualTo("java.util.List")
                    Truth.assertThat(prop.converter).isEqualTo("io.objectbox.processor.test.SimpleEntity.SimpleEnumListConverter")
                    assertType(prop, PropertyType.Int, hasNonPrimitiveFlag = true)
                }

                "dateNanoPrimitive" -> {
                    assertPrimitiveType(prop, PropertyType.DateNano)
                }

                "dateNano" -> {
                    assertType(prop, PropertyType.DateNano, hasNonPrimitiveFlag = true)
                }

                "idCompanion" -> {
                    Truth.assertThat(prop.isIdCompanion).isTrue()
                    assertType(prop, PropertyType.Date)
                }

                "stringFlexMap" -> {
                    prop.run {
                        Truth.assertThat(propertyType).isEqualTo(PropertyType.Flex)

                        Truth.assertThat(converter).isEqualTo("io.objectbox.converter.StringFlexMapConverter")
                        Truth.assertThat(converterClassName).isEqualTo("StringFlexMapConverter")

                        Truth.assertThat(customType).isEqualTo("java.util.Map")
                        Truth.assertThat(customTypeClassName).isEqualTo("Map")
                    }
                }

                "flexProperty" -> {
                    prop.run {
                        Truth.assertThat(propertyType).isEqualTo(PropertyType.Flex)

                        Truth.assertThat(converter).isEqualTo("io.objectbox.converter.FlexObjectConverter")
                        Truth.assertThat(converterClassName).isEqualTo("FlexObjectConverter")

                        Truth.assertThat(customType).isEqualTo("java.lang.Object")
                        Truth.assertThat(customTypeClassName).isEqualTo("Object")
                    }
                }

                "toOneId" -> {
                    Truth.assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    Truth.assertThat(prop.virtualTargetName).isEqualTo("toOne")
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    // note: relations themselves are properly tested in RelationsTest
                }

                else -> Assert.fail("Found stray field '${prop.propertyName}' in schema.")
            }
        }

        // assert model
        val model = envFresh.readModel()
        Truth.assertThat(model.lastEntityId).isNotNull()
        Truth.assertThat(model.lastEntityId).isNotEqualTo(IdUid())
        Truth.assertThat(model.lastEntityId).isEqualTo(schema.lastEntityId)
        Truth.assertThat(model.lastIndexId).isNotNull()
        Truth.assertThat(model.lastIndexId).isNotEqualTo(IdUid())
        Truth.assertThat(model.lastIndexId).isEqualTo(schema.lastIndexId)

        // assert model entity
        val modelEntity = model.findEntity(dbName, null)
        Truth.assertThat(modelEntity).isNotNull()
        Truth.assertThat(modelEntity!!.id).isNotNull()
        Truth.assertThat(modelEntity.id).isNotEqualTo(IdUid())
        Truth.assertThat(modelEntity.id.id).isEqualTo(schemaEntity.modelId)
        Truth.assertThat(modelEntity.id.uid).isEqualTo(schemaEntity.modelUid)
        Truth.assertThat(modelEntity.lastPropertyId).isNotNull()
        Truth.assertThat(modelEntity.lastPropertyId).isNotEqualTo(IdUid())
        Truth.assertThat(modelEntity.lastPropertyId).isEqualTo(schemaEntity.lastPropertyId)

        // assert model properties
        val modelPropertyNames = listOf(
            "id",
            "simpleShortPrimitive",
            "simpleShort",
            "simpleIntPrimitive",
            "simpleInt",
            "simpleLongPrimitive",
            "simpleLong",
            "simpleFloatPrimitive",
            "simpleFloat",
            "simpleDoublePrimitive",
            "simpleDouble",
            "simpleBooleanPrimitive",
            "simpleBoolean",
            "simpleBytePrimitive",
            "simpleByte",
            "simpleDate",
            "simpleCharPrimitive",
            "simpleChar",
            "simpleString",
            "simpleByteArray",
            "simpleStringArray",
            "simpleStringList",
            "indexedProperty", // indexed
            "B",
            "customType",
            "customTypes",
            "dateNanoPrimitive",
            "dateNano",
            "idCompanion",
            "stringFlexMap",
            "flexProperty",
            "toOneId" // last
        )
        val modelProperties = modelEntity.properties
        Truth.assertThat(modelProperties.size).isAtLeast(1)

        modelProperties
            .filterNot { modelPropertyNames.contains(it.name) }
            .forEach { Assert.fail("Found stray property '${it.name}' in model file.") }

        modelPropertyNames.forEach { name ->
            val property = modelProperties.singleOrNull { it.name == name }
            Truth.assertWithMessage("Property '$name' not in model file").that(property).isNotNull()
            Truth.assertWithMessage("Property '$name' has no id").that(property!!.id).isNotNull()
            Truth.assertWithMessage("Property '$name' id:uid is 0:0").that(property.id).isNotEqualTo(IdUid())

            // Assert model property type and flags match schema.
            val schemaProperty = schemaProperties.find { it.dbName == name }!!
            Truth.assertThat(property.type).isEqualTo(schemaProperty.dbTypeId)
            Truth.assertThat(property.flags).isEqualTo(schemaProperty.propertyFlagsForModelFile)
            // Additional asserts for specific properties.
            when (name) {
                "indexedProperty" -> {
                    // has valid IdUid
                    Truth.assertThat(property.indexId).isNotNull()
                    Truth.assertThat(property.indexId).isNotEqualTo(IdUid())
                }

                "toOneId" -> {
                    // has valid IdUid
                    Truth.assertThat(property.indexId).isNotNull()
                    Truth.assertThat(property.indexId).isNotEqualTo(IdUid())

                    Truth.assertThat(property.relationTarget).isEqualTo(schemaProperty.targetEntity.dbName)

                    // is last index
                    Truth.assertThat(property.indexId).isEqualTo(model.lastIndexId)

                    // is last property
                    Truth.assertThat(property.id).isEqualTo(modelEntity.lastPropertyId)
                }
            }
        }

        // assert standalone relation
        val relations = modelEntity.relations!!
        Truth.assertThat(relations).isNotEmpty()
        for (relation in relations) {
            when (relation.name) {
                "toMany" -> {
                    Truth.assertThat(relation.id).isNotNull()
                    Truth.assertThat(relation.id).isNotEqualTo(IdUid())
                    Truth.assertThat(relation.targetId).isNotNull()
                    val targetEntityIdUid = model.findEntity(relatedClassName, null)!!.id
                    Truth.assertThat(targetEntityIdUid).isNotEqualTo(IdUid())
                    Truth.assertThat(relation.targetId).isEqualTo(targetEntityIdUid)
                }

                else -> Assert.fail("Found stray relation '${relation.name}' in model file.")
            }
        }
    }

    @Test
    fun simpleEntity_customMyObjectBoxPackage() {
        val className = "SimpleEntity"
        val relatedClassName = "IdEntity"

        // need stable model file + ids to verify sources match
        val environment = TestEnvironment("default.json", "io.objectbox.processor.custom")

        environment.compile(className, relatedClassName)
            .assertThatIt { succeededWithoutWarnings() }
            // assert generated files source trees
            // MyObjectBox package and imports should be different
            .assertGeneratedSourceMatches("io.objectbox.processor.custom.MyObjectBox", "MyObjectBox-custom.java")
            // all other files should stay the same
            .assertGeneratedSourceMatches("${className}_")
            .assertGeneratedSourceMatches("${className}Cursor")
    }

}