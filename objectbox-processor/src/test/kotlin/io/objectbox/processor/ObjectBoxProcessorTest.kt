package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.IdUid
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
import io.objectbox.generator.model.ToMany
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import javax.tools.JavaFileObject

class ObjectBoxProcessorTest {

    class TestEnvironment(modelFile: String) {
        val modelFilesBaseBath = "src/test/resources/objectbox-models/"
        val modelFilePath = "$modelFilesBaseBath$modelFile"
        val modelFileProcessorOption = "-A${ObjectBoxProcessor.OPTION_MODEL_PATH}=$modelFilePath"

        val processor = ObjectBoxProcessorShim()
        val schema: Schema
            get() = processor.schema!!

        fun compile(vararg files: String): Compilation {
            val fileObjects = files.map { JavaFileObjects.forResource("$it.java") }
            return compile(fileObjects)
        }

        fun compile(files: List<JavaFileObject>): Compilation {
            return javac()
                    .withProcessors(processor)
                    .withOptions(modelFileProcessorOption)
                    .compile(files)
        }

        fun compileDaoCompat(vararg files: String): Compilation {
            val fileObjects = files.map { JavaFileObjects.forResource("$it.java") }
            return javac()
                    .withProcessors(processor)
                    .withOptions(
                            modelFileProcessorOption
                            // disabled as compat DAO currently requires entity property getters/setters
//                        "-A${ObjectBoxProcessor.OPTION_DAO_COMPAT}=true"
                    )
                    .compile(fileObjects)
        }
    }

    @Test
    fun testProcessor() {
        val className = "SimpleEntity"
        val modelFileName = "default.json"

        val environment = TestEnvironment(modelFileName)

        val compilation = environment.compileDaoCompat(className)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // assert generated files source trees
        assertGeneratedSourceMatches(compilation, "MyObjectBox")
        assertGeneratedSourceMatches(compilation, "${className}_")
        assertGeneratedSourceMatches(compilation, "${className}Cursor")

        // assert schema
        val schema = environment.schema
        assertThat(schema).isNotNull()
        assertThat(schema.version).isEqualTo(1)
        assertThat(schema.defaultJavaPackage).isEqualTo("io.objectbox.processor.test")
        val lastEntityId = IdUid(1, 4858050548069557694)
        assertThat(schema.lastEntityId).isEqualTo(lastEntityId)
        val lastIndexId = IdUid(1, 4551328960004588074)
        assertThat(schema.lastIndexId).isEqualTo(lastIndexId)
        assertThat(schema.entities).hasSize(1)

        // assert entity
        val entity = schema.entities[0]
        assertThat(entity.className).isEqualTo(className)
        val dbName = "A"
        assertThat(entity.dbName).isEqualTo(dbName)
        assertThat(entity.modelId).isEqualTo(1)
        assertThat(entity.modelUid).isEqualTo(4858050548069557694)
        assertThat(entity.lastPropertyId).isEqualTo(IdUid(23, 4772590935549770830))
        assertThat(entity.isConstructors).isFalse()

        // assert index
        assertThat(entity.indexes.size).isAtLeast(1)
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
                "indexedProperty" -> {
                    assertType(prop, PropertyType.Int)
                    assertThat(prop.index).isEqualTo(entity.indexes[0])
                }
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

        // assert model file
        val modelFile = File(environment.modelFilePath)
        val absolutePath = modelFile.absolutePath
        System.out.println("Model file path: $absolutePath")
        val idSync = IdSync(modelFile)
        assertThat(idSync.lastEntityId).isEqualTo(lastEntityId)
        assertThat(idSync.lastIndexId).isEqualTo(lastIndexId)
        val modelEntity = idSync.findEntity(dbName, null)
        assertThat(modelEntity).isNotNull()
        assertThat(modelEntity!!.properties.size).isAtLeast(1)

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
                "simpleString",
                "simpleByteArray",
                "indexedProperty",
                "B",
                "uidProperty",
                "customType",
                "customTypes"
        )
        modelPropertyNames.forEach { name ->
            val property = modelEntity.properties.singleOrNull { it.name == name }
            assertWithMessage("Property '$name' not in model file").that(property).isNotNull()
            assertWithMessage("Property '$name' has no id").that(property!!.id).isNotNull()
            assertWithMessage("Property '$name' id:uid is 0:0").that(property.id).isNotEqualTo(IdUid())

            when (name) {
                "indexedProperty" -> {
                    assertThat(property.indexId).isNotNull()
                    assertThat(property.indexId).isNotEqualTo(IdUid())
                }
                "uidProperty" -> assertThat(property.id.uid).isEqualTo(3817914863709111804)
            }
        }

