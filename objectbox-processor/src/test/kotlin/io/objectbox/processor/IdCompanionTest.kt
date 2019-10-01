package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.JavaFileObjects
import io.objectbox.model.PropertyFlags
import org.junit.Test
import javax.tools.JavaFileObject

/**
 * Additional tests for the IdCompanion annotation. For basic tests see SimpleEntity test in ObjectBoxProcessorTest.
 */
class IdCompanionTest : BaseProcessorTest() {

    @Test
    fun datenano_primitive_supported() {
        val dateNanoLongPrimitive = """
        package com.example;
        import io.objectbox.annotation.DatabaseType;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.IdCompanion;
        import io.objectbox.annotation.Type;       

        @Entity
        public class Example {
            @Id long id;
            @IdCompanion @Type(DatabaseType.DateNano) long companion;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        assertIdCompanionDateNano(dateNanoLongPrimitive)
    }

    @Test
    fun datenano_nullable_supported() {
        val dateNanoLongNullable = """
        package com.example;
        import io.objectbox.annotation.DatabaseType;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.IdCompanion;
        import io.objectbox.annotation.Type;

        @Entity
        public class Example {
            @Id long id;
            @IdCompanion @Type(DatabaseType.DateNano) Long companion;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        assertIdCompanionDateNano(dateNanoLongNullable)
    }

    private fun assertIdCompanionDateNano(javaFileObject: JavaFileObject) {
        val environment = TestEnvironment("idcompanion-datenano.json", useTemporaryModelFile = true)

        val compilation = environment.compile(listOf(javaFileObject))
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // Assert model file.
        val model = environment.readModel()
        val modelEntity = model.findEntity("Example", null)
        assertThat(modelEntity).isNotNull()
        val modelPropertyCompanion = modelEntity!!.properties.find { it.name == "companion" }
        assertThat(modelPropertyCompanion).isNotNull()
        assertThat(modelPropertyCompanion!!.type).isEqualTo(io.objectbox.model.PropertyType.DateNano)
        assertThat(modelPropertyCompanion.flags).isEqualTo(PropertyFlags.ID_COMPANION)
    }

    @Test
    fun multiple_error() {
        val javaFileObject = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.IdCompanion;
        import java.util.Date;

        @Entity
        public class Example {
            @Id long id;
            @IdCompanion Date companion1;
            @IdCompanion Date companion2;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        val environment = TestEnvironment("idcompanion-multiple.json", useTemporaryModelFile = true)

        val compilation = environment.compile(listOf(javaFileObject))
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("'companion1' is already an @IdCompanion property, there can only be one.")
    }

    @Test
    fun not_date_unsupported() {
        val javaFileObject = """
        package com.example;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.IdCompanion;

        @Entity
        public class Example {
            @Id long id;
            @IdCompanion Long companion;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        val environment = TestEnvironment("idcompanion-not-date.json", useTemporaryModelFile = true)

        val compilation = environment.compile(listOf(javaFileObject))
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("@IdCompanion has to be of type Date or a long annotated with @Type(DateNano).")
    }

}