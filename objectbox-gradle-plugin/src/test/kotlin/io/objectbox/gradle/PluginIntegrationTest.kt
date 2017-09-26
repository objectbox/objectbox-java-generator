package io.objectbox.gradle

import org.gradle.testkit.runner.GradleRunner
import org.greenrobot.essentials.StringUtils
import org.greenrobot.essentials.io.IoUtils
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileInputStream

/**
 * See https://docs.gradle.org/current/userguide/test_kit.html
 * See https://docs.gradle.org/current/javadoc/org/gradle/testkit/runner/GradleRunner.html
 */
class PluginIntegrationTest {

    @Test
    fun buildTestProjectJava() {
        val args = listOf("--stacktrace", "clean", "build")
        buildTestProject("java", args, "io/objectbox/test/entityannotation", "apt/main/")
    }

    @Test
    fun buildTestProjectJavaAndroid() {
        val args = listOf("--stacktrace", "clean", "build", "-xlint")
        buildTestProject("java-android", args, "io/objectbox/test", "apt/release/")
    }

//    @Test
//    @Ignore("FIXME kapt does not run, works fine if used as standalone project")
//    fun buildTestProjectKotlinAndroid() {
//        // Disable Lint, fails with kotlin-android
//        val args = listOf("--stacktrace", "clean", "build", "-xlint")
//        buildTestProject("kotlin-android", args, "io/objectbox/test/kotlin", "kapt/release/")
//    }

    fun buildTestProject(name: String, args: List<String>, expectedPackageDir: String, genDirPath: String) {
        var dir = File("test-gradle-projects/" + name)
        if (!dir.exists()) {
            dir = File("objectbox-gradle-plugin/test-gradle-projects/" + name)
        }
        assertTrue(dir.absolutePath, dir.exists())

        var classpathFileIn = javaClass.classLoader.getResourceAsStream("plugin-classpath.txt")
        if (classpathFileIn == null) {
            classpathFileIn = FileInputStream("build/createClasspathManifest/plugin-classpath.txt")
        }

        val classpathContent = IoUtils.readAllChars(classpathFileIn.bufferedReader()).replace("\\", "\\\\")
        val classpath = StringUtils.splitLines(classpathContent, true).map(::File)
        classpath.forEach {
            val path = it.absolutePath
            assertTrue(path, it.exists() || path.contains("/build/") || path.contains("\\build\\"))
        }

        val result = GradleRunner.create()
                .withProjectDir(dir)
                // to do: Make this work some time
//                .withPluginClasspath()
                .withPluginClasspath(classpath)
                // Note: args must be passed all at once, or they will overwrite each other
                .withArguments(args)
                .forwardOutput()
                .withDebug(true)
                .build()

        assertNotNull(result)

        val genSourceDir = File(dir, "build/generated/source/$genDirPath")
        assertTrue(genSourceDir.exists())

        val packageDir = File(genSourceDir, expectedPackageDir)
        assertTrue(packageDir.exists())

        assertEquals(9, packageDir.list().filter { it.endsWith(".java") }.size)
    }
}