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
    val testProjectDir = TemporaryFolder()

    private val internalObjectBoxRepo = System.getProperty("internalObjectBoxRepo")
    private val internalObjectBoxRepoUser = System.getProperty("internalObjectBoxRepoUser")
    private val internalObjectBoxRepoPassword = System.getProperty("internalObjectBoxRepoPassword")

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
            assertThat(output).contains("Full recompilation is required because no incremental change information is available.")
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
            assertThat(output).doesNotContain("Full recompilation is required ")
            // Why 4 classes? Example.java, Example_.java, ExampleCursor.java, MyObjectBox.java.
            assertThat(output).contains("Incremental compilation of 4 classes completed")
            assertThat(output).contains("[ObjectBox] Valid all-args constructor found")
        }
    }

    private fun projectSetup() {
        testProjectDir.newFile("settings.gradle").writeText("rootProject.name = 'incap-project'")

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
                jcenter()
                maven {
                    url "$internalObjectBoxRepo"
                    credentials {
                        username "$internalObjectBoxRepoUser"
                        password "$internalObjectBoxRepoPassword"
                    }
                }
            }
            
            // Enable ObjectBox plugin and processor debug output.
            objectbox {
                debug true
            }
            tasks.withType(JavaCompile) {
                options.compilerArgs += [ "-Aobjectbox.debug=true" ]
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

}
