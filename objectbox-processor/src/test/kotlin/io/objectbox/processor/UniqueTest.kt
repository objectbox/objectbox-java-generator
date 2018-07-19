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

        assertHasIndexAndIsUnique(environment)
    }

    @Test
    fun unique_andIndex_makesIndexUnique() {
        val entity = "UniqueAndIndex"

        val environment = TestEnvironment("unique-and-index-temp.json")

        val compilation = environment.compile(entity)
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        assertHasIndexAndIsUnique(environment)
    }

    private fun assertHasIndexAndIsUnique(environment: TestEnvironment) {
        assertWithMessage("test files broken").that(environment.schema.entities).isNotEmpty()
        environment.schema.entities.forEach {
            assertWithMessage("test files broken").that(it.properties).isNotEmpty()
            it.properties.forEach propLoop@{
                if (it.isPrimaryKey) {
                    return@propLoop
                }

                // assert index is created
                assertWithMessage("${it.propertyName} should have index").that(it.index).isNotNull()

                // assert index is unique
                assertWithMessage("${it.propertyName} index is not unique").that(it.index.isUnique).isTrue()
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