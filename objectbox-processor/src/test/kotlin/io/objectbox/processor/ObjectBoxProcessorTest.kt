/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.PropertyType
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ObjectBoxProcessorTest : BaseProcessorTest() {

    @Test
    fun testSelectPackage() {
        assertEquals("", ObjectBoxProcessor.selectPackage(listOf("")))
        assertEquals("a", ObjectBoxProcessor.selectPackage(listOf("a")))
        assertEquals("a", ObjectBoxProcessor.selectPackage(listOf("a", "b")))
        assertEquals("a", ObjectBoxProcessor.selectPackage(listOf("b", "a")))
        assertEquals("a.a", ObjectBoxProcessor.selectPackage(listOf("a.b", "a.a", "a.c")))
        assertEquals("a.a", ObjectBoxProcessor.selectPackage(listOf("a.a.b", "a.a.a", "a.a.c")))
        assertEquals("a.a", ObjectBoxProcessor.selectPackage(listOf("a.a.ab", "a.a.aa", "a.a.ac")))

        // Common parent package with different child hierarchy at the end
        assertEquals("a.b.c", ObjectBoxProcessor.selectPackage(listOf("a.b.c.x.a", "a.b.c.y.a", "a.b.c.z.a")))
        assertEquals("a.b.c", ObjectBoxProcessor.selectPackage(listOf("a.b.c.x", "a.b.c.y.a")))

        // Min 2 level needed for parent selection
        assertEquals("a.a", ObjectBoxProcessor.selectPackage(listOf("a.b", "a.a", "a.c")))

        // Different number of sub packages
        assertEquals("a.b.c", ObjectBoxProcessor.selectPackage(listOf("a.b.c", "a.b.c.d")))
    }

    @Test
    fun testSimpleEntity() {
        val className = "SimpleEntity"

        testGeneratedSources(className)

        testSchemaAndModel(className)
    }

    private fun testGeneratedSources(className: String) {
        // need stable model file + ids to verify sources match
        val environment = TestEnvironment("default.json")

        val compilation = environment.compileDaoCompat(className)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // assert generated files source trees
        assertGeneratedSourceMatches(compilation, "MyObjectBox")
        assertGeneratedSourceMatches(compilation, "${className}_")
        assertGeneratedSourceMatches(compilation, "${className}Cursor")
    }

    private fun testSchemaAndModel(className: String) {
        // ensure mode file is re-created on each run
        val environment = TestEnvironment("default-temp.json")
        environment.cleanModelFile()

        val compilation = environment.compileDaoCompat(className)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // assert schema
        val schema = environment.schema
        assertThat(schema).isNotNull()
        assertThat(schema.version).isEqualTo(1)
        assertThat(schema.defaultJavaPackage).isEqualTo("io.objectbox.processor.test")
        assertThat(schema.entities).hasSize(1)

        // assert entity
        val schemaEntity = schema.entities[0]
        assertThat(schemaEntity.className).isEqualTo(className)
        val dbName = "A"
        assertThat(schemaEntity.dbName).isEqualTo(dbName)
        assertThat(schemaEntity.isConstructors).isFalse()

        // assert index
        assertThat(schemaEntity.indexes).hasSize(1)
        val index = schemaEntity.indexes[0]
        assertThat(index.isNonDefaultName).isFalse()
        assertThat(index.isUnique).isFalse()
        assertThat(index.properties).hasSize(1)
        val indexProperty = index.properties[0]

        // assert properties
        assertThat(schemaEntity.properties.size).isAtLeast(1)
        for (prop in schemaEntity.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertThat(prop.isIdAssignable).isTrue()
                    assertThat(prop.dbName).isEqualTo("id")
                    assertType(prop, PropertyType.Long)
                }
                "simpleShortPrimitive" -> assertPrimitiveType(prop, PropertyType.Short)
                "simpleShort" -> assertType(prop, PropertyType.Short)
                "simpleIntPrimitive" -> assertPrimitiveType(prop, PropertyType.Int)
                "simpleInt" -> assertType(prop, PropertyType.Int)
                "simpleLongPrimitive" -> assertPrimitiveType(prop, PropertyType.Long)
                "simpleLong" -> assertType(prop, PropertyType.Long)
                "simpleFloatPrimitive" -> assertPrimitiveType(prop, PropertyType.Float)
                "simpleFloat" -> assertType(prop, PropertyType.Float)
                "simpleDoublePrimitive" -> assertPrimitiveType(prop, PropertyType.Double)
                "simpleDouble" -> assertType(prop, PropertyType.Double)
                "simpleBooleanPrimitive" -> assertPrimitiveType(prop, PropertyType.Boolean)
                "simpleBoolean" -> assertType(prop, PropertyType.Boolean)
                "simpleBytePrimitive" -> assertPrimitiveType(prop, PropertyType.Byte)
                "simpleByte" -> assertType(prop, PropertyType.Byte)
                "simpleDate" -> assertType(prop, PropertyType.Date)
                "simpleCharPrimitive" -> assertPrimitiveType(prop, PropertyType.Char)
                "simpleChar" -> assertType(prop, PropertyType.Char)
                "simpleString" -> assertType(prop, PropertyType.String)
                "simpleByteArray" -> assertType(prop, PropertyType.ByteArray)
                "transientField", "transientField2", "transientField3" ->
                    fail("Transient field should not be added to schema.")
                "indexedProperty" -> {
                    assertType(prop, PropertyType.Int)
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
                    assertType(prop, PropertyType.Int)
                }
                "customTypes" -> {
                    assertThat(prop.customType).isEqualTo("java.util.List")
                    assertThat(prop.converter).isEqualTo("io.objectbox.processor.test.SimpleEntity.SimpleEnumListConverter")
                    assertType(prop, PropertyType.Int)
                }
                else -> fail("Found stray field '${prop.propertyName}' in schema.")
            }
        }

        // assert model
        val model = environment.readModel()
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
        assertThat(modelEntity.id).isEqualTo(model.lastEntityId)
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
                "indexedProperty", // indexed
                "B",
                "customType",
                "customTypes" // last
        )
        val modelProperties = modelEntity.properties
        assertThat(modelProperties.size).isAtLeast(1)

        modelProperties
                .filterNot { modelPropertyNames.contains(it.name) }
                .forEach { fail("Found stray property '${it.name}' in model file.") }

        modelPropertyNames.forEach { name ->
            val property = modelProperties.singleOrNull { it.name == name }
            assertWithMessage("Property '$name' not in model file").that(property).isNotNull()
            assertWithMessage("Property '$name' has no id").that(property!!.id).isNotNull()
            assertWithMessage("Property '$name' id:uid is 0:0").that(property.id).isNotEqualTo(IdUid())

            when (name) {
                "indexedProperty" -> {
                    assertThat(property.indexId).isNotNull()
                    assertThat(property.indexId).isNotEqualTo(IdUid())
                    assertThat(property.indexId).isEqualTo(model.lastIndexId)
                }
                "customTypes" -> {
                    assertThat(property.id).isEqualTo(modelEntity.lastPropertyId)
                }
            }
        }
    }

    @Test
    fun simpleEntity_customMyObjectBoxPackage() {
        val className = "SimpleEntity"

        // need stable model file + ids to verify sources match
        val environment = TestEnvironment("default.json", "io.objectbox.processor.custom")

        val compilation = environment.compile(className)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // assert generated files source trees
        // MyObjectBox package and imports should be different
        val generatedFile = CompilationSubject.assertThat(compilation)
                .generatedSourceFile("io.objectbox.processor.custom.MyObjectBox")
        generatedFile.isNotNull()
        generatedFile.hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/MyObjectBox-custom.java"))
        // all other files should stay the same
        assertGeneratedSourceMatches(compilation, "${className}_")
        assertGeneratedSourceMatches(compilation, "${className}Cursor")
    }

    @Test
    fun testIdNotLong() {
        // test that instead of just failing compilation, processor warns if @Id is not Long
        val source = """
        package io.objectbox.processor.test;
        import io.objectbox.annotation.Entity; import io.objectbox.annotation.Id;

        @Entity
        public class NotLongEntity {
            @Id String id;
        }
        """
        val javaFileObject = JavaFileObjects.forSourceString("io.objectbox.processor.test.NotLongEntity", source)

        val environment = TestEnvironment("not-generated.json")

        val compilation = environment.compile(listOf(javaFileObject))
        CompilationSubject.assertThat(compilation).failed()

        CompilationSubject.assertThat(compilation).hadErrorContaining("An @Id property has to be of type Long")
    }

    @Test
    fun testMultipleAnnotations() {
        // test multiple (non-conflicting) annotations on a single property
        val className = "MultipleEntity"

        val environment = TestEnvironment("multiple-annotations.json")

        val compilation = environment.compile(className)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val entity = environment.schema.entities.single { it.className == className }

        // assert index
        assertThat(entity.indexes.size).isAtLeast(1)
        for (index in entity.indexes) {
            when (index.orderSpec) {
                "someString ASC" -> {
                    // just ensure it exists
                }
                else -> fail("Found stray index '${index.orderSpec}' in schema.")
            }
        }

        // assert property
        for (property in entity.properties) {
            when (property.propertyName) {
                "id" -> {
                }
                "someString" -> {
                    assertThat(property.dbName).isEqualTo("A")
                    assertThat(property.index).isEqualTo(entity.indexes[0])
                    assertThat(property.modelId.uid).isEqualTo(167962951075785953)
                    assertThat(property.customType).isEqualTo("io.objectbox.processor.test.$className.SimpleEnum")
                    assertThat(property.converter)
                            .isEqualTo("io.objectbox.processor.test.$className.SimpleEnumConverter")
                    assertType(property, PropertyType.String)
                }
                else -> fail("Found stray field '${property.propertyName}' in schema.")
            }
        }
    }

    @Test
    fun testMultiplePackages() {
        // tests if entities are in multiple packages, code is generated in the highest, lexicographically first package
        val entityTopFirstPackageName = "MultiPackageTopFirst"
        val entityTopLastPackageName = "MultiPackageTopLast"
        val entitySubPackageName = "MultiPackageSub"

        val environment = TestEnvironment("multiple-packages-temp.json")
        environment.cleanModelFile()

        // add to compiler ordered by length of package (unsorted)
        val compilation = environment.compile(entityTopLastPackageName, entityTopFirstPackageName, entitySubPackageName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val schema = environment.schema
        // "io.objectbox.processor.test" would actually be better (common parent package)
        assertThat(schema.defaultJavaPackage).isEqualTo("io.objectbox.processor.test.a_long")
    }

    @Test
    fun testAllArgsConstructor() {
        // tests if constructor with param for virtual property (to-one target id) and custom type is recognized
        // implicitly tests if all-args-constructor check can handle virtual and custom type properties
        val parentName = "ToOneParent"
        val childName = "ToOneAllArgs"

        val environment = TestEnvironment("to-one-all-args-temp.json")
        environment.cleanModelFile()

        val compilation = environment.compile(parentName, childName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val schema = environment.schema
        val child = schema.entities.single { it.className == childName }
        assertThat(child.isConstructors).isTrue()
    }

    @Test
    fun testKotlinByteCode() {
        val entityName = "SimpleKotlinEntity"

        val environment = TestEnvironment("kotlin-temp.json")
        environment.cleanModelFile()

        val compilation = environment.compile(entityName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // assert schema
        val schema = environment.schema
        assertThat(schema).isNotNull()
        assertThat(schema.version).isEqualTo(1)
        assertThat(schema.defaultJavaPackage).isEqualTo("io.objectbox.processor.test")
        assertThat(schema.entities).hasSize(1)

        // assert entity
        val entity = schema.entities[0]
        assertThat(entity.className).isEqualTo(entityName)
        assertThat(entity.isConstructors).isTrue()

        // assert properties
        for (prop in entity.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertThat(prop.isIdAssignable).isFalse()
                    assertThat(prop.dbName).isEqualTo("id")
                    assertPrimitiveType(prop, PropertyType.Long)
                }
                "simpleShort" -> assertType(prop, PropertyType.Short)
                "simpleInt" -> assertType(prop, PropertyType.Int)
                "simpleLong" -> assertType(prop, PropertyType.Long)
                "simpleFloat" -> assertType(prop, PropertyType.Float)
                "simpleDouble" -> assertType(prop, PropertyType.Double)
                "simpleBoolean" -> assertType(prop, PropertyType.Boolean)
                "isBoolean" -> assertType(prop, PropertyType.Boolean)
                "simpleByte" -> assertType(prop, PropertyType.Byte)
                "simpleDate" -> assertType(prop, PropertyType.Date)
                "simpleString" -> assertType(prop, PropertyType.String)
                "simpleByteArray" -> assertType(prop, PropertyType.ByteArray)
                else -> fail("Found stray field '${prop.propertyName}' in schema.")
            }
        }
    }

    /**
     * Tests if using an entity named like the io.objectbox.Property class works.
     */
    @Test
    fun entityNameConflictWithObjectBoxClass() {
        val parentName = "NameConflict"
        val childName = "Property" // <-- named like ObjectBox class io.objectbox.Property imported in generated classes

        val environment = TestEnvironment("name-conflict-temp.json")

        val compilation = environment.compile(parentName, childName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
    }

}
