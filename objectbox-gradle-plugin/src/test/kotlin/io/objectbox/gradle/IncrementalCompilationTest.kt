package io.objectbox.gradle

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IncrementalCompilationTest {

    @JvmField
    @Rule
    val testProjectDir: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    /**
     * Tests that when changing entity compilation is incremental,
     * checks constructor can still be detected.
     *
     * Note: Gradle docs state that aggregating annotation processors
     * do not see parameter names by default, however in this and other tests they do?!
     * Keep the check anyhow to detect future regressions.
     *
     * https://docs.gradle.org/current/userguide/java_plugin.html#sec:incremental_annotation_processing
     */
    @Test
    fun incrementalAnnotationProcessor() {
        val gradleRunner = createRunner()
        val sourceFile = gradleRunner.addSourceFile(
            "Example.java",
            """
                package com.example;

                import io.objectbox.annotation.Entity;
                import io.objectbox.annotation.Id;
                
                @Entity
                public class Example {
                    @Id public long id;
                } 
                """.trimIndent()
        )
        // Compile 1st time.
        with(gradleRunner.assemble()) {
            assertThat(output).contains(GRADLE_MSG_FULL_RECOMPILE_REQ_FIRST_BUILD)
        }

        // Add new property and all-properties constructor to class.
        sourceFile.writeText(
            """
            package com.example;

            import io.objectbox.annotation.Entity;
            import io.objectbox.annotation.Id;
            
            @Entity
            public class Example {
                @Id public long id; 
                public String newProperty;
                public Example(long id, String newProperty) {
                    this.id = id;
                    this.newProperty = newProperty;
                }
            } 
            """.trimIndent()
        )
        // Compile 2nd time.
        with(gradleRunner.assemble()) {
            assertThat(output).doesNotContain(GRADLE_MSG_FULL_RECOMPILE_REQ)
            // Why 6 classes? Example.java + anonym. ID-Getter class, Example_.java,
            // ExampleCursor.java + anonym. Factory class, MyObjectBox.java.
            assertThat(output).contains("Incremental compilation of 6 classes completed")
            assertThat(output).contains("[ObjectBox] Valid all-args constructor found")
        }
    }

    /**
     * Tests that during incremental compilation a direct super BaseEntity class can be seen.
     */
    @Test
    fun incrementalAnnotationProcessor_baseEntity() {
        val gradleRunner = createRunner()
        gradleRunner.addSourceFile(
            "BaseExample.java",
            """
            package com.example;
            
            import io.objectbox.annotation.BaseEntity;
            import io.objectbox.annotation.Id;
            
            @BaseEntity
            public class BaseExample {
                @Id public long id;
            } 
            """.trimIndent()
        )
        val sourceFile = gradleRunner.addSourceFile(
            "Example.java",
            """
                package com.example;

                import io.objectbox.annotation.Entity;
                
                @Entity
                public class Example extends BaseExample {
                } 
                """.trimIndent()
        )
        // Compile 1st time.
        with(gradleRunner.assemble()) {
            assertThat(output).contains(GRADLE_MSG_FULL_RECOMPILE_REQ_FIRST_BUILD)
        }

        // Add new property to class.
        sourceFile.writeText(
            """
            package com.example;

            import io.objectbox.annotation.Entity;
            
            @Entity
            public class Example extends BaseExample {
                public String newProperty;
            } 
            """.trimIndent()
        )
        // Compile 2nd time.
        with(gradleRunner.assemble()) {
            assertThat(output).doesNotContain(GRADLE_MSG_FULL_RECOMPILE_REQ)
            // Why 6 classes? Example.java + anonym. ID-Getter class, Example_.java,
            // ExampleCursor.java + anonym. Factory class, MyObjectBox.java.
            assertThat(output).contains("Incremental compilation of 6 classes completed")
            assertThat(output).contains("[ObjectBox] Detected entity inheritance chain: Example->BaseExample")
        }
    }

    /**
     * Tests that an indirect super BaseEntity class can be seen.
     *
     * Background: during an incremental processor run RoundEnvironment.rootElements
     * only contains annotated elements (for this processor BaseEntity and Entity classes).
     * The processor should then still be able to detect indirect super classes that are annotated.
     */
    @Test
    fun incrementalAnnotationProcessor_baseEntityIndirect() {
        val gradleRunner = createRunner()
        gradleRunner.addSourceFile(
            "BaseExample.java",
            """
            package com.example;
            
            import io.objectbox.annotation.BaseEntity;
            import io.objectbox.annotation.Id;
            
            @BaseEntity
            public class BaseExample {
                @Id public long id;
            } 
            """.trimIndent()
        )
        gradleRunner.addSourceFile(
            "IntermediateExample.java",
            """
            package com.example;

            import io.objectbox.annotation.Entity;
            
            public class IntermediateExample extends BaseExample {
            } 
            """.trimIndent()
        )
        val sourceFile = gradleRunner.addSourceFile(
            "Example.java",
            """
                package com.example;

                import io.objectbox.annotation.Entity;
                
                @Entity
                public class Example extends IntermediateExample {
                } 
                """.trimIndent()
        )
        // Compile 1st time.
        with(gradleRunner.assemble()) {
            assertThat(output).contains(GRADLE_MSG_FULL_RECOMPILE_REQ_FIRST_BUILD)
        }

        // Add new property to class.
        sourceFile.writeText(
            """
            package com.example;

            import io.objectbox.annotation.Entity;
            
            @Entity
            public class Example extends IntermediateExample {
                public String newProperty;
            } 
            """.trimIndent()
        )
        // Compile 2nd time.
        with(gradleRunner.assemble()) {
            assertThat(output).doesNotContain(GRADLE_MSG_FULL_RECOMPILE_REQ)
            // Why 6 classes? Example.java + anonym. ID-Getter class, Example_.java,
            // ExampleCursor.java + anonym. Factory class, MyObjectBox.java.
            assertThat(output).contains("Incremental compilation of 6 classes completed")
            assertThat(output).contains("[ObjectBox] Detected entity inheritance chain: Example->BaseExample")
        }
    }

    private fun createRunner(): GradleTestRunner {
        return GradleTestRunner(testProjectDir)
            .apply { additionalPlugins += "java-library" }
    }

    companion object {
        private const val GRADLE_MSG_FULL_RECOMPILE_REQ_FIRST_BUILD =
            "Full recompilation is required because no incremental change information is available."
        private const val GRADLE_MSG_FULL_RECOMPILE_REQ = "Full recompilation is required "
    }

}
