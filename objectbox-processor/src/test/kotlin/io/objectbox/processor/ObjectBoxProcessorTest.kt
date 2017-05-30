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
import io.objectbox.generator.model.ToMany
import org.junit.Assert.fail
import org.junit.Test

class ObjectBoxProcessorTest {

    val modelFilesBaseBath = "src/test/resources/objectbox-models/"
    val processorOptionBasePath = "-A${ObjectBoxProcessor.OPTION_MODEL_PATH}=$modelFilesBaseBath"

    @Test
    fun testProcessor() {
        val entitySimple = JavaFileObjects.forResource("SimpleEntity.java")

        val processor = ObjectBoxProcessorShim()

        val modelFile = "default.json"
        val compilation = javac()
                .withProcessors(processor)
                .withOptions(
                        "$processorOptionBasePath$modelFile"
                        // disabled as compat DAO currently requires entity property getters/setters
//                        "-A${ObjectBoxProcessor.OPTION_DAO_COMPAT}=true"
                )
                .compile(entitySimple)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        CompilationSubject.assertThat(compilation)
                .hadNoteContaining("Processing @Entity annotation.")
                .inFile(entitySimple)

        // assert generated files source trees
        assertGeneratedSourceMatches(compilation, "io.objectbox.processor.test.MyObjectBox",
                "expected-source/MyObjectBox.java")
        assertGeneratedSourceMatches(compilation, "io.objectbox.processor.test.SimpleEntity_",
                "expected-source/SimpleEntity_.java")
        assertGeneratedSourceMatches(compilation, "io.objectbox.processor.test.SimpleEntityCursor",
                "expected-source/SimpleEntityCursor.java")

        // assert schema
        val schema = processor.schema
        assertThat(schema).isNotNull()
        assertThat(schema!!.version).isEqualTo(1)
        assertThat(schema.defaultJavaPackage).isEqualTo("io.objectbox.processor.test")
        assertThat(schema.lastEntityId).isEqualTo(IdUid(1, 4858050548069557694))
        assertThat(schema.lastIndexId).isEqualTo(IdUid(0, 0))
        assertThat(schema.entities).hasSize(1)

        // assert entity
        val entity = schema.entities[0]
        assertThat(entity.className).isEqualTo("SimpleEntity")
        assertThat(entity.dbName).isEqualTo("A")
        assertThat(entity.modelId).isEqualTo(1)
        assertThat(entity.modelUid).isEqualTo(4858050548069557694)
        assertThat(entity.lastPropertyId).isEqualTo(IdUid(22, 8133069888579241668))

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
                    assertThat(prop.dbName).isEqualTo("_id")
                    assertPrimitiveType(prop, PropertyType.Long)
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
                else -> fail("Found stray field '${prop.propertyName}' in schema.")
            }
        }
    }

    @Test
    fun testRelation() {
        val entityParent = JavaFileObjects.forResource("RelationParentEntity.java")
        val entityRelated = JavaFileObjects.forResource("RelationChildEntity.java")

        val processor = ObjectBoxProcessorShim()

        val modelFile = "relation.json"
        val compilation = javac()
                .withProcessors(processor)
                .withOptions("$processorOptionBasePath$modelFile")
                .compile(entityParent, entityRelated)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // assert generated files source trees
        assertGeneratedSourceMatches(compilation, "io.objectbox.processor.test.RelationParentEntity_",
                "expected-source/RelationParentEntity_.java")
        assertGeneratedSourceMatches(compilation, "io.objectbox.processor.test.RelationParentEntityCursor",
                "expected-source/RelationParentEntityCursor.java")

        // assert schema
        val schema = processor.schema
        assertThat(schema).isNotNull()
        assertThat(schema!!.entities).hasSize(2)

        // assert entity
        val parent = schema.entities.single { it.className == "RelationParentEntity" }
        val child = schema.entities.single { it.className == "RelationChildEntity" }

        // assert properties
        for (prop in parent.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertThat(prop.isIdAssignable).isFalse()
                    assertThat(prop.dbName).isEqualTo("_id")
                    assertPrimitiveType(prop, PropertyType.Long)
                }
                "childId" -> {
                    assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    assertThat(prop.virtualTargetName).isNull()
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    assertToOneIndexAndRelation(parent, child, prop, toOneName = "child", toOneFieldName = "childToOne")
                }
                else -> fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
    }

    @Test
    fun testToOne() {
        val entityParent = JavaFileObjects.forResource("ToOneParentEntity.java")
        val entityRelated = JavaFileObjects.forResource("ToOneChildEntity.java")

        val processor = ObjectBoxProcessorShim()

        val modelFile = "to-one.json"
        val compilation = javac()
                .withProcessors(processor)
                .withOptions("$processorOptionBasePath$modelFile")
                .compile(entityParent, entityRelated)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // assert generated files source trees
        assertGeneratedSourceMatches(compilation, "io.objectbox.processor.test.ToOneParentEntity_",
                "expected-source/ToOneParentEntity_.java")
        assertGeneratedSourceMatches(compilation, "io.objectbox.processor.test.ToOneParentEntityCursor",
                "expected-source/ToOneParentEntityCursor.java")

        // assert schema
        val schema = processor.schema
        assertThat(schema).isNotNull()
        assertThat(schema!!.entities).hasSize(2)

        // assert entity
        val parent = schema.entities.single { it.className == "ToOneParentEntity" }
        val child = schema.entities.single { it.className == "ToOneChildEntity" }

        // assert properties
        for (prop in parent.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertThat(prop.isIdAssignable).isFalse()
                    assertThat(prop.dbName).isEqualTo("_id")
                    assertPrimitiveType(prop, PropertyType.Long)
                }
                "childId" -> {
                    assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    assertThat(prop.virtualTargetName).isEqualTo("child")
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    assertToOneIndexAndRelation(parent, child, prop, toOneName = "child")
                }
                else -> fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
    }

    @Test
    fun testBacklinkList() {
        val parentName = "BacklinkListParent"
        val childName = "BacklinkListChild"
        val entityParent = JavaFileObjects.forResource("$parentName.java")
        val entityChild = JavaFileObjects.forResource("$childName.java")

        val processor = ObjectBoxProcessorShim()

        val modelFile = "backlink-list.json"
        val compilation = javac()
                .withProcessors(processor)
                .withOptions("$processorOptionBasePath$modelFile")
                .compile(entityParent, entityChild)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // TODO ut: assert generated files source trees

        assertToManyEntities(processor, parentName, childName)
    }

    @Test
    fun testBacklinkToMany() {
        val parentName = "BacklinkToManyParent"
        val childName = "BacklinkToManyChild"
        val entityParent = JavaFileObjects.forResource("$parentName.java")
        val entityChild = JavaFileObjects.forResource("$childName.java")

        val processor = ObjectBoxProcessorShim()

        val modelFile = "backlink-to-many.json"
        val compilation = javac()
                .withProcessors(processor)
                .withOptions("$processorOptionBasePath$modelFile")
                .compile(entityParent, entityChild)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // TODO ut: assert generated files source trees

        assertToManyEntities(processor, parentName, childName)
    }

    private fun assertToManyEntities(processor: ObjectBoxProcessorShim, parentName: String, childName: String) {
        // assert schema
        val schema = processor.schema
        assertThat(schema).isNotNull()
        assertThat(schema!!.entities).hasSize(2)

        // assert parent properties
        val parent = schema.entities.single { it.className == parentName }
        for (prop in parent.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertThat(prop.isIdAssignable).isFalse()
                    assertThat(prop.dbName).isEqualTo("_id")
                    assertPrimitiveType(prop, PropertyType.Long)
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
                    assertThat(prop.dbName).isEqualTo("_id")
                    assertPrimitiveType(prop, PropertyType.Long)
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

    private fun assertToOneIndexAndRelation(parent: Entity, child: Entity, prop: Property, toOneName: String,
            toOneFieldName: String = toOneName) {
        // assert index
        assertThat(parent.indexes).hasSize(1)
        val foreignKeyIndex = parent.indexes[0]
        assertThat(foreignKeyIndex.properties[0]).isEqualTo(prop)

        // assert to one relation
        assertThat(parent.toOneRelations).hasSize(1)
        for (toOneRelation in parent.toOneRelations) {
            when (toOneRelation.name) {
                toOneName -> {
                    assertThat(toOneRelation.targetEntity).isEqualTo(child)
                    assertThat(toOneRelation.targetIdProperty).isEqualTo(prop)
                    assertThat(toOneRelation.nameToOne).isEqualTo(toOneFieldName)
                }
                else -> fail("Found stray toOneRelation '${toOneRelation.name}' in schema.")
            }
        }
    }

    private fun assertGeneratedSourceMatches(compilation: Compilation?, qualifiedName: String, fileName: String) {
        val generatedFile = CompilationSubject.assertThat(compilation).generatedSourceFile(qualifiedName)
        generatedFile.isNotNull()
        generatedFile.hasSourceEquivalentTo(JavaFileObjects.forResource(fileName))
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