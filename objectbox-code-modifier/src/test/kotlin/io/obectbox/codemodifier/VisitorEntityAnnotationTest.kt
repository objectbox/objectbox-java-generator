package io.obectbox.codemodifier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isBlank
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test

/**
 * Tests if the @Entity annotation is properly parsed.
 */
class VisitorEntityAnnotationTest : VisitorTestBase() {

    @Test
    fun entityIsRecognised() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.Entity;

        @Entity class Foobar {}
        """)
        Assert.assertNotNull(entity)
    }

    @Test
    fun entityIsRecognisedQualifiedName() {
        val entity = visit(
                //language=java
                """
        @io.objectbox.annotation.Entity class Foobar {}
        """)
        Assert.assertNotNull(entity)
    }

    @Test
    fun entityIsNotRecognized() {
        val entity = visit("class Foobar {}")
        Assert.assertNull(entity)
    }

    @Test
    fun entityIsNotRecognizedIfWrongAnnotation() {
        val entity = visit(
                //language=java
                """
        import myapp.Entity;

        @Entity class Foobar {}
        """)
        Assert.assertNull(entity)
    }

    @Test
    fun entityIsNotRecognizedWithoutAnnotationImport() {
        val entity = visit(
                //language=java
                """
        @Entity class Foobar {}
        """)
        Assert.assertNull(entity)
    }

    @Test
    fun entityIsRecognizedWithWildcardImports() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;
        import org.redrobot.reddao.annotations.*;

        @Entity class Foobar {}
        """)
        Assert.assertNotNull(entity)
    }

    @Test
    @Ignore("Feature not yet available")
    fun activeEntity() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.Entity;

        @Entity(active = true) class Foobar {}
        """)!!
        Assert.assertTrue(entity.active)
    }

    @Test
    fun entityName() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity class Foobar {}
        """)!!
        Assert.assertEquals("Foobar", entity.name)
    }

    @Test
    fun packageName() {
        val entity = visit(
                //language=java
                """
        package com.user.myapp;

        import io.objectbox.annotation.*;

        @Entity class Foobar {}
        """)!!
        Assert.assertEquals("com.user.myapp", entity.packageName)
    }

    @Test
    fun noPackageName() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity class Foobar {}
        """)!!
        assertThat(entity.packageName, isBlank)
    }

    @Test
    fun noCustomTableName() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity class Foobar {}
        """)!!
        Assert.assertNull(entity.dbName)
    }

    @Test
    fun customTableName() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity(nameInDb = "BAR")
        class Foobar {}
        """)!!
        Assert.assertEquals("BAR", entity.dbName)
    }

    @Test
    fun defaultSchemaName() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {}
        """)!!
        Assert.assertEquals("default", entity.schema)
    }

    @Test
    @Ignore("Feature not yet available")
    fun customSchemaName() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity(schema="custom")
        class Foobar {}
        """)!!
        Assert.assertEquals("custom", entity.schema)
    }

    @Test
    fun noKeepAnnotation() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {}
        """)!!
        Assert.assertFalse(entity.keepSource)
    }

    @Test
    fun keepAnnotation() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        @Keep
        class Foobar {}
        """)!!
        Assert.assertTrue(entity.keepSource)
    }

    @Test
    fun uid() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        @Uid(1234567890L)
        class Foobar {
            String noUid;

            @Uid(9876543210L)
            String name;
        }
        """)!!
        assertEquals(1234567890L, entity.uid)
        assertNull(entity.properties[0].uid)
        assertEquals("name", entity.properties[1].variable.name)
        assertEquals(9876543210L, entity.properties[1].uid)
    }

}