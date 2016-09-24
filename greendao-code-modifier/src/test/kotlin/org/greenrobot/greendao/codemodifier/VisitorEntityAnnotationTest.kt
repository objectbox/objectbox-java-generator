package org.greenrobot.greendao.codemodifier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isBlank
import org.junit.Assert
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
        import org.greenrobot.greendao.annotation.Entity;

        @Entity class Foobar {}
        """)
        Assert.assertNotNull(entity)
    }

    @Test
    fun entityIsRecognisedQualifiedName() {
        val entity = visit(
                //language=java
                """
        @org.greenrobot.greendao.annotation.Entity class Foobar {}
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
        import org.greenrobot.greendao.annotation.*;
        import org.redrobot.reddao.annotations.*;

        @Entity class Foobar {}
        """)
        Assert.assertNotNull(entity)
    }

    @Test
    fun activeEntity() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.Entity;

        @Entity(active = true) class Foobar {}
        """)!!
        Assert.assertTrue(entity.active)
    }

    @Test
    fun entityName() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

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

        import org.greenrobot.greendao.annotation.*;

        @Entity class Foobar {}
        """)!!
        Assert.assertEquals("com.user.myapp", entity.packageName)
    }

    @Test
    fun noPackageName() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity class Foobar {}
        """)!!
        assertThat(entity.packageName, isBlank)
    }

    @Test
    fun noCustomTableName() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity class Foobar {}
        """)!!
        Assert.assertNull(entity.dbName)
    }

    @Test
    fun customTableName() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

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
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {}
        """)!!
        Assert.assertEquals("default", entity.schema)
    }

    @Test
    fun customSchemaName() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

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
        import org.greenrobot.greendao.annotation.*;

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
        import org.greenrobot.greendao.annotation.*;

        @Entity
        @Keep
        class Foobar {}
        """)!!
        Assert.assertTrue(entity.keepSource)
    }

}
