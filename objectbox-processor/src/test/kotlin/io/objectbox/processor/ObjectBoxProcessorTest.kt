package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType
import org.junit.Assert.fail
import org.junit.Test

class ObjectBoxProcessorTest {

    @Test
    fun testProcessor() {
        val entitySimple = JavaFileObjects.forResource("SimpleEntity.java")
        val entityRelated = JavaFileObjects.forResource("RelatedEntity.java")

        val processor = ObjectBoxProcessorShim()

        val compilation = javac()
                .withProcessors(processor)
                .withOptions(
                        "-A${ObjectBoxProcessor.OPTION_MODEL_PATH}=src/test/resources/objectbox-models/default.json"
                        // disabled as compat DAO currently requires entity property getters/setters
//                        "-A${ObjectBoxProcessor.OPTION_DAO_COMPAT}=true"
                )
                .compile(entitySimple, entityRelated)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        CompilationSubject.assertThat(compilation)
                .hadNoteContaining("Processing @Entity annotation.")
                .inFile(entitySimple)

        // assert generated files source trees
        assertGeneratedSourceMatches(compilation, "io.objectbox.processor.test.MyObjectBox", "MyObjectBox.java")
        assertGeneratedSourceMatches(compilation, "io.objectbox.processor.test.SimpleEntity_", "SimpleEntity_.java")
        assertGeneratedSourceMatches(compilation, "io.objectbox.processor.test.SimpleEntityCursor",
                "SimpleEntityCursor.java")

        // assert schema
        val schema = processor.schema
        assertThat(schema).isNotNull()
        assertThat(schema!!.version).isEqualTo(1)
        assertThat(schema.defaultJavaPackage).isEqualTo("io.objectbox.processor.test")
        assertThat(schema.lastEntityId).isEqualTo(IdUid(1, 4858050548069557694))
        assertThat(schema.lastIndexId).isEqualTo(IdUid(0, 0))
        assertThat(schema.entities).hasSize(1)

        // assert entity
        val entity = schema.entities.get(0)
        assertThat(entity.className).isEqualTo("SimpleEntity")
        assertThat(entity.dbName).isEqualTo("A")
        assertThat(entity.modelId).isEqualTo(1)
        assertThat(entity.modelUid).isEqualTo(4858050548069557694)
        assertThat(entity.lastPropertyId).isEqualTo(IdUid(22, 8133069888579241668))

        // assert index
        for (index in entity.indexes) {
            when (index.orderSpec) {
                "indexedProperty ASC" -> {
                    assertThat(!index.isUnique)
                    assertThat(!index.isNonDefaultName)
                }
                else -> fail("Found stray index '${index.orderSpec}' in schema.")
            }
        }

        // assert properties
        for (prop in entity.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey)
                    assertThat(prop.isIdAssignable)
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

    private fun assertGeneratedSourceMatches(compilation: Compilation?, qualifiedName: String, fileName: String) {
        val generatedFile = CompilationSubject.assertThat(compilation).generatedSourceFile(qualifiedName)
        generatedFile.isNotNull()
        generatedFile.hasSourceEquivalentTo(JavaFileObjects.forResource(fileName))
    }

    private fun assertPrimitiveType(prop: Property, type: PropertyType) {
        assertThat(prop.propertyType).isEqualTo(type)
        assertThat(prop.isNotNull)
        assertThat(!prop.isNonPrimitiveType)
    }

    private fun assertType(prop: Property, type: PropertyType) {
        assertThat(prop.propertyType).isEqualTo(type)
        assertThat(!prop.isNotNull)
        assertThat(prop.isNonPrimitiveType)
    }

}
