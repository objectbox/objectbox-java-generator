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
import java.io.FileNotFoundException
import java.nio.file.Files
import javax.tools.JavaFileObject

class ObjectBoxProcessorTest {

    class TestEnvironment(modelFile: String, val optionDisableTransform: Boolean = false) {
        val modelFilesPathModule = "src/test/resources/objectbox-models/"
        val modelFilesPathProject = "objectbox-processor/$modelFilesPathModule"

        val modelFilePath: String
        val modelFileProcessorOption: List<String>
            get() {
                val options = mutableListOf("-A${ObjectBoxProcessor.OPTION_MODEL_PATH}=$modelFilePath")
                options += "-A${ObjectBoxProcessor.OPTION_DEBUG}=true"
                if(optionDisableTransform) options+= "-A${ObjectBoxProcessor.OPTION_TRANSFORMATION_ENABLED}=false"
                return options
            }

        val processor = ObjectBoxProcessorShim()
        val schema: Schema
            get() = processor.schema!!

        init {
            // tests run from Gradle are relative to project directory
            val modelFilePathProject = "$modelFilesPathProject$modelFile"
            // tests run from IntelliJ are relative to module directory
            val modelFilePathModule = "$modelFilesPathModule$modelFile"
            if (File(modelFilePathProject).parentFile.isDirectory) {
                modelFilePath = modelFilePathProject
            } else if (File(modelFilePathModule).parentFile.isDirectory) {
                modelFilePath = modelFilePathModule
            } else {
                throw FileNotFoundException("Can not find model file directory.")
            }
        }

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

        fun cleanModelFile() {
            Files.deleteIfExists(File(modelFilePath).toPath())
        }

        fun readModel(): IdSync {
            return IdSync(File(modelFilePath))
        }
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
    fun testUid() {
        // test that @Uid values are picked up
        val className = "UidEntity"

        val environment = TestEnvironment("uid.json")

        val compilation = environment.compile(className)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val entity = environment.schema.entities[0]
        assertThat(entity.modelUid).isEqualTo(2361091532752425885)

        val property = entity.properties.single { it.propertyName == "uidProperty" }
        assertThat(property.modelId.uid).isEqualTo(7287685531948841886)
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
    fun testRelation() {
        // tested relation: a child has a parent
        val parentName = "RelationParent"
        val childName = "RelationChild"

        // assert generated files source trees
        testToOneSources(parentName, childName, "relation.json")

        // assert schema and model
        val environment = TestEnvironment("relation-temp.json")
        environment.cleanModelFile()

        val compilation = environment.compile(parentName, childName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val schema = environment.schema
        assertThat(schema).isNotNull()
        assertThat(schema.entities).hasSize(2)

        val parent = schema.entities.single { it.className == parentName }
        val child = schema.entities.single { it.className == childName }

        // assert to-one and index on target property in schema
        assertThat(child.properties.size).isAtLeast(1)
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

        assertToOneModel(environment, childName)
    }

    @Test
    fun testToOne() {
        // tested relation: a child has a parent
        val parentName = "ToOneParent"
        val childName = "ToOneChild"

        // assert generated files source trees
        testToOneSources(parentName, childName, "to-one.json")

        // assert schema and model
        val environment = TestEnvironment("to-one-temp.json")
        environment.cleanModelFile()

        val compilation = environment.compile(parentName, childName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

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

        assertToOneModel(environment, childName)
    }

    private fun testToOneSources(parentName: String, childName: String, modelFileName: String) {
        val fixedEnvironment = TestEnvironment(modelFileName)

        val fixedCompilation = fixedEnvironment.compile(parentName, childName)
        CompilationSubject.assertThat(fixedCompilation).succeededWithoutWarnings()

        assertGeneratedSourceMatches(fixedCompilation, "${childName}_")
        assertGeneratedSourceMatches(fixedCompilation, "${childName}Cursor")
    }

    private fun assertToOneModel(environment: TestEnvironment, childName: String) {
        val model = environment.readModel()
        val modelChild = model.findEntity(childName, null)

        // assert only target property has an index in model
        assertThat(modelChild!!.properties.size).isAtLeast(1)
        for (property in modelChild.properties) {
            when (property.name) {
                "parentId" -> {
                    assertThat(property.indexId).isNotNull()
                    assertThat(property.indexId).isNotEqualTo(IdUid())
                }
                else -> {
                    assertThat(property.indexId).isNull()
                }
            }
        }
    }

    @Test
    fun testToOneNoBoxStoreField() {
        // tested relation: a child has a parent
        val parentName = "ToOneParent"
        val childName = "ToOneNoBoxStore"

        val environment = TestEnvironment("not-generated.json", optionDisableTransform = true)
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
    fun testToManyStandalone() {
        val parentName = "ToManyStandalone"
        val childName = "IdEntity"

        val environment = TestEnvironment("standalone-to-many.json")

        val compilation = environment.compile(parentName, childName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        assertGeneratedSourceMatches(compilation, "${parentName}_")
        assertGeneratedSourceMatches(compilation, "${parentName}Cursor")

        assertToManySchema(environment.schema, parentName, childName)
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

        val environment = TestEnvironment("backlink-with-to-temp.json")
        environment.cleanModelFile()

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
