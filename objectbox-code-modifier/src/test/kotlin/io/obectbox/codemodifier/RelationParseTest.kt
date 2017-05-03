package io.obectbox.codemodifier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import io.objectbox.codemodifier.*
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test

/**
 * Tests @Relation parsing (to-one and to-many)
 */
class RelationParseTest : VisitorTestBase() {

    @Test
    fun toOne() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.relation.ToOne;

        @Entity
        class Foobar {
            String name;

            ToOne<Bar> bar;
        }
        """, listOf("Bar"))!!
        assertThat(entity.toOneRelations, hasSize(equalTo(1)))
        val toOneRelation = entity.toOneRelations[0]
        assertEquals(toOneRelation.variable.type.name, "io.objectbox.relation.ToOne")
        assertEquals(toOneRelation.targetType, BarType)
        assertNull(toOneRelation.targetIdName)
        assertTrue(toOneRelation.variableIsToOne)
    }

    @Test
    fun toOneTargetWithCustomTargetId() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Relation;

        @Entity
        class Foobar {
            String name;
            long customBarId;

            @Relation(idProperty = "customBarId")
            Bar bar;
        }
        """, listOf("Bar"))!!

        assertToOneBar(entity.toOneRelations.single(), "customBarId")
    }

    @Test
    fun toOneTargetWithDefaultTargetIdProperty() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Relation;

        @Entity
        class Foobar {
            String name;

            @Relation
            Bar bar;
        }
        """, listOf("Bar"))!!

        assertToOneBar(entity.toOneRelations.single(), null)
    }

    private fun assertToOneBar(toOne: ToOneRelation, expectedTargetIdFieldName: String?) {
        assertEquals(Variable(BarType, "bar"), toOne.variable)
        assertEquals(BarType, toOne.targetType)
        assertEquals(expectedTargetIdFieldName, toOne.targetIdName)
    }

    @Test
    fun toOneTargetWithAdditionalAnnotations() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Column;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Property;
        import io.objectbox.annotation.Relation;
        // unsupported: import io.objectbox.annotation.Unique;
        import org.jetbrains.annotations.NotNull;

        @Entity
        class Foobar {
            String name;

            @SuppressWarnings("NullableProblems")
            @Relation
            // unsupported: @Unique
            @NotNull
            Bar bar;
        }
        """, listOf("Bar"))!!

        val toOne = entity.toOneRelations.single()
        assertToOneBar(toOne, null)
        assertTrue(toOne.isNotNull)
    }

    @Test
    fun toManyTarget() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Relation;

        import java.util.List;

        @Entity
        class Foobar {
            String name;

            @Relation(idProperty = "barId")
            List<Bar> bars;
        }
        """, listOf("Bar"))!!

        val toMany = entity.toManyRelations.single()
        assertEquals(Variable(BarListType, "bars"), toMany.variable);
        assertEquals("barId", toMany.mappedBy)
    }

    @Test
    @Ignore("to-many with multiple join conditions is not supported by ObjectBox")
    fun toManyWithMulticolumnJoin() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.JoinOn;
        import io.objectbox.annotation.JoinProperty;
        import io.objectbox.annotation.ToMany;

        import java.util.List;

        @Entity
        class Foobar {
            String name;
            long barId;
            long barSubId;

            @ToMany(joinProperties = {
                @JoinProperty(name = "barId", referencedName = "id"),
                @JoinProperty(name = "barSubId", referencedName = "subId")
            })
            List<Bar> bars;
        }
        """, listOf("Bar"))!!
        assertThat(entity.toManyRelations, equalTo(
                listOf(ToManyRelation(Variable(BarListType, "bars"), joinOnProperties = listOf(
                        JoinOnProperty("barId", "id"),
                        JoinOnProperty("barSubId", "subId")
                )))
        ))
    }

    @Test
    @Ignore("to-many with order is not yet supported by ObjectBox")
    fun toManyOrderBy() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.OrderBy;
        import io.objectbox.annotation.ToMany;

        import java.util.List;

        @Entity
        class Foo {
            String name;
            long barId;

            @ToMany(referencedJoinProperty = "barId")
            @OrderBy("date, likes DESC")
            List<Bar> bars;
        }
        """, listOf("Bar"))!!
        assertThat(entity.toManyRelations, equalTo(
                listOf(ToManyRelation(Variable(BarListType, "bars"), mappedBy = "barId",
                        order = listOf(OrderProperty("date", Order.ASC), OrderProperty("likes", Order.DESC))
                ))
        ))
    }
}

