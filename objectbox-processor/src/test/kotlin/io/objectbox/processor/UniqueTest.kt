package io.objectbox.processor

import com.google.common.truth.Truth.assertWithMessage
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.JavaFileObjects
import org.junit.Test


class UniqueTest : BaseProcessorTest() {

    @Test
    fun unique_createsUniqueIndex() {
        val entity = "UniqueCreatesIndex"

        val environment = TestEnvironment("unique-creates-index-temp.json")

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

                // assert index is unique
                assertWithMessage("${prop.propertyName} index is not unique").that(prop.index.isUnique).isTrue()
            }
        }
    }

    @Test
    fun unique_unsupportedProperties_failsWithError() {
        val entity = "UniqueUnsupported"

        val environment = TestEnvironment("unique-unsupported-temp.json")

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

        val environment = TestEnvironment("unique-and-index-temp.json")

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
                    assertWithMessage("${prop.propertyName} index should not be unique").that(prop.index.isUnique).isFalse()
                } else {
                    // assert index is unique
                    assertWithMessage("${prop.propertyName} index should be unique").that(prop.index.isUnique).isTrue()
                }
            }
        }
    }

    @Test
    fun unique_generatedCodeHasFlag() {
        val entity = "UniqueGenerated"

        // need stable model file + ids to verify sources match
        val environment = TestEnvironment("unique-generated.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        val generatedFile = CompilationSubject.assertThat(compilation)
                .generatedSourceFile("io.objectbox.processor.test.MyObjectBox")
        generatedFile.isNotNull()
        generatedFile.hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/MyObjectBox-unique.java"))
    }
}