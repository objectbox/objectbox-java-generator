package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
import io.objectbox.generator.model.ToMany
import org.junit.Assert.fail
import org.junit.Test

class ObjectBoxProcessorTest {

    class TestEnvironment {
        val modelFilesBaseBath = "src/test/resources/objectbox-models/"
        val processorOptionBasePath = "-A${ObjectBoxProcessor.OPTION_MODEL_PATH}=$modelFilesBaseBath"

        val processor = ObjectBoxProcessorShim()

        val schema: Schema
            get() = processor.schema!!

        fun compile(modelFile: String, vararg files: String): Compilation {
            val fileObjects = files.map { JavaFileObjects.forResource("$it.java") }
            return javac()
                    .withProcessors(processor)
                    .withOptions("$processorOptionBasePath$modelFile")
                    .compile(fileObjects)
        }

        fun compileDaoCompat(modelFile: String, vararg files: String): Compilation {
            val fileObjects = files.map { JavaFileObjects.forResource("$it.java") }
            return javac()
                    .withProcessors(processor)
                    .withOptions(
                            "$processorOptionBasePath$modelFile"
                            // disabled as compat DAO currently requires entity property getters/setters
//                        "-A${ObjectBoxProcessor.OPTION_DAO_COMPAT}=true"
                    )
                    .compile(fileObjects)
        }
    }

