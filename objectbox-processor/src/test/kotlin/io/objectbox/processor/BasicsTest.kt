/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2024 ObjectBox Ltd.
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
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.model.PropertyType
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests some common functionality and some special cases for `@Entity` classes.
 */
class BasicsTest : BaseProcessorTest() {

    @Test
    fun selectPackage() {
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
    fun entity_noPackage() {
        val className = "SimpleEntityNoPackage"

        val environment = TestEnvironment("default-no-pkg.json", useTemporaryModelFile = true)

        environment.compile(className)
            .assertThatIt { succeededWithoutWarnings() }
    }

    @Test
    fun entity_multipleAnnotations() {
        // test multiple (non-conflicting) annotations on a single property
        val className = "MultipleEntity"

        val environment = TestEnvironment("multiple-annotations.json")

        environment.compile(className)
            .assertThatIt { succeededWithoutWarnings() }

        val entity = environment.schema.entities.single { it.className == className }

        // assert index
        assertThat(entity.indexes).hasSize(1)
        assertThat(entity.indexes[0].properties).hasSize(1)
        assertThat(entity.indexes[0].properties[0].propertyName).isEqualTo("someString")

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
                    assertType(property, PropertyType.String, hasNonPrimitiveFlag = true)
                }

                else -> fail("Found stray field '${property.propertyName}' in schema.")
            }
        }
    }

    @Test
    fun entity_multiplePackages() {
        // tests if entities are in multiple packages, code is generated in the highest, lexicographically first package
        val entityTopFirstPackageName = "MultiPackageTopFirst"
        val entityTopLastPackageName = "MultiPackageTopLast"
        val entitySubPackageName = "MultiPackageSub"

        val environment = TestEnvironment("multiple-packages.json", useTemporaryModelFile = true)

        // add to compiler ordered by length of package (unsorted)
        environment.compile(entityTopLastPackageName, entityTopFirstPackageName, entitySubPackageName)
            .assertThatIt { succeededWithoutWarnings() }

        val schema = environment.schema
        // "io.objectbox.processor.test" would actually be better (common parent package)
        assertThat(schema.defaultJavaPackage).isEqualTo("io.objectbox.processor.test.a_long")
    }

    @Test
    fun constructor_noArgCtrMissing_errors() {
        val noArgCtrMissing = """
        package io.objectbox.processor.test;
        
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        
        @Entity
        public class NoArgCtrMissing {
        
            @Id long id;
            String secondProperty;
        
            // No no-arg constructor.
            // public NoArgCtrMissing() { }
        
            // No all-args constructor either.
            public NoArgCtrMissing(String secondProperty) {
                this.secondProperty = secondProperty;
            }
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("io.objectbox.processor.test.NoArgCtrMissing", it)
        }

        val environment = TestEnvironment("ctr-no-arg-missing.json", useTemporaryModelFile = true)

        environment.compile(listOf(noArgCtrMissing))
            .assertThatIt {
                failed()
                hadErrorContaining("No-argument or all-properties constructor is required for @Entity class")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    /**
     * Ensures no-arg constructor check (see [constructor_noArgCtrMissing_errors]
     * does not trigger if there is an all-properties constructor.
     */
    @Test
    fun constructor_allPropertiesCtrOnly_works() {
        val allPropertiesCtrOnly = """
        package io.objectbox.processor.test;
        
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        
        @Entity
        public class AllPropsCtrOnly {
        
            @Id long id;
            String secondProperty;
        
            // No no-arg constructor.
            // public AllPropsCtrOnly() { }
        
            // But all-args constructor.
            public AllPropsCtrOnly(long id, String secondProperty) {
                this.id = id;
                this.secondProperty = secondProperty;
            }
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("io.objectbox.processor.test.AllPropsCtrOnly", it)
        }

        val environment = TestEnvironment("ctr-all-props.json", useTemporaryModelFile = true)

        environment.compile(listOf(allPropertiesCtrOnly))
            .assertThatIt { succeededWithoutWarnings() }
        assertThat(environment.isModelFileExists()).isTrue()
    }

    @Test
    fun constructor_customAndVirtualTypes() {
        // tests if constructor with param for virtual property (to-one target id) and custom type is recognized
        // implicitly tests if all-args-constructor check can handle virtual and custom type properties
        val parentName = "ToOneParent"
        val childName = "ToOneAllArgs"

        val environment = TestEnvironment("to-one-all-args.json", useTemporaryModelFile = true)

        environment.compile(parentName, childName)
            .assertThatIt { succeededWithoutWarnings() }

        val schema = environment.schema
        val child = schema.entities.single { it.className == childName }
        assertThat(child.hasAllArgsConstructor()).isTrue()
    }

    /**
     * Tests with a source file based on a decompiled Kotlin class.
     */
    @Test
    fun entity_kotlin() {
        val entityName = "SimpleKotlinEntity"

        val environment = TestEnvironment("kotlin.json", useTemporaryModelFile = true)

        environment.compile(entityName)
            .assertThatIt { succeededWithoutWarnings() }

        // assert schema
        val schema = environment.schema
        assertThat(schema).isNotNull()
        assertThat(schema.version).isEqualTo(1)
        assertThat(schema.defaultJavaPackage).isEqualTo("io.objectbox.processor.test")
        assertThat(schema.entities).hasSize(1)

        // assert entity
        val entity = schema.entities[0]
        assertThat(entity.className).isEqualTo(entityName)
        assertThat(entity.hasAllArgsConstructor()).isTrue()

        // assert properties
        for (prop in entity.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertThat(prop.isIdAssignable).isFalse()
                    assertThat(prop.dbName).isEqualTo("id")
                    assertPrimitiveType(prop, PropertyType.Long)
                }

                "simpleShort" -> assertType(prop, PropertyType.Short, hasNonPrimitiveFlag = true)
                "simpleInt" -> assertType(prop, PropertyType.Int, hasNonPrimitiveFlag = true)
                "simpleLong" -> assertType(prop, PropertyType.Long, hasNonPrimitiveFlag = true)
                "simpleFloat" -> assertType(prop, PropertyType.Float, hasNonPrimitiveFlag = true)
                "simpleDouble" -> assertType(prop, PropertyType.Double, hasNonPrimitiveFlag = true)
                "simpleBoolean" -> assertType(prop, PropertyType.Boolean, hasNonPrimitiveFlag = true)
                "simpleByte" -> assertType(prop, PropertyType.Byte, hasNonPrimitiveFlag = true)
                "simpleDate" -> assertType(prop, PropertyType.Date)
                "simpleString" -> assertType(prop, PropertyType.String)
                "simpleByteArray" -> assertType(prop, PropertyType.ByteArray)
                "isAnything" -> {
                    assertType(prop, PropertyType.String)
                    assertThat(prop.getterMethodName).isEqualTo("isAnything")
                }

                else -> fail("Found stray field '${prop.propertyName}' in schema.")
            }
        }
    }

    /**
     * Tests if using an entity named like the io.objectbox.Property class works.
     */
    @Test
    fun entity_nameConflictWithObjectBoxClass() {
        val parentName = "NameConflict"
        val childName = "Property" // <-- named like ObjectBox class io.objectbox.Property imported in generated classes

        val environment = TestEnvironment("name-conflict.json", useTemporaryModelFile = true)

        environment.compile(parentName, childName)
            .assertThatIt { succeededWithoutWarnings() }
    }

    @Test
    fun entity_withReservedBoxstoreProperty_errors() {
        val entityWithBoxstoreProperty = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class Example {
            @Id long id;
            String __boxStore;
        }
        """.let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        TestEnvironment("reserved-boxstore.json", useTemporaryModelFile = true)
            .compile(listOf(entityWithBoxstoreProperty))
            .assertThatIt {
                hadErrorContaining("A property can not be named `__boxStore`. Adding a BoxStore field for relations? Annotate it with @Transient.")
            }
    }

    @Test
    fun entity_duplicateWithSameName_shouldError() {
        val javaFileObjectOriginal = """
        package com.example.original;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class Example {
            @Id long id;
        }
        """.let {
            JavaFileObjects.forSourceString("com.example.original.Example", it)
        }
        val javaFileObjectDuplicate = """
        package com.example.duplicate;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;

        @Entity
        public class Example {
            @Id long id;
        }
        """.let {
            JavaFileObjects.forSourceString("com.example.duplicate.Example", it)
        }

        TestEnvironment("getter-matching-return.json", useTemporaryModelFile = true)
            .compile(listOf(javaFileObjectOriginal, javaFileObjectDuplicate))
            .assertThatIt {
                hadErrorContaining("There is already an entity class 'Example': 'com.example.original.Example'.")
            }
    }
}
