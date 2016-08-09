package org.greenrobot.greendao.codemodifier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

/**
 * Tests if the @ToOne and @ToMany annotations are properly parsed.
 */
class VisitorRelationsTest : VisitorTestBase() {

    @Test
    fun toOne() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.ToOne;

        @Entity
        class Foobar {
            String name;
            long barId;

            @ToOne(joinProperty = "barId")
            Bar bar;
        }
        """)!!
        assertThat(entity.oneRelations, equalTo(
                listOf(OneRelation(Variable(BarType, "bar"), foreignKeyField = "barId"))
        ))
    }

    @Test
    fun toOneWithoutProperty() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Column;
        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.ToOne;
        import org.jetbrains.annotations.NotNull;

        @Entity
        class Foobar {
            String name;

            @ToOne
            Bar bar;
        }
        """)!!
        assertThat(entity.oneRelations, equalTo(
                listOf(OneRelation(Variable(BarType, "bar")))
        ))
    }

    @Test
    fun toOneWithoutPropertyMore() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Column;
        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.Property;import org.greenrobot.greendao.annotation.ToOne;
        import org.greenrobot.greendao.annotation.Unique;
        import org.jetbrains.annotations.NotNull;

        @Entity
        class Foobar {
            String name;

            @ToOne
            @Unique
            @NotNull
            @Property(nameInDb = "BAR_ID")
            Bar bar;
        }
        """)!!
        assertThat(entity.oneRelations, equalTo(
                listOf(OneRelation(Variable(BarType, "bar"), columnName = "BAR_ID", isNotNull = true, unique = true))
        ))
    }

    @Test
    fun toMany() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.ToMany;

        import java.util.List;

        @Entity
        class Foobar {
            String name;

            @ToMany(referencedJoinProperty = "barId")
            List<Bar> bars;
        }
        """)!!
        assertThat(entity.manyRelations, equalTo(
                listOf(ManyRelation(Variable(BarListType, "bars"), mappedBy = "barId"))
        ))
    }

    @Test
    fun toManyWithMulticolumnJoin() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.JoinOn;
        import org.greenrobot.greendao.annotation.JoinProperty;import org.greenrobot.greendao.annotation.ToMany;

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
        """)!!
        assertThat(entity.manyRelations, equalTo(
                listOf(ManyRelation(Variable(BarListType, "bars"), joinOnProperties = listOf(
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

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.JoinEntity;
        import org.greenrobot.greendao.annotation.ToMany;

        import java.util.List;

        @Entity
        class Foo {
            String name;

            @ToMany
            @JoinEntity(entity = Foobar.class, sourceProperty = "fooId", targetProperty = "barId")
            List<Bar> bars;
        }
        """)!!
        assertThat(entity.manyRelations, equalTo(
                listOf(ManyRelation(Variable(BarListType, "bars"), joinEntitySpec =
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

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.OrderBy;
        import org.greenrobot.greendao.annotation.ToMany;

        import java.util.List;

        @Entity
        class Foo {
            String name;
            long barId;

            @ToMany(referencedJoinProperty = "barId")
            @OrderBy("date, likes DESC")
            List<Bar> bars;
        }
        """)!!
        assertThat(entity.manyRelations, equalTo(
                listOf(ManyRelation(Variable(BarListType, "bars"), mappedBy = "barId",
                        order = listOf(OrderProperty("date", Order.ASC), OrderProperty("likes", Order.DESC))
                ))
        ))
    }
}

