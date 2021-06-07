package io.objectbox.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.TextUtil
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Note: if these tests fail to run because dependencies are missing,
 * check if the build.gradle file needs to publish additional artifacts to test repository.
 */
class IncrementalCompilationTest {

    @JvmField
    @Rule
    val testProjectDir: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val gitlabUrl = System.getProperty("gitlabUrl")
    private val gitlabTokenName = System.getProperty("gitlabTokenName")
    private val gitlabToken =  System.getProperty("gitlabToken")

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
        projectSetup()
        val sourceFile = testProjectDir.newFile("src/main/java/example/Example.java").apply {
            writeText(
                """
                package example;

                import io.objectbox.annotation.Entity;
                import io.objectbox.annotation.Id;
                
                @Entity
                public class Example {
                    @Id public long id;
                } 
                """.trimIndent()
            )
        }
        // Compile 1st time.
        with(compileJava()) {
            assertThat(output).contains(GRADLE_MSG_FULL_RECOMPILE_REQ_FIRST_BUILD)
        }

        // Add new property and all-properties constructor to class.
        sourceFile.writeText(
            """
            package example;

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
        with(compileJava()) {
            assertThat(output).doesNotContain(GRADLE_MSG_FULL_RECOMPILE_REQ)
            // Why 5 classes? Example.java, Example_.java, ExampleCursor.java + anonym. Factory class, MyObjectBox.java.
            assertThat(output).contains("Incremental compilation of 5 classes completed")
            assertThat(output).contains("[ObjectBox] Valid all-args constructor found")
        }
    }

    /**
     * Tests that during incremental compilation a direct super BaseEntity class can be seen.
     */
    @Test
    fun incrementalAnnotationProcessor_baseEntity() {
        projectSetup()
        testProjectDir.newFile("src/main/java/example/BaseExample.java").writeText(
            """
            package example;
            
            import io.objectbox.annotation.BaseEntity;
            import io.objectbox.annotation.Id;
            
            @BaseEntity
            public class BaseExample {
                @Id public long id;
            } 
            """.trimIndent()
        )
        val sourceFile = testProjectDir.newFile("src/main/java/example/Example.java").apply {
            writeText(
                """
                package example;

                import io.objectbox.annotation.Entity;
                
                @Entity
                public class Example extends BaseExample {
                } 
                """.trimIndent()
            )
        }
        // Compile 1st time.
        with(compileJava()) {
            assertThat(output).contains(GRADLE_MSG_FULL_RECOMPILE_REQ_FIRST_BUILD)
        }

        // Add new property to class.
        sourceFile.writeText(
            """
            package example;

            import io.objectbox.annotation.Entity;
            
            @Entity
            public class Example extends BaseExample {
                public String newProperty;
            } 
            """.trimIndent()
        )
        // Compile 2nd time.
        with(compileJava()) {
            assertThat(output).doesNotContain(GRADLE_MSG_FULL_RECOMPILE_REQ)
            // Why 5 classes? Example.java, Example_.java, ExampleCursor.java + anonym. Factory class, MyObjectBox.java.
            assertThat(output).contains("Incremental compilation of 5 classes completed")
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
        projectSetup()

        testProjectDir.newFile("src/main/java/example/BaseExample.java").writeText(
            """
            package example;
            
            import io.objectbox.annotation.BaseEntity;
            import io.objectbox.annotation.Id;
            
            @BaseEntity
            public class BaseExample {
                @Id public long id;
            } 
            """.trimIndent()
        )
        testProjectDir.newFile("src/main/java/example/IntermediateExample.java").writeText(
            """
            package example;

            import io.objectbox.annotation.Entity;
            
            public class IntermediateExample extends BaseExample {
            } 
            """.trimIndent()
        )
        val sourceFile = testProjectDir.newFile("src/main/java/example/Example.java").apply {
            writeText(
                """
                package example;

                import io.objectbox.annotation.Entity;
                
                @Entity
                public class Example extends IntermediateExample {
                } 
                """.trimIndent()
            )
        }
        // Compile 1st time.
        with(compileJava()) {
            assertThat(output).contains(GRADLE_MSG_FULL_RECOMPILE_REQ_FIRST_BUILD)
        }

        // Add new property to class.
        sourceFile.writeText(
            """
            package example;

            import io.objectbox.annotation.Entity;
            
            @Entity
            public class Example extends IntermediateExample {
                public String newProperty;
            } 
            """.trimIndent()
        )
        // Compile 2nd time.
        with(compileJava()) {
            assertThat(output).doesNotContain(GRADLE_MSG_FULL_RECOMPILE_REQ)
            // Why 5 classes? Example.java, Example_.java, ExampleCursor.java + anonym. Factory class, MyObjectBox.java.
            assertThat(output).contains("Incremental compilation of 5 classes completed")
            assertThat(output).contains("[ObjectBox] Detected entity inheritance chain: Example->BaseExample")
        }
    }

    private fun projectSetup(javaCompilerArgs: List<String> = emptyList()) {
        testProjectDir.newFile("settings.gradle").writeText("rootProject.name = 'incap-project'")

        val compilerArgs = javaCompilerArgs
            .plus("-Aobjectbox.debug=true")
            .joinToString(separator = "\",\"", prefix = "\"", postfix = "\"")

        // Note: instead of getting artifacts of the modules in this project from internal repo,
        // publish them to a directory in the build folder, then add that as repo below.
        val testRepository = TextUtil.normaliseFileSeparators(File("build/repository").absolutePath)
        val buildFile = testProjectDir.newFile("build.gradle")
        buildFile.writeText(
            """
            plugins {
                id 'java'
                id 'io.objectbox'
            }
            
            targetCompatibility = '1.8'
            sourceCompatibility = '1.8'

            repositories {
                maven { url "$testRepository" }
                mavenCentral()
                maven {
                    url "$gitlabUrl/api/v4/groups/objectbox/-/packages/maven"
                    credentials(HttpHeaderCredentials) {
                        name = "$gitlabTokenName"
                        value = "$gitlabToken"
                    }
                    authentication {
                        header(HttpHeaderAuthentication)
                    }
                }
            }
            
            configurations.all {
                // Projects are using snapshot dependencies that may update more often than 24 hours.
                resolutionStrategy {
                    cacheChangingModulesFor 0, 'seconds'
                }
            }
            
            // Enable ObjectBox plugin and processor debug output.
            objectbox {
                debug true
            }
            tasks.withType(JavaCompile) {
                options.compilerArgs += [ $compilerArgs ]
            }
            """.trimIndent()
        )

        testProjectDir.newFolder("src", "main", "java", "example")
    }

    private fun compileJava(): BuildResult {
        val pluginClasspathResource = javaClass.classLoader.getResource("plugin-classpath.txt")
            ?: throw IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")

        val pluginClasspath = pluginClasspathResource.readText().lines().map { File(it) }

        return GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("--info", "compileJava")
            .withPluginClasspath(pluginClasspath)
            .build()
    }

    companion object {
        private const val GRADLE_MSG_FULL_RECOMPILE_REQ_FIRST_BUILD =
            "Full recompilation is required because no incremental change information is available."
        private const val GRADLE_MSG_FULL_RECOMPILE_REQ = "Full recompilation is required "
    }

}
