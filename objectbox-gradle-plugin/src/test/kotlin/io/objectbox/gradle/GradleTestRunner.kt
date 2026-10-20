/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2024 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle

import io.objectbox.gradle.ProjectEnv.Const
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Helps create a minimal Gradle project and assert its compilation.
 *
 * Note: if tests using this fail to run because dependencies are missing,
 * check if the build.gradle file needs to publish additional artifacts to test repository.
 */
class GradleTestRunner(
    private val testProjectDir: TemporaryFolder
) {

    val javaCompilerArgs: MutableList<String> = mutableListOf()
    var buildscriptBlock: String = ""
    val additionalPlugins: MutableList<String> = mutableListOf()

    /** Set to false to build a project that does not apply the ObjectBox plugin. */
    var applyObjectBoxPlugin: Boolean = true

    /** Additional blocks to add to the build script. Indent should be trimmed. */
    var additionalBlocks: MutableSet<String> = mutableSetOf()

    private val gitlabUrl = System.getProperty("gitlabUrl")
    private val gitlabTokenName = System.getProperty("gitlabTokenName")
    private val gitlabToken = System.getProperty("gitlabToken")

    private var buildFile: File? = null

    init {
        testProjectDir.newFolder("src", "main", "java", "com", "example")
        testProjectDir.newFile("settings.gradle").writeText("rootProject.name = 'obx-test-project'")
    }

    fun addSourceFile(name: String, content: String): File {
        return testProjectDir.newFile("src/main/java/com/example/$name").apply {
            writeText(content)
        }
    }

    /**
     * Enable debug mode if the annotation processor is run via the Java compiler.
     */
    fun debugJavacProcessor() {
        javaCompilerArgs += "-Aobjectbox.debug=true"
    }

    /**
     * Enable debug mode if the annotation processor is run using kapt.
     */
    fun debugKaptProcessor() {
        @Language("Groovy")
        val snippet =
            """
            kapt {
                arguments {
                    arg("objectbox.debug", "true")
                }
            }
            """.trimIndent()
        additionalBlocks += snippet
    }

    /**
     * Enable debug mode if the annotation processor is run using KSP.
     */
    fun debugKspProcessor() {
        @Language("Groovy")
        val snippet =
            """
            ksp {
                arg("objectbox.debug", "true")
            }
            """.trimIndent()
        additionalBlocks += snippet
    }

    private fun writeBuildFile() {
        // Note: instead of getting artifacts of the modules in this project from internal repo,
        // publish them to a directory in the build folder, then add that as repo below.
        val testRepository = File("build/repository").absolutePath.normaliseFileSeparators()
        buildFile?.delete() // Might change in between builds.
        val buildFile = testProjectDir.newFile("build.gradle")
            .also { buildFile = it }

        val pluginIds = if (applyObjectBoxPlugin) {
            additionalPlugins + Const.PLUGIN_ID
        } else additionalPlugins
        val plugins = pluginIds.joinToString(
            prefix = "plugins {\n",
            separator = "\n",
            postfix = "\n}"
        ) { "    id(\"$it\")" }

        @Language("Groovy")
        val repos =
            """
            repositories {
                maven { url "$testRepository" }
                mavenCentral()
                google()
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
                // Don't (permanently) add the local repo to avoid CI build failures. But to test
                // with locally built dependencies add:
                // mavenLocal()
            }
            
            configurations.all {
                // Projects are using snapshot dependencies that may update more often than 24 hours.
                resolutionStrategy {
                    cacheChangingModulesFor(0, "seconds")
                }
            }
            """.trimIndent()

        val javaCompilerConfig = if (javaCompilerArgs.isNotEmpty()) {
            val compilerArgs = javaCompilerArgs
                .joinToString(separator = "\",\"", prefix = "\"", postfix = "\"")

            @Language("Groovy")
            val snippet =
                """
                tasks.withType(JavaCompile) {
                    options.compilerArgs += [ $compilerArgs ]
                }
                """.trimIndent()
            snippet
        } else ""

        val debugObjectboxPlugin = if (applyObjectBoxPlugin) {
            @Language("Groovy")
            val snippet =
                """
                // Enable ObjectBox plugin debug output.
                objectbox {
                    debug = true
                }""".trimIndent()
            snippet
        } else ""

        val buildScript =
            buildscriptBlock + "\n" +
                    plugins + "\n" +
                    repos + "\n" +
                    additionalBlocks.joinToString(separator = "\n") + "\n" +
                    javaCompilerConfig + "\n" +
                    debugObjectboxPlugin

        buildFile.writeText(buildScript)
    }

    fun build(
        args: List<String>,
        additionalRunnerConfiguration: ((GradleRunner) -> Unit)? = null
    ): BuildResult {
        return createRunner(args, additionalRunnerConfiguration).build()
    }

    /**
     * Like [build], but expects the build to fail (like due to a processor error).
     */
    fun buildAndFail(
        args: List<String>,
        additionalRunnerConfiguration: ((GradleRunner) -> Unit)? = null
    ): BuildResult {
        return createRunner(args, additionalRunnerConfiguration).buildAndFail()
    }

    private fun createRunner(
        args: List<String>,
        additionalRunnerConfiguration: ((GradleRunner) -> Unit)?
    ): GradleRunner {
        writeBuildFile()

        val pluginClasspathResource = javaClass.classLoader.getResource("plugin-classpath.txt")
            ?: throw IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")

        val pluginClasspath = pluginClasspathResource.readText().lines().map { File(it) }

        val runner = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments(args)
            .withPluginClasspath(pluginClasspath)
        additionalRunnerConfiguration?.invoke(runner)
        return runner
    }

    /**
     * Converts all native file separators in the specified string to '/'.
     */
    private fun String.normaliseFileSeparators(): String = replace(File.separatorChar, '/')

}