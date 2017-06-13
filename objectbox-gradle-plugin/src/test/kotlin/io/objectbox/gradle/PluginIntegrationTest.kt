package io.objectbox.gradle

import org.gradle.testkit.runner.GradleRunner
import org.greenrobot.essentials.StringUtils
import org.greenrobot.essentials.io.IoUtils
import org.junit.Assert.*
import org.junit.Ignore
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
        buildTestProject("java", args, "io/objectbox/test/entityannotation")
    }

    @Test
    fun buildTestProjectKotlinJavaEntitiesAndroid() {
        // Disable Lint, fails with kotlin-android
        val args = listOf("--stacktrace", "clean", "build", "-xlint")
        buildTestProject("kotlin-java-entities-android", args, "io/objectbox/test/kotlin")
    }

//    @Test
//    @Ignore("FIXME")
//    fun buildTestProjectKotlinAndroid() {
//        // Disable Lint, fails with kotlin-android
//        val args = listOf("--stacktrace", "--refresh-dependencies", "clean", "build", "-xlint")
//        buildTestProject("kotlin-android", args, "io/objectbox/test/kotlin")
//    }

    fun buildTestProject(name: String, args: List<String>, expectedPackageDir: String) {
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
        classpath.forEach { assertTrue(it.absolutePath, it.name.endsWith("test") || it.exists()) }

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

        val genSourceDir = File(dir, "build/generated/source/objectbox/")
        assertTrue(genSourceDir.exists())

        val packageDir = File(genSourceDir, expectedPackageDir)
        assertTrue(packageDir.exists())

        assertEquals(9, packageDir.list().filter { it.endsWith(".java") }.size)
    }
}