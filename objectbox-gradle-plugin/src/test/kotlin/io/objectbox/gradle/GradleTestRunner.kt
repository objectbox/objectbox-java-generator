package io.objectbox.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
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
    var additionalBlocks: String = ""

    private val gitlabUrl = System.getProperty("gitlabUrl")
    private val gitlabTokenName = System.getProperty("gitlabTokenName")
    private val gitlabToken =  System.getProperty("gitlabToken")

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

    private fun writeBuildFile() {
        // Enable debug mode for plugin.
        val compilerArgs = javaCompilerArgs
            .plus("-Aobjectbox.debug=true")
            .joinToString(separator = "\",\"", prefix = "\"", postfix = "\"")

        // Note: instead of getting artifacts of the modules in this project from internal repo,
        // publish them to a directory in the build folder, then add that as repo below.
        val testRepository = File("build/repository").absolutePath.normaliseFileSeparators()
        buildFile?.delete() // Might change in between builds.
        val buildFile = testProjectDir.newFile("build.gradle")
            .also { buildFile = it }
        buildFile.writeText(
            """
            $buildscriptBlock                
                
            plugins {
                ${additionalPlugins.joinToString(separator = "\n") { "id(\"$it\")" }}
                id("io.objectbox")
            }
            
            java {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }

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
            }
            
            configurations.all {
                // Projects are using snapshot dependencies that may update more often than 24 hours.
                resolutionStrategy {
                    cacheChangingModulesFor(0, "seconds")
                }
            }
            
            $additionalBlocks
            
            // Enable ObjectBox plugin and processor debug output.
            objectbox {
                debug = true
            }
            tasks.withType(JavaCompile) {
                options.compilerArgs += [ $compilerArgs ]
            }
            """.trimIndent()
        )
    }

    private fun build(task: String): BuildResult {
        writeBuildFile()

        val pluginClasspathResource = javaClass.classLoader.getResource("plugin-classpath.txt")
            ?: throw IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")

        val pluginClasspath = pluginClasspathResource.readText().lines().map { File(it) }

        return GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("--info", task)
            .withPluginClasspath(pluginClasspath)
            .build()
    }

    fun assemble(): BuildResult {
        return build("assemble")
    }

    fun assembleDebug(): BuildResult {
        return build("assembleDebug")
    }

    /**
     * Converts all native file separators in the specified string to '/'.
     */
    private fun String.normaliseFileSeparators(): String = replace(File.separatorChar, '/')

}