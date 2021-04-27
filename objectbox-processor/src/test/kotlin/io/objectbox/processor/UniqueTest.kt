package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.JavaFileObjects
import org.junit.Test


class UniqueTest : BaseProcessorTest() {

    @Test
    fun unique_createsUniqueIndex() {
        val entity = "UniqueCreatesIndex"

        val environment = TestEnvironment("unique-creates-index.json", useTemporaryModelFile = true)

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        assertWithMessage("test files broken").that(environment.schema.entities).isNotEmpty()
        environment.schema.entities.forEach {
            assertWithMessage("test files broken").that(it.properties).isNotEmpty()
            it.properties.forEach propLoop@{ prop ->
                if (prop.isPrimaryKey) {
                    return@propLoop
                }

                // assert index is created
                assertWithMessage("${prop.propertyName} should have index")
                    .that(prop.index).isNotNull()

                // assert index is unique
                assertWithMessage("${prop.propertyName} index should be unique")
                    .that(prop.index!!.isUnique).isTrue()
                assertWithMessage("${prop.propertyName} on conflict flag should not be set")
                    .that(prop.index!!.uniqueOnConflictFlag).isEqualTo(0)
            }
        }
    }

    @Test
    fun unique_unsupportedProperties_failsWithError() {
        val entity = "UniqueUnsupported"

        val environment = TestEnvironment("unique-unsupported.json", useTemporaryModelFile = true)

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).failed()

        CompilationSubject.assertThat(compilation).hadErrorContaining("@Id property is unique and indexed by default, remove @Unique.")

        CompilationSubject.assertThat(compilation).hadErrorContaining("@Unique can not be used with a ToOne relation, remove @Unique.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Unique can not be used with a ToMany relation, remove @Unique.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Unique can not be used with a List relation, remove @Unique.")

        CompilationSubject.assertThat(compilation).hadErrorContaining("@Unique is not supported for Float, remove @Unique.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Unique is not supported for Double, remove @Unique.")
        CompilationSubject.assertThat(compilation).hadErrorContaining("@Unique is not supported for ByteArray, remove @Unique.")
    }

    @Test
    fun unique_andIndex_makesIndexUnique() {
        val entity = "UniqueAndIndex"

        val environment = TestEnvironment("unique-and-index.json", useTemporaryModelFile = true)

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        assertWithMessage("test files broken").that(environment.schema.entities).isNotEmpty()
        environment.schema.entities.forEach {
            assertWithMessage("test files broken").that(it.properties).isNotEmpty()
            it.properties.forEach propLoop@{ prop ->
                if (prop.isPrimaryKey) {
                    return@propLoop
                }

                // assert index is created
                assertWithMessage("${prop.propertyName} should have index").that(prop.index).isNotNull()

                if (prop.propertyName == "notUniqueProp") {
                    // assert index is non-unique
                    assertWithMessage("${prop.propertyName} index should not be unique")
                        .that(prop.index!!.isUnique).isFalse()
                } else {
                    // assert index is unique
                    assertWithMessage("${prop.propertyName} index should be unique")
                        .that(prop.index!!.isUnique).isTrue()
                }
                assertWithMessage("${prop.propertyName} on conflict flag should not be set")
                    .that(prop.index!!.uniqueOnConflictFlag).isEqualTo(0)
            }
        }
    }

    @Test
    fun unique_onConflictReplaceOnMultiple_fails() {
        val sourceFile = """
        package com.example;
        
        import io.objectbox.annotation.ConflictStrategy;
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Id;
        import io.objectbox.annotation.Unique;
        
        @Entity
        public class Example {
        
            @Id public long id;
        
            @Unique(onConflict = ConflictStrategy.REPLACE)
            public long replace1;
            
            @Unique(onConflict = ConflictStrategy.REPLACE)
            public long replace2;
        }
        """.trimIndent().let {
            JavaFileObjects.forSourceString("com.example.Example", it)
        }

        val environment = TestEnvironment("not-generated.json", useTemporaryModelFile = true)

        val compilation = environment.compile(listOf(sourceFile))
        CompilationSubject.assertThat(compilation).failed()
        CompilationSubject.assertThat(compilation).hadErrorContaining(
            "ConflictStrategy.REPLACE can only be used on a single property"
        )
        assertThat(environment.isModelFileExists()).isFalse()
    }

    @Test
    fun unique_generatedCodeHasFlag() {
        val entity = "UniqueGenerated"

        // need stable model file + ids to verify sources match
        val environment = TestEnvironment("unique-generated.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        compilation.assertGeneratedSourceMatches(
            "io.objectbox.processor.test.MyObjectBox",
            "MyObjectBox-unique.java"
        )
    }
}