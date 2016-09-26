package io.objectbox.gradle

import org.junit.Assert.*
import org.junit.Test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import org.gradle.testkit.runner.TaskOutcome.*
import org.greenrobot.essentials.StringUtils
import org.greenrobot.essentials.io.IoUtils
import java.io.FileInputStream

class PluginTest {
    @Test
    fun buildTestProject() {
        var dir = File ("test-project-files/")
        if(!dir.exists()) {
            dir = File ("objectbox-gradle-plugin/test-project-files/")
        }
        assertTrue(dir.absolutePath, dir.exists())
//        val classpath = listOf(File("build/classes/main"),File("build/resources/main"))


        var classpathFileIn= javaClass.classLoader.getResourceAsStream("plugin-classpath.txt")
        if (classpathFileIn == null) {
            classpathFileIn = FileInputStream("build/createClasspathManifest/plugin-classpath.txt")
        }

        val classpathContent = IoUtils.readAllChars(classpathFileIn.bufferedReader()).replace("\\", "\\\\")
        val classpath = StringUtils.splitLines(classpathContent, true).map(::File)
        classpath.forEach { assertTrue(it.absolutePath, it.exists()) }

        val result = GradleRunner.create().withProjectDir(dir)
//                .withPluginClasspath()
                .withPluginClasspath(classpath)
                .withArguments("--stacktrace").withArguments("--info")
                .withArguments("objectbox")
                .forwardOutput()
                .withDebug(true)
                .build()

        assertNotNull(result)
        println (result.output)
//        assertTrue(result.getOutput().contains("Hello world!"))
        assertNotNull(result.task("objectbox"))
        assertEquals(result.task("objectbox").getOutcome(), SUCCESS)
    }
}