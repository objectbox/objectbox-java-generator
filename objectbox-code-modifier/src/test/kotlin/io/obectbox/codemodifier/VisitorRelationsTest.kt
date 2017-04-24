package io.obectbox.codemodifier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.objectbox.codemodifier.*
import org.junit.Ignore
import org.junit.Test

/**
 * Tests if the @ToOne and @ToMany annotations are properly parsed.
 */
@Ignore("Feature not yet available")
class VisitorRelationsTest : VisitorTestBase() {

    @Test
    fun toOne() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.ToOne;

        @Entity
        class Foobar {
            String name;
            long barId;

            @ToOne(joinProperty = "barId")
            Bar bar;
        }
        """, listOf("Bar"))!!
        assertThat(entity.toOneRelations, equalTo(
                listOf(ToOneRelation(Variable(BarType, "bar"), targetIdField = "barId"))
        ))
    }

    @Test
    fun toOneWithoutProperty() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Column;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.ToOne;
        import org.jetbrains.annotations.NotNull;

        @Entity
        class Foobar {
            String name;

            @ToOne
            Bar bar;
        }
        """, listOf("Bar"))!!
        assertThat(entity.toOneRelations, equalTo(
                listOf(ToOneRelation(Variable(BarType, "bar")))
        ))
    }

    @Test
    fun toOneWithoutPropertyMore() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Column;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Property;import io.objectbox.annotation.ToOne;
        import io.objectbox.annotation.Unique;
        import org.jetbrains.annotations.NotNull;

        @Entity
        class Foobar {
            String name;

            @SuppressWarnings("NullableProblems")
            @ToOne
            @Unique
            @NotNull
            Bar bar;
        }
        """, listOf("Bar"))!!
        assertThat(entity.toOneRelations, equalTo(
                listOf(ToOneRelation(Variable(BarType, "bar"), isNotNull = true, unique = true))
        ))
    }

    @Test
    fun toMany() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.ToMany;

        import java.util.List;

        @Entity
        class Foobar {
            String name;

            @ToMany(referencedJoinProperty = "barId")
            List<Bar> bars;
        }
        """, listOf("Bar"))!!
        assertThat(entity.toManyRelations, equalTo(
                listOf(ToManyRelation(Variable(BarListType, "bars"), mappedBy = "barId"))
        ))
    }

    @Test
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
    fun toManyWithJoinEntity() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.JoinEntity;
        import io.objectbox.annotation.ToMany;

        import java.util.List;

        @Entity
        class Foo {
            String name;

            @ToMany
            @JoinEntity(entity = Foobar.class, sourceProperty = "fooId", targetProperty = "barId")
            List<Bar> bars;
        }
        """, listOf("Bar", "Foobar"))!!
        assertThat(entity.toManyRelations, equalTo(
                listOf(ToManyRelation(Variable(BarListType, "bars"), joinEntitySpec =
                JoinEntitySpec("com.example.Foobar", "fooId", "barId")
                ))
        ))
    }

    @Test
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