    @Test
    fun testProcessor() {
        val entityName = "SimpleEntity"

        val environment = TestEnvironment()

        val compilation = environment.compileDaoCompat("default.json", entityName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // assert generated files source trees
        assertGeneratedSourceMatches(compilation, "MyObjectBox")
        assertGeneratedSourceMatches(compilation, "${entityName}_")
        assertGeneratedSourceMatches(compilation, "${entityName}Cursor")

        // assert schema
        val schema = environment.schema
        assertThat(schema).isNotNull()
        assertThat(schema.version).isEqualTo(1)
        assertThat(schema.defaultJavaPackage).isEqualTo("io.objectbox.processor.test")
        assertThat(schema.lastEntityId).isEqualTo(IdUid(1, 4858050548069557694))
        assertThat(schema.lastIndexId).isEqualTo(IdUid(0, 0))
        assertThat(schema.entities).hasSize(1)

        // assert entity
        val entity = schema.entities[0]
        assertThat(entity.className).isEqualTo(entityName)
        assertThat(entity.dbName).isEqualTo("A")
        assertThat(entity.modelId).isEqualTo(1)
        assertThat(entity.modelUid).isEqualTo(4858050548069557694)
        assertThat(entity.lastPropertyId).isEqualTo(IdUid(23, 4772590935549770830))
        assertThat(entity.isConstructors).isFalse()

        // assert index
        for (index in entity.indexes) {
            when (index.orderSpec) {
                "indexedProperty ASC" -> {
                    assertThat(index.isUnique).isFalse()
                    assertThat(index.isNonDefaultName).isFalse()
                }
                else -> fail("Found stray index '${index.orderSpec}' in schema.")
            }
        }

        // assert properties
        for (prop in entity.properties) {
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
                "simpleString" -> assertType(prop, PropertyType.String)
                "simpleByteArray" -> assertType(prop, PropertyType.ByteArray)
                "transientField", "transientField2", "transientField3" ->
                    fail("Transient field should not be added to schema.")
                "indexedProperty" -> assertType(prop, PropertyType.Int)
                "namedProperty" -> {
                    assertThat(prop.dbName).isEqualTo("B")
                    assertType(prop, PropertyType.String)
                }
                "uidProperty" -> {
                    assertThat(prop.modelId.uid).isEqualTo(3817914863709111804)
                    assertType(prop, PropertyType.Long)
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
    }

    @Test
    fun testRelation() {
        // tested relation: a child has a parent
        val parentName = "RelationParent"
        val childName = "RelationChild"

        val environment = TestEnvironment()

        val compilation = environment.compile("relation.json", parentName, childName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // assert generated files source trees
        assertGeneratedSourceMatches(compilation, "${childName}_")
        assertGeneratedSourceMatches(compilation, "${childName}Cursor")

        // assert schema
        val schema = environment.schema
        assertThat(schema).isNotNull()
        assertThat(schema.entities).hasSize(2)

        // assert entity
        val parent = schema.entities.single { it.className == parentName }
        val child = schema.entities.single { it.className == childName }

        // assert properties
        for (prop in child.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertThat(prop.isIdAssignable).isFalse()
                    assertThat(prop.dbName).isEqualTo("id")
                    assertType(prop, PropertyType.Long)
                }
                "parentId" -> {
                    assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    assertThat(prop.virtualTargetName).isNull()
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    assertToOneIndexAndRelation(child, parent, prop, toOneName = "parent", toOneFieldName = "parentToOne")
                }
                else -> fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
    }

    @Test
    fun testToOne() {
        // tested relation: a child has a parent
        val parentName = "ToOneParent"
        val childName = "ToOneChild"

        val environment = TestEnvironment()

        val compilation = environment.compile("to-one.json", parentName, childName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // assert generated files source trees
        assertGeneratedSourceMatches(compilation, "${childName}_")
        assertGeneratedSourceMatches(compilation, "${childName}Cursor")

        // assert schema
        val schema = environment.schema
        assertThat(schema).isNotNull()
        assertThat(schema.entities).hasSize(2)

        // assert entity
        val parent = schema.entities.single { it.className == parentName }
        val child = schema.entities.single { it.className == childName }

        // assert properties
        for (prop in child.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertThat(prop.isIdAssignable).isFalse()
                    assertThat(prop.dbName).isEqualTo("id")
                    assertType(prop, PropertyType.Long)
                }
                "parentId" -> {
                    assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    assertThat(prop.virtualTargetName).isEqualTo("parent")
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    assertToOneIndexAndRelation(child, parent, prop, toOneName = "parent")
                }
                else -> fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
    }

    @Test
    fun testToOneAllArgsConstructor() {
        // tests if constructor with param for virtual property is recognized
        // implicitly tests if all-args-constructor check can handle virtual properties
        val parentName = "ToOneParent"
        val childName = "ToOneAllArgs"

        val environment = TestEnvironment()

        val compilation = environment.compile("to-one-all-args.json", parentName, childName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val schema = environment.schema
        val child = schema.entities.single { it.className == childName }
        assertThat(child.isConstructors).isTrue()
    }

    @Test
    fun testToOneNoBoxStoreField() {
        // tested relation: a child has a parent
        val parentName = "ToOneParent"
        val childName = "ToOneNoBoxStore"

        val environment = TestEnvironment()

        val compilation = environment.compile("to-one-no-boxstore.json", parentName, childName)
        CompilationSubject.assertThat(compilation).failed()

        CompilationSubject.assertThat(compilation).hadErrorCount(1)
        CompilationSubject.assertThat(compilation).hadErrorContainingMatch("in '$childName' add a field '__boxStore'")
    }

    @Test
    fun testBacklinkList() {
        // tested relation: a parent has children
        val parentName = "BacklinkListParent"
        val childName = "BacklinkListChild"

        val environment = TestEnvironment()

        val compilation = environment.compile("backlink-list.json", parentName, childName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        assertGeneratedSourceMatches(compilation, "${parentName}_")
        assertGeneratedSourceMatches(compilation, "${parentName}Cursor")

        assertToManySchema(environment.schema, parentName, childName)
    }

    @Test
    fun testBacklinkToMany() {
        // tested relation: a parent has children
        val parentName = "BacklinkToManyParent"
        val childName = "BacklinkToManyChild"

        val environment = TestEnvironment()

        val compilation = environment.compile("backlink-to-many.json", parentName, childName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        assertGeneratedSourceMatches(compilation, "${parentName}_")
        assertGeneratedSourceMatches(compilation, "${parentName}Cursor")

        assertToManySchema(environment.schema, parentName, childName)
    }

    @Test
    fun testBacklinkMissing() {
        // test if missing @Backlink on ToMany field is detected
        val parentName = "BacklinkMissingParent"
        val childName = "IdEntity"

        val environment = TestEnvironment()

        val compilation = environment.compile("backlink-missing.json", parentName, childName)
        CompilationSubject.assertThat(compilation).failed()

        CompilationSubject.assertThat(compilation).hadErrorContaining("ToMany field must be annotated with @Backlink")
    }

    @Test
    fun testBacklinkMultiple() {
        // test if multiple to-one fields for one @Backlink (without 'to' value) are detected
        val parentName = "BacklinkMultipleParent"
        val childName = "BacklinkMultipleChild"

        val environment = TestEnvironment()

        val compilation = environment.compile("backlink-multiple.json", parentName, childName)
        CompilationSubject.assertThat(compilation).failed()

        CompilationSubject.assertThat(compilation).hadErrorContaining("Set name of one to-one relation of '$childName'")
    }

    @Test
    fun testKotlinByteCode() {
        val entityName = "SimpleKotlinEntity"

        val environment = TestEnvironment()

        val compilation = environment.compile("kotlin.json", entityName)
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
                else -> fail("Found stray field '${prop.propertyName}' in schema.")
            }
        }
    }

    private fun assertToManySchema(schema: Schema, parentName: String, childName: String) {
        // assert schema
        assertThat(schema).isNotNull()
        assertThat(schema.entities).hasSize(2)

        // assert parent properties
        val parent = schema.entities.single { it.className == parentName }
        for (prop in parent.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertThat(prop.isIdAssignable).isFalse()
                    assertThat(prop.dbName).isEqualTo("id")
                    assertType(prop, PropertyType.Long)
                }
                else -> fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
        // assert child properties
        val child = schema.entities.single { it.className == childName }
        for (prop in child.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertThat(prop.isIdAssignable).isFalse()
                    assertThat(prop.dbName).isEqualTo("id")
                    assertType(prop, PropertyType.Long)
                }
                "parentId" -> {
                    assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    assertThat(prop.virtualTargetName).isEqualTo("parent")
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    assertToOneIndexAndRelation(child, parent, prop, toOneName = "parent")
                    assertToManyRelation(parent, child, prop)
                }
                else -> fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
    }

    private fun assertToManyRelation(parent: Entity, child: Entity, prop: Property) {
        for (toManyRelation in parent.toManyRelations) {
            when (toManyRelation.name) {
                "children" -> {
                    assertThat(toManyRelation.sourceEntity).isEqualTo(parent)
                    assertThat(toManyRelation.targetEntity).isEqualTo(child)
                    val toMany = toManyRelation as ToMany
                    assertThat(toMany.targetProperties).hasLength(1)
                    assertThat(toMany.targetProperties[0]).isEqualTo(prop)
                    // generator takes care of populating sourceProperties if we do not set them, so do not assert here
                }
                else -> fail("Found stray toManyRelation '${toManyRelation.name}' in schema.")
            }
        }
    }

    private fun assertToOneIndexAndRelation(child: Entity, parent: Entity, prop: Property, toOneName: String,
                                            toOneFieldName: String = toOneName) {
        // assert index
        assertThat(child.indexes).hasSize(1)
        val foreignKeyIndex = child.indexes[0]
        assertThat(foreignKeyIndex.properties[0]).isEqualTo(prop)

        // assert to one relation
        assertThat(child.toOneRelations).hasSize(1)
        for (toOneRelation in child.toOneRelations) {
            when (toOneRelation.name) {
                toOneName -> {
                    assertThat(toOneRelation.targetEntity).isEqualTo(parent)
                    assertThat(toOneRelation.targetIdProperty).isEqualTo(prop)
                    assertThat(toOneRelation.nameToOne).isEqualTo(toOneFieldName)
                }
                else -> fail("Found stray toOneRelation '${toOneRelation.name}' in schema.")
            }
        }
    }

    private fun assertGeneratedSourceMatches(compilation: Compilation, simpleName: String) {
        val generatedFile = CompilationSubject.assertThat(compilation)
                .generatedSourceFile("io.objectbox.processor.test.${simpleName}")
        generatedFile.isNotNull()
        generatedFile.hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/${simpleName}.java"))
    }

    private fun assertPrimitiveType(prop: Property, type: PropertyType) {
        assertThat(prop.propertyType).isEqualTo(type)
        assertThat(prop.isNotNull).isTrue()
        assertThat(prop.isNonPrimitiveType).isFalse()
    }

    private fun assertType(prop: Property, type: PropertyType) {
        assertThat(prop.propertyType).isEqualTo(type)
        assertThat(prop.isNotNull).isFalse()
        assertThat(prop.isNonPrimitiveType).isTrue()
    }

}
