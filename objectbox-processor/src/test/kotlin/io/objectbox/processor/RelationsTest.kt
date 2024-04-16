/*
 * ObjectBox Build Tools
 * Copyright (C) 2019-2024 ObjectBox Ltd.
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
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
import io.objectbox.generator.model.ToManyByBacklink
import io.objectbox.generator.model.ToManyStandalone
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test


/**
 * Tests relations using standalone ToOne, standalone ToMany and ToMany with @Backlink ToOne.
 */
class RelationsTest : BaseProcessorTest() {

    private val idEntityFileObject = JavaFileObjects.forResource("IdEntity.java")

    @Test
    fun testRelation() {
        // tested relation: a child has a parent
        val parentName = "RelationParent"
        val childName = "RelationChild"

        // assert generated files source trees
        testToOneSources(parentName, childName, "relation.json")

        // assert schema and model
        val environment = TestEnvironment("relation.json", useTemporaryModelFile = true)

        environment.compile(parentName, childName)
            .assertThatIt { succeededWithoutWarnings() }

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
                    assertType(prop, PropertyType.Long, hasNonPrimitiveFlag = true)
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
        val environment = TestEnvironment("to-one.json", useTemporaryModelFile = true)

        environment.compile(parentName, childName)
            .assertThatIt { succeededWithoutWarnings() }

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
                    assertType(prop, PropertyType.Long, hasNonPrimitiveFlag = true)
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

        fixedEnvironment.compile(parentName, childName)
            .assertThatIt { succeededWithoutWarnings() }
            .assertGeneratedSourceMatches("${childName}_")
            .assertGeneratedSourceMatches("${childName}Cursor")
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

    private val javaFileObjectToOneTarget = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        
        @Entity
        public class ToOneTarget {
            @Id long id;
        }
        """.trimIndent().let {
        JavaFileObjects.forSourceString("com.example.ToOneTarget", it)
    }

    @Test
    fun toOne_noTypeParameter_errors() {
        val javaFileObjectSource = """
        package com.example;
        import io.objectbox.BoxStore;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.relation.ToOne;
        
        @Entity
        public class NoType {
            @Id long id;
        
            ToOne toOne = new ToOne(this, NoType_.toOne);
        
            // need to add manually, as processor can not modify entity
            transient BoxStore __boxStore;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.NoType", it)
        }

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(listOf(javaFileObjectSource, javaFileObjectToOneTarget))
            .assertThatIt {
                failed()
                hadErrorContaining("The generic ToOne property 'toOne' in 'NoType' must have a type argument, e.g. ToOne<Entity>.")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun toMany_noTypeParameter_errors() {
        val javaFileObjectSource = """
        package com.example;
        import io.objectbox.BoxStore;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.relation.ToMany;
        
        @Entity
        public class NoType {
            @Id long id;
        
            ToMany toMany = new ToMany(this, NoType_.toMany);
        
            // need to add manually, as processor can not modify entity
            transient BoxStore __boxStore;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.NoType", it)
        }

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(listOf(javaFileObjectSource, javaFileObjectToOneTarget))
            .assertThatIt {
                failed()
                hadErrorContaining("The generic ToMany property 'toMany' in 'NoType' must have a type argument, e.g. ToMany<Entity>.")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun toOne_targetIdTypeNotLong_errors() {
        val javaFileObjectSource = """
        package com.example;
        import io.objectbox.BoxStore;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.relation.ToOne;
        
        @Entity
        public class ToOneSource {
            @Id long id;
        
            String targetId;
        
            ToOne<ToOneTarget> target = new ToOne<>(this, ToOneSource_.target);
        
            // need to add manually, as processor can not modify entity
            transient BoxStore __boxStore;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.ToOneSource", it)
        }

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(listOf(javaFileObjectSource, javaFileObjectToOneTarget))
            .assertThatIt {
                failed()
                hadErrorContaining("The target ID property 'targetId' for ToOne relation 'target' in 'ToOneSource' must be long.")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun toOne_targetIdAnnotationTypeNotLong_errors() {
        val javaFileObjectSource = """
        package com.example;
        import io.objectbox.BoxStore;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.TargetIdProperty;
        import io.objectbox.relation.ToOne;
        
        @Entity
        public class ToOneSource {
            @Id long id;
        
            String targetIdUnconventionalName;
        
            @TargetIdProperty("targetIdUnconventionalName")
            ToOne<ToOneTarget> target = new ToOne<>(this, ToOneSource_.target);
        
            // need to add manually, as processor can not modify entity
            transient BoxStore __boxStore;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.ToOneSource", it)
        }

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(listOf(javaFileObjectSource, javaFileObjectToOneTarget))
            .assertThatIt {
                failed()
                hadErrorContaining("The target ID property 'targetIdUnconventionalName' for ToOne relation 'target' in 'ToOneSource' must be long.")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun testToOneNoBoxStoreField() {
        // tested relation: a child has a parent
        val parentName = "ToOneParent"
        val childName = "ToOneNoBoxStore"

        val environment =
            TestEnvironment("not-generated.json", optionDisableTransform = true, useTemporaryModelFile = true)

        environment.compile(parentName, childName)
            .assertThatIt {
                failed()
                hadErrorCount(1)
                hadErrorContainingMatch("in '$childName' add a field '__boxStore'")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun backlink_toOne_list() {
        // tested relation: a source has ONE target, a target a BACKLINK to its sources
        val targetName = "BacklinkToOneListTarget"
        val sourceName = "BacklinkToOneListSource"

        val environment = TestEnvironment("backlink-list.json")

        environment.compile(targetName, sourceName)
            .assertThatIt { succeededWithoutWarnings() }
            .assertGeneratedSourceMatches("${targetName}_")
            .assertGeneratedSourceMatches("${targetName}Cursor")

        assertToManySchema(environment.schema, targetName, sourceName)
    }

    @Test
    fun backlink_toOne_toMany() {
        // tested relation: a source has ONE target, a target a BACKLINK to its sources
        val targetName = "BacklinkToOneTarget"
        val sourceName = "BacklinkToOneSource"

        val environment = TestEnvironment("backlink-to-many.json")

        environment.compile(targetName, sourceName)
            .assertThatIt { succeededWithoutWarnings() }
            .assertGeneratedSourceMatches("${targetName}_")
            .assertGeneratedSourceMatches("${targetName}Cursor")

        assertToManySchema(environment.schema, targetName, sourceName)
    }

    @Test
    fun backlink_toMany_toMany() {
        // tested relation: a source has MULTIPLE targets, a target a BACKLINK to its sources
        val targetName = "BacklinkToManyTarget"
        val sourceName = "BacklinkToManySource"

        val environment = TestEnvironment("backlink-to-many-to-many.json", useTemporaryModelFile = true)

        environment.compile(targetName, sourceName)
            .assertThatIt { succeededWithoutWarnings() }
            .assertGeneratedSourceMatches("${targetName}_")

        // assert schema
        val schema = environment.schema
        assertThat(schema).isNotNull()
        assertThat(schema.entities).hasSize(2)

        val target = schema.entities.single { it.className == targetName }
        val source = schema.entities.single { it.className == sourceName }

        // assert target to-many schema
        assertThat(target.toManyRelations).isNotEmpty()
        for (toManyRelation in target.toManyRelations) {
            when (toManyRelation.name) {
                "sources" -> {
                    assertThat(toManyRelation.sourceEntity).isEqualTo(target)
                    assertThat(toManyRelation.targetEntity).isEqualTo(source)
                    assertThat(toManyRelation is ToManyByBacklink)
                    val toManyByBacklink = toManyRelation as ToManyByBacklink
                    assertThat(toManyByBacklink.targetToOne).isNull()
                    assertThat(toManyByBacklink.targetToMany).isNotNull()
                    assertThat(toManyByBacklink.targetToMany!!.name).isEqualTo("targets")
                }

                else -> fail("Found stray to-many relation '${toManyRelation.name}' in schema.")
            }
        }

        // ensure target schema is as expected
        assertThat(target.toOneRelations).isEmpty()
        assertThat(target.properties).isNotEmpty()
        for (prop in target.properties) {
            when (prop.propertyName) {
                "id" -> assertType(prop, PropertyType.Long, hasNonPrimitiveFlag = true)
                else -> fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
        assertThat(target.incomingToManyRelations).isNotEmpty()
        for (toManyRelation in target.incomingToManyRelations) {
            when (toManyRelation.name) {
                "targets" -> assertThat(toManyRelation is ToManyStandalone)
                else -> fail("Found stray incoming to-many relation '${toManyRelation.name}' in schema.")
            }
        }
        // ensure source schema is as expected
        assertThat(source.toOneRelations).isEmpty()
        assertThat(source.properties).isNotEmpty()
        for (prop in source.properties) {
            when (prop.propertyName) {
                "id" -> assertType(prop, PropertyType.Long, hasNonPrimitiveFlag = true)
                else -> fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
        assertThat(source.toManyRelations).isNotEmpty()
        for (toManyRelation in source.toManyRelations) {
            when (toManyRelation.name) {
                "targets" -> assertThat(toManyRelation is ToManyStandalone)
                else -> fail("Found stray to-many relation '${toManyRelation.name}' in schema.")
            }
        }
        assertThat(source.incomingToManyRelations).isNotEmpty()
        for (toManyRelation in source.incomingToManyRelations) {
            when (toManyRelation.name) {
                "sources" -> {
                    assertThat(toManyRelation is ToManyByBacklink)
                    assertThat((toManyRelation as ToManyByBacklink).targetToOne).isNull()
                    assertThat(toManyRelation.targetToMany).isNotNull()
                }

                else -> fail("Found stray incoming to-many relation '${toManyRelation.name}' in schema.")
            }
        }
    }

    @Test
    fun backlink_multiple_toOne_toOne() {
        val targetName = "BacklinkMultipleOOTarget"
        val sourceName = "BacklinkMultipleOOSource"
        assertMultipleRelationsError(targetName, sourceName)
    }

    @Test
    fun backlink_multiple_toOne_toMany() {
        val targetName = "BacklinkMultipleOMTarget"
        val sourceName = "BacklinkMultipleOMSource"
        assertMultipleRelationsError(targetName, sourceName)
    }

    @Test
    fun backlink_multiple_toMany_toMany() {
        val targetName = "BacklinkMultipleMMTarget"
        val sourceName = "BacklinkMultipleMMSource"
        assertMultipleRelationsError(targetName, sourceName)
    }

    /**
     * Tests if multiple relation fields for one @Backlink (without 'to' value) are causing an error.
     */
    private fun assertMultipleRelationsError(targetName: String, sourceName: String) {
        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(targetName, sourceName)
            .assertThatIt {
                failed()
                hadErrorContaining("Set name of one to-one or to-many relation of '$sourceName'")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun backlink_multipleToToOneRelation_shouldError() {
        val backlinkEntity = "BacklinkMultipleErrorO"
        val relationEntity = "BacklinkMultipleErrorORelation"

        assertMultipleBacklinksError(backlinkEntity, relationEntity)
    }

    @Test
    fun backlink_multipleToToManyRelation_shouldError() {
        val backlinkEntity = "BacklinkMultipleErrorM"
        val relationEntity = "BacklinkMultipleErrorMRelation"

        assertMultipleBacklinksError(backlinkEntity, relationEntity)
    }

    private fun assertMultipleBacklinksError(backlinkEntity: String, relationEntity: String) {
        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(backlinkEntity, relationEntity)
            .assertThatIt {
                failed()
                hadErrorContaining("Only one @Backlink per relation allowed. Remove all but one @Backlink.")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun backlink_withTo() {
        // test if correct relation of @Backlink (with 'to' value) is detected
        val targetName = "BacklinkWithToTarget"
        val sourceName = "BacklinkWithToSource"

        val environment = TestEnvironment("backlink-with-to.json", useTemporaryModelFile = true)

        environment.compile(targetName, sourceName)
            .assertThatIt { succeededWithoutWarnings() }

        val schema = environment.schema

        val target = schema.entities.single { it.className == targetName }
        val source = schema.entities.single { it.className == sourceName }

        var toOneTargetProperty: Property? = null
        for (prop in source.properties) {
            when (prop.propertyName) {
                "targetId" -> {
                    assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    assertThat(prop.virtualTargetName).isEqualTo("target")
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    toOneTargetProperty = prop
                }

                "id", "targetOtherId" -> {
                    /* just ensure they exist */
                }

                else -> fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
        assertThat(toOneTargetProperty).isNotNull()

        for (toManyRelation in target.toManyRelations) {
            when (toManyRelation.name) {
                "sources" -> {
                    assertThat(toManyRelation.sourceEntity).isEqualTo(target)
                    assertThat(toManyRelation.targetEntity).isEqualTo(source)
                    assertThat(toManyRelation is ToManyByBacklink)
                    val toManyByBacklink = toManyRelation as ToManyByBacklink
                    assertThat(toManyByBacklink.targetToMany).isNull()
                    assertThat(toManyByBacklink.targetToOne).isNotNull()
                    assertThat(toManyByBacklink.targetToOne!!.name).isEqualTo("target")
                    assertThat(toManyByBacklink.targetToOne!!.idRefProperty).isEqualTo(toOneTargetProperty)
                    // generator takes care of populating sourceProperties if we do not set them, so do not assert here
                }

                "sourcesOther" -> {
                    assertThat(toManyRelation.sourceEntity).isEqualTo(target)
                    assertThat(toManyRelation.targetEntity).isEqualTo(source)
                    assertThat(toManyRelation is ToManyByBacklink)
                    assertThat((toManyRelation as ToManyByBacklink).targetToMany).isNull()
                    assertThat(toManyRelation.targetToOne).isNotNull()
                    assertThat(toManyRelation.targetToOne!!.name).isEqualTo("targetOther")
                }

                "sourcesMany" -> {
                    assertThat(toManyRelation.sourceEntity).isEqualTo(target)
                    assertThat(toManyRelation.targetEntity).isEqualTo(source)
                    assertThat(toManyRelation is ToManyByBacklink)
                    val toManyByBacklink = toManyRelation as ToManyByBacklink
                    assertThat(toManyByBacklink.targetToOne).isNull()
                    assertThat(toManyByBacklink.targetToMany).isNotNull()
                    assertThat(toManyByBacklink.targetToMany!!.name).isEqualTo("targets")
                }

                else -> fail("Found stray to-many relation '${toManyRelation.name}' in schema.")
            }
        }
    }

    @Test
    fun backlink_wrongTo() {
        // test if correct to-one of @Backlink (with 'to' value) is detected
        val targetName = "BacklinkWrongToTarget"
        val sourceName = "BacklinkWrongToSource"

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(targetName, sourceName)
            .assertThatIt {
                failed()
                hadErrorContaining("Could not find target property 'wrongTarget' in '$sourceName'")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun backlink_toOne_shouldError() {
        val sourceName = "BacklinkToOneError"

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(sourceName)
            .assertThatIt {
                failed()
                hadErrorContaining("'nonsensicalToOne' @Backlink can only be used on a ToMany relation")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun testToManyStandalone() {
        val parentName = "ToManyStandalone"
        val parentFileObject = """
        package io.objectbox.processor.test;
        
        import java.util.List;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.relation.ToMany;
        
        @Entity
        public class ToManyStandalone {
            @Id
            Long id;
        
            ToMany<IdEntity> children = new ToMany<>(this, ToManyStandalone_.children);
                                                        
            List<IdEntity> childrenList = new ToMany<>(this, ToManyStandalone_.childrenList);
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("io.objectbox.processor.test.${parentName}", it)
        }
        val childName = "IdEntity"

        val environment = TestEnvironment("standalone-to-many.json")

        environment.compile(listOf(parentFileObject, idEntityFileObject))
            .assertThatIt { succeededWithoutWarnings() }
            .assertGeneratedSourceMatches("${parentName}_")
            .assertGeneratedSourceMatches("${parentName}Cursor")

        assertToManySchema(environment.schema, parentName, childName)

        assertToManyStandaloneModel(environment, parentName, listOf("children", "childrenList"))
    }

    @Test
    fun testToManyStandaloneUidName() {
        val parentName = "ToManyStandaloneUidName"
        val childName = "IdEntity"

        val environment = TestEnvironment("standalone-to-many-uid-name.json")

        val compilation = environment.compile(parentName, childName)
            .assertThatIt { succeededWithoutWarnings() }

        val myObjectBoxContent = compilation
            .generatedSourceFileOrFail("io.objectbox.processor.test.MyObjectBox")
            .contentsAsUtf8String()
        myObjectBoxContent.contains("420000000L")
        myObjectBoxContent.contains("\"Hoolaloop\"")
        val entity = environment.schema.entities.single { it.className == "ToManyStandaloneUidName" }
        assertEquals(1, entity.toManyRelations.size)
        val toMany = entity.toManyRelations[0] as ToManyStandalone
        assertEquals("Hoolaloop", toMany.dbName)
        assertEquals(420000000L, toMany.modelId!!.uid)

        assertToManyStandaloneModel(environment, parentName, listOf("Hoolaloop"))
    }

    @Test
    fun testToManyAndConverter() {
        val parentName = "ToManyAndConverter"
        val childName = "IdEntity"

        val environment = TestEnvironment("to-many-and-converter.json")

        environment.compile(parentName, childName, "TestConverter")
            .assertThatIt { succeededWithoutWarnings() }
    }

    @Test
    fun toOne_relatedEntityIsGeneric_error() {
        val className = "ToOneGenerics"

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(className)
            .assertThatIt {
                failed()
                hadErrorCount(1)
                hadErrorContainingMatch("Property 'toOne' can not have a relation to type T")
            }
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun toMany_relatedEntityIsGeneric_error() {
        val className = "ToManyGenerics"

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        environment.compile(className)
            .assertThatIt {
                failed()
                hadErrorCount(1)
                hadErrorContainingMatch("Property 'toMany' can not have a relation to type T")
            }
        assertThat(environment.isModelFileExists()).isFalse()
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
                    assertType(prop, PropertyType.Long, hasNonPrimitiveFlag = true)
                }

                else -> fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
        // assert child properties
        val child = schema.entities.single { it.className == childName }
        val targetPropertyName = "target"
        for (prop in child.properties) {
            when (prop.propertyName) {
                "id" -> {
                    assertThat(prop.isPrimaryKey).isTrue()
                    assertThat(prop.isIdAssignable).isFalse()
                    assertThat(prop.dbName).isEqualTo("id")
                    assertType(prop, PropertyType.Long, hasNonPrimitiveFlag = true)
                }

                "${targetPropertyName}Id" -> {
                    assertThat(prop.dbName).isEqualTo(prop.propertyName)
                    assertThat(prop.virtualTargetName).isEqualTo(targetPropertyName)
                    assertPrimitiveType(prop, PropertyType.RelationId)
                    assertThat(child.indexes).hasSize(1)
                    assertThat(child.toOneRelations).hasSize(1)
                    assertToOneIndexAndRelation(child, parent, prop, toOneName = targetPropertyName)
                    assertToManyRelation(parent, child, prop)
                }

                else -> fail("Found stray property '${prop.propertyName}' in schema.")
            }
        }
    }

    private fun assertToManyRelation(parent: Entity, child: Entity, prop: Property) {
        for (toManyRelation in parent.toManyRelations) {
            when (toManyRelation.name) {
                "sources" -> {
                    assertThat(toManyRelation.sourceEntity).isEqualTo(parent)
                    assertThat(toManyRelation.targetEntity).isEqualTo(child)
                    val toMany = toManyRelation as ToManyByBacklink
                    assertThat(toMany.targetToMany).isNull()
                    assertThat(toMany.targetToOne!!.idRefProperty).isEqualTo(prop)
                    // generator takes care of populating sourceProperties if we do not set them, so do not assert here
                }

                "sourcesOther" -> {
                    assertThat(toManyRelation.sourceEntity).isEqualTo(parent)
                    assertThat(toManyRelation.targetEntity).isEqualTo(child)
                }

                else -> fail("Found stray toManyRelation '${toManyRelation.name}' in schema.")
            }
        }
    }

    private fun assertToOneIndexAndRelation(
        child: Entity, parent: Entity, prop: Property, toOneName: String,
        toOneFieldName: String = toOneName
    ) {
        // assert index
        val indexesForProperty = child.indexes.filter { it.properties[0] == prop }
        assertThat(indexesForProperty).hasSize(1)

        // assert to one relation
        val toOneRelation = child.toOneRelations.single { it.name == toOneName }
        assertThat(toOneRelation.targetEntity).isEqualTo(parent)
        assertThat(toOneRelation.idRefProperty).isEqualTo(prop)
        assertThat(toOneRelation.name).isEqualTo(toOneFieldName)
    }

    private fun assertToManyStandaloneModel(
        environment: TestEnvironment,
        parentName: String,
        relationNames: List<String>
    ) {
        val model = environment.readModel()
        val modelParent = model.findEntity(parentName, null)

        val relations = modelParent!!.relations!!
        assertThat(relations).isNotEmpty()

        for (relation in relations) {
            if (relationNames.contains(relation.name)) {
                assertThat(relation.id).isNotNull()
                assertThat(relation.id).isNotEqualTo(IdUid())
            } else fail("Found stray relation '${relation.name}' in model file.")
        }
    }

}