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
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjectSubject
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.IdUid
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
import io.objectbox.generator.model.ToMany
import io.objectbox.generator.model.ToManyStandalone
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import javax.tools.JavaFileObject

class ObjectBoxProcessorTest {

    class TestEnvironment(modelFile: String,
                          val optionDisableTransform: Boolean = false,
                          copyModelFile: Boolean = false) {

        // tests run from IntelliJ are relative to module directory
        private val modelFilesPathModule = "src/test/resources/objectbox-models/"

        // tests run from Gradle are relative to project directory
        private val modelFilesPathProject = "objectbox-processor/$modelFilesPathModule"

        private val modelFilePath: String
        private val modelFileProcessorOption: List<String>
            get() {
                val options = mutableListOf("-A${ObjectBoxProcessor.OPTION_MODEL_PATH}=$modelFilePath")
                options += "-A${ObjectBoxProcessor.OPTION_DEBUG}=true"
                if (optionDisableTransform) options += "-A${ObjectBoxProcessor.OPTION_TRANSFORMATION_ENABLED}=false"
                return options
            }

        val processor = ObjectBoxProcessorShim()
        val schema: Schema
            get() = processor.schema!!

        init {
            val path = when {
                File(modelFilesPathModule).isDirectory -> modelFilesPathModule
                File(modelFilesPathProject).isDirectory -> modelFilesPathProject
                else -> throw FileNotFoundException("Can not find model file directory.")
            } + modelFile

            modelFilePath = if (copyModelFile) {
                val pathCopy = "$path.copy"
                File(path).copyTo(File(pathCopy), overwrite = true)
                pathCopy
            } else path
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
    fun testUidEmpty() {
        val environment = TestEnvironment("uid.json", copyModelFile = true)
        val compilation = environment.compile("UidEmptyEntity")
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Uid(2361091532752425885L)")
    }

    @Test
    fun testUidNew() {
        val environment = TestEnvironment("uid-new-uid-pool.json", copyModelFile = true)
        val modelBefore = environment.readModel()
        assertEquals(1, modelBefore.newUidPool.size)

        val compilation = environment.compile("UidNewEntity")
        CompilationSubject.assertThat(compilation).succeeded()
        val model = environment.readModel()

        val newUid = modelBefore.newUidPool.single()
        val entity = model.findEntity("UidEntity", null)!!
        assertEquals(newUid, entity.uid)
        assertEquals(modelBefore.lastEntityId.id + 1, model.lastEntityId.id)
        assertEquals(newUid, model.lastEntityId.uid)
    }

    @Test
    fun testPropertyUidEmpty() {
        val environment = TestEnvironment("uid.json", copyModelFile = true)
        val compilation = environment.compile("UidPropertyEmptyEntity")
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Uid(7287685531948841886L)")
    }

    @Test
    fun testPropertyUidNew() {
        val environment = TestEnvironment("uid-new-uid-pool.json", copyModelFile = true)
        val entityName = "UidPropertyNewEntity"
        val modelBefore = environment.readModel()
        assertEquals(1, modelBefore.newUidPool.size)
        val entityBefore = modelBefore.findEntity("UidEntity", null)!!

        val compilation = environment.compile(entityName)
        CompilationSubject.assertThat(compilation).succeeded()
        val model = environment.readModel()
        val entity = model.findEntity("UidEntity", null)!!

        val property = entity.properties.single { it.name == "uidProperty" }
        val newUid = modelBefore.newUidPool.single()
        assertEquals(newUid, property.uid)
        assertEquals(entityBefore.lastPropertyId.id + 1, entity.lastPropertyId.id)
        assertEquals(newUid, entity.lastPropertyId.uid)

        assertEquals(0, model.newUidPool.size)
    }

    @Test
    fun testToOneUidEmpty() {
        val environment = TestEnvironment("uid-relation.json", copyModelFile = true)
        val compilation = environment.compile("UidToOneEmptyEntity")
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Uid(4055646088440538446L)")
    }

    @Test
    fun testToOneUidNew() {
        val environment = TestEnvironment("uid-relation-new-uid-pool.json", copyModelFile = true)
        val entityName = "UidToOneNewEntity"
        val modelBefore = environment.readModel()
        assertEquals(1, modelBefore.newUidPool.size)
        val entityBefore = modelBefore.findEntity("UidRelationNewEntity", null)!!

        val compilation = environment.compile(entityName)
        CompilationSubject.assertThat(compilation).succeeded()
        val model = environment.readModel()
        val entity = model.findEntity("UidRelationNewEntity", null)!!

        val property = entity.properties.single { it.name == "toOneId" }
        val newUid = modelBefore.newUidPool.single()
        assertEquals(newUid, property.uid)
        assertEquals(entityBefore.lastPropertyId.id + 1, entity.lastPropertyId.id)
        assertEquals(newUid, entity.lastPropertyId.uid)

        assertEquals(0, model.newUidPool.size)
    }

    @Test
    fun testToManyUidNew() {
        val environment = TestEnvironment("uid-relation-new-uid-pool.json", copyModelFile = true)
        val entityName = "UidToManyNewEntity"
        val modelBefore = environment.readModel()
        assertEquals(1, modelBefore.newUidPool.size)

        val compilation = environment.compile(entityName)
        CompilationSubject.assertThat(compilation).succeeded()
        val model = environment.readModel()
        val entity = model.findEntity("UidRelationNewEntity", null)!!

        val relation = entity.relations.single { it.name == "toManyStandalone" }
        val newUid = modelBefore.newUidPool.single()
        assertEquals(newUid, relation.uid)
        assertEquals(modelBefore.lastRelationId.id + 1, model.lastRelationId.id)
        assertEquals(newUid, model.lastRelationId.uid)

        assertEquals(0, model.newUidPool.size)
    }

    @Test
    fun testToManyUidEmpty() {
        val environment = TestEnvironment("uid-relation.json", copyModelFile = true)
        val compilation = environment.compile("UidToManyEmptyEntity")
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Uid(823077930327936262L)")
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
    fun testInheritance() {
        // tests if properties from @BaseEntity class are used in ObjectBox, other super class and interface are ignored
        val nameBase = "InheritanceBase"
        val nameNoBase = "InheritanceNoBase"
        val nameSub = "InheritanceSub"
        val nameSubSub = "InheritanceSubSub"
        val nameInterface = "InheritanceInterface"

        val environment = TestEnvironment("inheritance.json")

        val compilation = environment.compile(nameBase, nameNoBase, nameSub, nameSubSub, nameInterface)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

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
                "noBaseString" -> fail("Found non-@BaseEntity field '${prop.propertyName}' in schema.")
                else -> fail("Found stray field '${prop.propertyName}' in schema.")
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
                "noBaseString" -> fail("Found non-@BaseEntity field '${prop.propertyName}' in schema.")
                else -> fail("Found stray field '${prop.propertyName}' in schema.")
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
                .forEach { fail("Found stray property '${it.name}' in model file.") }

        val modelEntity2 = model.findEntity(nameSubSub, null)
        assertThat(modelEntity2).isNotNull()
        val modelProperties2 = modelEntity2!!.properties
        assertThat(modelProperties2.size).isEqualTo(5)
        val modelPropertyNames2 = modelPropertyNames.plus("subSubString")
        modelProperties2
                .filterNot { modelPropertyNames2.contains(it.name) }
                .forEach { fail("Found stray property '${it.name}' in model file.") }
    }

    @Test
    fun testInheritanceBetweenEntities() {
        // tests if both entities are used, properties from super @Entity class are inherited, the interface is ignored
        val nameSuper = "InheritanceEntity"
        val nameSub = "InheritanceSubEntity"
        val nameInterface = "InheritanceInterface"

        val environment = TestEnvironment("inheritance-entities.json")

        val compilation = environment.compile(nameSuper, nameSub, nameInterface)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

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
                else -> fail("Found stray field '${prop.propertyName}' in schema.")
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
                else -> fail("Found stray field '${prop.propertyName}' in schema.")
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
                .forEach { fail("Found stray property '${it.name}' in model file.") }

        val modelEntity2 = model.findEntity(nameSub, null)
        assertThat(modelEntity2).isNotNull()
        val modelProperties2 = modelEntity2!!.properties
        assertThat(modelProperties2.size).isEqualTo(3)
        val modelPropertyNames2 = modelPropertyNames.plus("subString")
        modelProperties2
                .filterNot { modelPropertyNames2.contains(it.name) }
                .forEach { fail("Found stray property '${it.name}' in model file.") }
    }

    @Test
    fun testInheritanceOverriddenField() {
        // tests that adding a duplicate property results in an error (and no crash)
        val nameBase = "InheritanceBase"
        val nameSub = "InheritanceSubOverride"

        val environment = TestEnvironment("inheritance-overridden-temp.json")

        val compilation = environment.compile(nameBase, nameSub)
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining("Duplicate name \"overriddenString\"")
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
                    assertThat(child.indexes).hasSize(1)
                    assertThat(child.toOneRelations).hasSize(1)
                    assertToOneIndexAndRelation(child, parent, prop, toOneName = "parent", toOneFieldName = "parent")
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
        assertThat(child.properties).hasSize(3)

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
                "aParentId" -> {
                    assertThat(prop.dbName).isEqualTo(prop.propertyName)
//                    assertThat(prop.virtualTargetName).isEqualTo("parentWithIdProperty")
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    assertToOneIndexAndRelation(child, parent, prop, toOneName = "parentWithIdProperty")
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
                "parentId", "aParentId" -> {
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
    fun testToManyStandaloneUidName() {
        val parentName = "ToManyStandaloneUidName"
        val childName = "IdEntity"

        val environment = TestEnvironment("standalone-to-many-uid-name.json")

        val compilation = environment.compile(parentName, childName)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val myObjectBoxContent = getGeneratedJavaFile(compilation, "MyObjectBox").contentsAsUtf8String()
        myObjectBoxContent.contains("420000000L")
        myObjectBoxContent.contains("\"Hoolaloop\"")
        val entity = environment.schema.entities.filter { it.className == "ToManyStandaloneUidName" }.single()
        assertEquals(1, entity.toManyRelations.size)
        val toMany = entity.toManyRelations[0] as ToManyStandalone
        assertEquals("Hoolaloop", toMany.dbName)
        assertEquals(420000000L, toMany.modelId.uid)
    }

    @Test
    fun testToManyAndConverter() {
        val parentName = "ToManyAndConverter"
        val childName = "IdEntity"

        val environment = TestEnvironment("to-many-and-converter.json")

        val compilation = environment.compile(parentName, childName, "TestConverter")
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
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
                .hadErrorContaining("Could not find target property 'wrongParent' in '$childName'")
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
                    assertThat(child.indexes).hasSize(1)
                    assertThat(child.toOneRelations).hasSize(1)
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
                "childrenOther" -> {
                    assertThat(toManyRelation.sourceEntity).isEqualTo(parent)
                    assertThat(toManyRelation.targetEntity).isEqualTo(child)
                }
                else -> fail("Found stray toManyRelation '${toManyRelation.name}' in schema.")
            }
        }
    }

    private fun assertToOneIndexAndRelation(child: Entity, parent: Entity, prop: Property, toOneName: String,
                                            toOneFieldName: String = toOneName) {
        // assert index
        val indexesForProperty = child.indexes.filter { it.properties[0] == prop }
        assertThat(indexesForProperty).hasSize(1)

        // assert to one relation
        val toOneRelation = child.toOneRelations.single { it.name == toOneName }
        assertThat(toOneRelation.targetEntity).isEqualTo(parent)
        assertThat(toOneRelation.targetIdProperty).isEqualTo(prop)
        assertThat(toOneRelation.nameToOne).isEqualTo(toOneFieldName)
    }

    private fun assertGeneratedSourceMatches(compilation: Compilation, simpleName: String) {
        val generatedFile = getGeneratedJavaFile(compilation, simpleName)
        generatedFile.hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/$simpleName.java"))
    }

    /** non-null*/
    private fun getGeneratedJavaFile(compilation: Compilation, simpleName: String): JavaFileObjectSubject {
        val generatedFile = CompilationSubject.assertThat(compilation)
                .generatedSourceFile("io.objectbox.processor.test.$simpleName")
        generatedFile.isNotNull()
        return generatedFile
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
