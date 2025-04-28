/*
 * ObjectBox Build Tools
 * Copyright (C) 2023-2025 ObjectBox Ltd.
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
import com.google.common.truth.Truth.assertWithMessage
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
        assertThat(schema).isNotNull()
        assertThat(schema.version).isEqualTo(1)
        assertThat(schema.defaultJavaPackage).isEqualTo("io.objectbox.processor.test")
        assertThat(schema.entities).hasSize(2) /* SimpleEntity and IdEntity */

        // assert entity
        val schemaEntity = schema.entities[0]
        assertThat(schemaEntity.className).isEqualTo(className)
        val dbName = "A"
        assertThat(schemaEntity.dbName).isEqualTo(dbName)
        assertThat(schemaEntity.hasAllArgsConstructor()).isFalse()

        // assert index
        assertThat(schemaEntity.indexes).hasSize(3) /* @Index, @HnswIndex and ToOne */
        val index = schemaEntity.indexes[0]
        assertThat(index.isUnique).isFalse()
        assertThat(index.indexFlags).isEqualTo(PropertyFlags.INDEXED)
        assertThat(index.properties).hasSize(1)
        val indexProperty = index.properties[0]

        // assert properties
        assertThat(schemaEntity.properties.size).isAtLeast(1)
        val schemaProperties = schemaEntity.properties
        for (prop in schemaProperties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertThat(prop.isIdAssignable).isTrue()
                    assertThat(prop.dbName).isEqualTo("id")
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
                    assertThat(prop.index).isEqualTo(index)
                    assertThat(prop).isEqualTo(indexProperty)
                }

                "namedProperty" -> {
                    assertThat(prop.dbName).isEqualTo("B")
                    assertType(prop, PropertyType.String)
                }

                "customType" -> {
                    assertThat(prop.customType).isEqualTo("io.objectbox.processor.test.SimpleEntity.SimpleEnum")
                    assertThat(prop.converter).isEqualTo("io.objectbox.processor.test.SimpleEntity.SimpleEnumConverter")
                    assertType(prop, PropertyType.Int, hasNonPrimitiveFlag = true)
                }

                "customTypes" -> {
                    assertThat(prop.customType).isEqualTo("java.util.List")
                    assertThat(prop.converter).isEqualTo("io.objectbox.processor.test.SimpleEntity.SimpleEnumListConverter")
                    assertType(prop, PropertyType.Int, hasNonPrimitiveFlag = true)
                }

                "dateNanoPrimitive" -> {
                    assertPrimitiveType(prop, PropertyType.DateNano)
                }

                "dateNano" -> {
                    assertType(prop, PropertyType.DateNano, hasNonPrimitiveFlag = true)
                }

                "idCompanion" -> {
                    assertThat(prop.isIdCompanion).isTrue()
                    assertType(prop, PropertyType.Date)
                }

                "stringFlexMap" -> {
                    prop.run {
                        assertThat(propertyType).isEqualTo(PropertyType.Flex)

                        assertThat(converter).isEqualTo("io.objectbox.converter.StringFlexMapConverter")
                        assertThat(converterClassName).isEqualTo("StringFlexMapConverter")

                        assertThat(customType).isEqualTo("java.util.Map")
                        assertThat(customTypeClassName).isEqualTo("Map")
                    }
                }

                "flexProperty" -> {
                    prop.run {
                        assertThat(propertyType).isEqualTo(PropertyType.Flex)

                        assertThat(converter).isEqualTo("io.objectbox.converter.FlexObjectConverter")
                        assertThat(converterClassName).isEqualTo("FlexObjectConverter")

                        assertThat(customType).isEqualTo("java.lang.Object")
                        assertThat(customTypeClassName).isEqualTo("Object")
                    }
                }

                "booleanArray" -> assertType(prop, PropertyType.BooleanArray)
                "shortArray" -> assertType(prop, PropertyType.ShortArray)
                "charArray" -> assertType(prop, PropertyType.CharArray)
                "intArray" -> assertType(prop, PropertyType.IntArray)
                "longArray" -> assertType(prop, PropertyType.LongArray)
                "floatArray" -> assertType(prop, PropertyType.FloatArray)
                "doubleArray" -> assertType(prop, PropertyType.DoubleArray)

                "floatArrayHnsw" -> assertType(prop, PropertyType.FloatArray)

                "toOneId" -> {
                    assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    assertThat(prop.virtualTargetName).isEqualTo("toOne")
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    // note: relations themselves are properly tested in RelationsTest
                }

                else -> Assert.fail("Found stray field '${prop.propertyName}' in schema.")
            }
        }

        // assert model
        val model = envFresh.readModel()
        assertThat(model.lastEntityId).isNotNull()
        assertThat(model.lastEntityId).isNotEqualTo(IdUid())
        assertThat(model.lastEntityId).isEqualTo(schema.lastEntityId)
        assertThat(model.lastIndexId).isNotNull()
        assertThat(model.lastIndexId).isNotEqualTo(IdUid())
        assertThat(model.lastIndexId).isEqualTo(schema.lastIndexId)

        // assert model entity
        val modelEntity = model.findEntity(dbName, null)
        assertThat(modelEntity).isNotNull()
        assertThat(modelEntity!!.id).isNotNull()
        assertThat(modelEntity.id).isNotEqualTo(IdUid())
        assertThat(modelEntity.id.id).isEqualTo(schemaEntity.modelId)
        assertThat(modelEntity.id.uid).isEqualTo(schemaEntity.modelUid)
        assertThat(modelEntity.lastPropertyId).isNotNull()
        assertThat(modelEntity.lastPropertyId).isNotEqualTo(IdUid())
        assertThat(modelEntity.lastPropertyId).isEqualTo(schemaEntity.lastPropertyId)

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
            "booleanArray",
            "shortArray",
            "charArray",
            "intArray",
            "longArray",
            "floatArray",
            "doubleArray",
            "floatArrayHnsw",
            "toOneId" // last
        )
        val modelProperties = modelEntity.properties
        assertThat(modelProperties.size).isAtLeast(1)

        modelProperties
            .filterNot { modelPropertyNames.contains(it.name) }
            .forEach { Assert.fail("Found stray property '${it.name}' in model file.") }

        modelPropertyNames.forEach { name ->
            val property = modelProperties.singleOrNull { it.name == name }
            assertWithMessage("Property '$name' not in model file").that(property).isNotNull()
            assertWithMessage("Property '$name' has no id").that(property!!.id).isNotNull()
            assertWithMessage("Property '$name' id:uid is 0:0").that(property.id).isNotEqualTo(IdUid())

            // Assert model property type and flags match schema.
            val schemaProperty = schemaProperties.find { it.dbName == name }!!
            assertThat(property.type).isEqualTo(schemaProperty.dbTypeId)
            assertThat(property.flags).isEqualTo(schemaProperty.propertyFlagsForModelFile)
            // Additional asserts for specific properties.
            when (name) {
                "indexedProperty" -> {
                    // has valid IdUid
                    assertThat(property.indexId).isNotNull()
                    assertThat(property.indexId).isNotEqualTo(IdUid())
                }

                "toOneId" -> {
                    // has valid IdUid
                    assertThat(property.indexId).isNotNull()
                    assertThat(property.indexId).isNotEqualTo(IdUid())

                    assertThat(property.relationTarget).isEqualTo(schemaProperty.targetEntity.dbName)

                    // is last index
                    assertThat(property.indexId).isEqualTo(model.lastIndexId)

                    // is last property
                    assertThat(property.id).isEqualTo(modelEntity.lastPropertyId)
                }
            }
        }

        // assert standalone relation
        val relations = modelEntity.relations!!
        assertThat(relations).isNotEmpty()
        for (relation in relations) {
            when (relation.name) {
                "toMany" -> {
                    assertThat(relation.id).isNotNull()
                    assertThat(relation.id).isNotEqualTo(IdUid())
                    assertThat(relation.targetId).isNotNull()
                    val targetEntityIdUid = model.findEntity(relatedClassName, null)!!.id
                    assertThat(targetEntityIdUid).isNotEqualTo(IdUid())
                    assertThat(relation.targetId).isEqualTo(targetEntityIdUid)
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