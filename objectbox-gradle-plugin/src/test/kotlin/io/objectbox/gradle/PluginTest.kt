package io.objectbox.gradle

import org.junit.Assert.*
import org.junit.Test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import org.gradle.testkit.runner.TaskOutcome.*
import org.greenrobot.essentials.StringUtils
import org.greenrobot.essentials.io.IoUtils
import org.junit.Ignore
import java.io.FileInputStream

/**
 * See https://docs.gradle.org/current/userguide/test_kit.html
 * See https://docs.gradle.org/current/javadoc/org/gradle/testkit/runner/GradleRunner.html
 */
class PluginTest {
    @Test
    fun buildTestProject() {
        var dir = File("test-project-files/")
        if (!dir.exists()) {
            dir = File("objectbox-gradle-plugin/test-project-files/")
        }
        assertTrue(dir.absolutePath, dir.exists())

        var classpathFileIn = javaClass.classLoader.getResourceAsStream("plugin-classpath.txt")
        if (classpathFileIn == null) {
            classpathFileIn = FileInputStream("build/createClasspathManifest/plugin-classpath.txt")
        }

        val classpathContent = IoUtils.readAllChars(classpathFileIn.bufferedReader()).replace("\\", "\\\\")
        val classpath = StringUtils.splitLines(classpathContent, true).map(::File)
        classpath.forEach { assertTrue(it.absolutePath, it.exists()) }

        val result = GradleRunner.create()
                .withProjectDir(dir)
                // to do: Make this work some time
//                .withPluginClasspath()
                .withPluginClasspath(classpath)
                // Note: args must be passed all at once, or they will overwrite each other
                .withArguments("--stacktrace", "clean", "objectbox", "build")
                .forwardOutput()
                .withDebug(true)
                .build()

        assertNotNull(result)

        val genSourceDir = File(dir, "build/generated/source/objectbox/")
        assertTrue(genSourceDir.exists())

        val packageDir = File(genSourceDir, "io/objectbox/test/entityannotation")
        assertTrue(packageDir.exists())

        assertEquals(9, packageDir.list().filter { it.endsWith(".java") }.size)
    }
}