        // assert no other model properties exist
        modelEntity.properties
                .filterNot { modelPropertyNames.contains(it.name) }
                .forEach { fail("Found stray property '${it.name}' in model file.") }
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
    fun testRelation() {
        // tested relation: a child has a parent
        val parentName = "RelationParent"
        val childName = "RelationChild"

        val environment = TestEnvironment("relation.json")

        val compilation = environment.compile(parentName, childName)
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

        val environment = TestEnvironment("to-one.json")

        val compilation = environment.compile(parentName, childName)
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

        val environment = TestEnvironment("to-one-all-args.json")

        val compilation = environment.compile(parentName, childName)
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

        val environment = TestEnvironment("not-generated.json")

        val compilation = environment.compile(parentName, childName)
        CompilationSubject.assertThat(compilation).failed()

        CompilationSubject.assertThat(compilation).hadErrorCount(1)
        CompilationSubject.assertThat(compilation).hadErrorContainingMatch("in '$childName' add a field '__boxStore'")
    }

    @Test
    fun testBacklinkList() {
        // tested relation: a parent has children
        val parentName = "BacklinkListParent"
        val childName = "BacklinkListChild"

        val environment = TestEnvironment("backlink-list.json")

        val compilation = environment.compile(parentName, childName)
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

        val environment = TestEnvironment("backlink-to-many.json")

        val compilation = environment.compile(parentName, childName)
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

        val environment = TestEnvironment("not-generated.json")

        val compilation = environment.compile(parentName, childName)
        CompilationSubject.assertThat(compilation).failed()

        CompilationSubject.assertThat(compilation).hadErrorContaining("ToMany field must be annotated with @Backlink")
    }

    @Test
    fun testBacklinkMultiple() {
        // test if multiple to-one fields for one @Backlink (without 'to' value) are detected
        val parentName = "BacklinkMultipleParent"
        val childName = "BacklinkMultipleChild"

        val environment = TestEnvironment("not-generated.json")

        val compilation = environment.compile(parentName, childName)
        CompilationSubject.assertThat(compilation).failed()

        CompilationSubject.assertThat(compilation).hadErrorContaining("Set name of one to-one relation of '$childName'")
    }

    @Test
    fun testBacklinkWithTo() {
        // test if correct to-one of @Backlink (with 'to' value) is detected
        val parentName = "BacklinkWithToParent"
        val childName = "BacklinkWithToChild"

        val environment = TestEnvironment("backlink-with-to.json")

        val compilation = environment.compile(parentName, childName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val schema = environment.schema

        val parent = schema.entities.single { it.className == parentName }
        val child = schema.entities.single { it.className == childName }

        for (prop in child.properties) {
            when (prop.propertyName) {
                "parentId" -> {
                    assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    assertThat(prop.virtualTargetName).isEqualTo("parent")
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    assertToManyRelation(parent, child, prop)
                }
                "id", "parentOtherId" -> {
                    // just ensure its exists
                }
                else -> fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
    }

    @Test
    fun testBacklinkWrongTo() {
        // test if correct to-one of @Backlink (with 'to' value) is detected
        val parentName = "BacklinkWrongToParent"
        val childName = "BacklinkWrongToChild"

        val environment = TestEnvironment("not-generated.json")

        val compilation = environment.compile(parentName, childName)
        CompilationSubject.assertThat(compilation).failed()

        CompilationSubject.assertThat(compilation)
                .hadErrorContaining("Could not find target property 'wrongParentId' in '$childName'")
    }

    @Test
    fun testKotlinByteCode() {
        val entityName = "SimpleKotlinEntity"

        val environment = TestEnvironment("kotlin.json")

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
