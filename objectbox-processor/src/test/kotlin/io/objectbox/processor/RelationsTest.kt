package io.objectbox.processor

import com.google.common.truth.Truth
import com.google.testing.compile.CompilationSubject
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
import io.objectbox.generator.model.ToMany
import io.objectbox.generator.model.ToManyStandalone
import org.junit.Assert
import org.junit.Test


/**
 * Tests relations using standalone ToOne, standalone ToMany and ToMany with @Backlink ToOne.
 */
class RelationsTest : BaseProcessorTest() {

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
        Truth.assertThat(schema).isNotNull()
        Truth.assertThat(schema.entities).hasSize(2)

        val parent = schema.entities.single { it.className == parentName }
        val child = schema.entities.single { it.className == childName }

        // assert to-one and index on target property in schema
        Truth.assertThat(child.properties.size).isAtLeast(1)
        for (prop in child.properties) {
            when (prop.propertyName) {
                "id" -> {
                    Truth.assertThat(prop.isPrimaryKey).isTrue()
                    Truth.assertThat(prop.isIdAssignable).isFalse()
                    Truth.assertThat(prop.dbName).isEqualTo("id")
                    assertType(prop, PropertyType.Long)
                }
                "parentId" -> {
                    Truth.assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    Truth.assertThat(prop.virtualTargetName).isNull()
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    Truth.assertThat(child.indexes).hasSize(1)
                    Truth.assertThat(child.toOneRelations).hasSize(1)
                    assertToOneIndexAndRelation(child, parent, prop, toOneName = "parent", toOneFieldName = "parent")
                }
                else -> Assert.fail("Found stray property '${prop.propertyName}' in schema.")
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
        Truth.assertThat(schema).isNotNull()
        Truth.assertThat(schema.entities).hasSize(2)

        // assert entity
        val parent = schema.entities.single { it.className == parentName }
        val child = schema.entities.single { it.className == childName }
        Truth.assertThat(child.properties).hasSize(3)

        // assert properties
        for (prop in child.properties) {
            when (prop.propertyName) {
                "id" -> {
                    Truth.assertThat(prop.isPrimaryKey).isTrue()
                    Truth.assertThat(prop.isIdAssignable).isFalse()
                    Truth.assertThat(prop.dbName).isEqualTo("id")
                    assertType(prop, PropertyType.Long)
                }
                "parentId" -> {
                    Truth.assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    Truth.assertThat(prop.virtualTargetName).isEqualTo("parent")
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    assertToOneIndexAndRelation(child, parent, prop, toOneName = "parent")
                }
                "aParentId" -> {
                    Truth.assertThat(prop.dbName).isEqualTo(prop.propertyName)
//                    assertThat(prop.virtualTargetName).isEqualTo("parentWithIdProperty")
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    assertToOneIndexAndRelation(child, parent, prop, toOneName = "parentWithIdProperty")
                }
                else -> Assert.fail("Found stray property '${prop.propertyName}' in schema.")
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
        Truth.assertThat(modelChild!!.properties.size).isAtLeast(1)
        for (property in modelChild.properties) {
            when (property.name) {
                "parentId", "aParentId" -> {
                    Truth.assertThat(property.indexId).isNotNull()
                    Truth.assertThat(property.indexId).isNotEqualTo(IdUid())
                }
                else -> {
                    Truth.assertThat(property.indexId).isNull()
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
        Assert.assertEquals(1, entity.toManyRelations.size)
        val toMany = entity.toManyRelations[0] as ToManyStandalone
        Assert.assertEquals("Hoolaloop", toMany.dbName)
        Assert.assertEquals(420000000L, toMany.modelId.uid)
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
                    Truth.assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    Truth.assertThat(prop.virtualTargetName).isEqualTo("parent")
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    assertToManyRelation(parent, child, prop)
                }
                "id", "parentOtherId" -> {
                    // just ensure its exists
                }
                else -> Assert.fail("Found stray property '${prop.propertyName}' in schema.")
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

    private fun assertToManySchema(schema: Schema, parentName: String, childName: String) {
        // assert schema
        Truth.assertThat(schema).isNotNull()
        Truth.assertThat(schema.entities).hasSize(2)

        // assert parent properties
        val parent = schema.entities.single { it.className == parentName }
        for (prop in parent.properties) {
            when (prop.propertyName) {
                "id" -> {
                    Truth.assertThat(prop.isPrimaryKey).isTrue()
                    Truth.assertThat(prop.isIdAssignable).isFalse()
                    Truth.assertThat(prop.dbName).isEqualTo("id")
                    assertType(prop, PropertyType.Long)
                }
                else -> Assert.fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
        // assert child properties
        val child = schema.entities.single { it.className == childName }
        for (prop in child.properties) {
            when (prop.propertyName) {
                "id" -> {
                    Truth.assertThat(prop.isPrimaryKey).isTrue()
                    Truth.assertThat(prop.isIdAssignable).isFalse()
                    Truth.assertThat(prop.dbName).isEqualTo("id")
                    assertType(prop, PropertyType.Long)
                }
                "parentId" -> {
                    Truth.assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    Truth.assertThat(prop.virtualTargetName).isEqualTo("parent")
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    Truth.assertThat(child.indexes).hasSize(1)
                    Truth.assertThat(child.toOneRelations).hasSize(1)
                    assertToOneIndexAndRelation(child, parent, prop, toOneName = "parent")
                    assertToManyRelation(parent, child, prop)
                }
                else -> Assert.fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
    }

    private fun assertToManyRelation(parent: Entity, child: Entity, prop: Property) {
        for (toManyRelation in parent.toManyRelations) {
            when (toManyRelation.name) {
                "children" -> {
                    Truth.assertThat(toManyRelation.sourceEntity).isEqualTo(parent)
                    Truth.assertThat(toManyRelation.targetEntity).isEqualTo(child)
                    val toMany = toManyRelation as ToMany
                    Truth.assertThat(toMany.targetProperties).hasLength(1)
                    Truth.assertThat(toMany.targetProperties[0]).isEqualTo(prop)
                    // generator takes care of populating sourceProperties if we do not set them, so do not assert here
                }
                "childrenOther" -> {
                    Truth.assertThat(toManyRelation.sourceEntity).isEqualTo(parent)
                    Truth.assertThat(toManyRelation.targetEntity).isEqualTo(child)
                }
                else -> Assert.fail("Found stray toManyRelation '${toManyRelation.name}' in schema.")
            }
        }
    }

    private fun assertToOneIndexAndRelation(child: Entity, parent: Entity, prop: Property, toOneName: String,
                                            toOneFieldName: String = toOneName) {
        // assert index
        val indexesForProperty = child.indexes.filter { it.properties[0] == prop }
        Truth.assertThat(indexesForProperty).hasSize(1)

        // assert to one relation
        val toOneRelation = child.toOneRelations.single { it.name == toOneName }
        Truth.assertThat(toOneRelation.targetEntity).isEqualTo(parent)
        Truth.assertThat(toOneRelation.targetIdProperty).isEqualTo(prop)
        Truth.assertThat(toOneRelation.nameToOne).isEqualTo(toOneFieldName)
    }

}