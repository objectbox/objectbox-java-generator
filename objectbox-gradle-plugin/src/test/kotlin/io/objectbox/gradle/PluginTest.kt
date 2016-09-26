package io.objectbox.gradle

import org.junit.Assert.*
import org.junit.Test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import org.gradle.testkit.runner.TaskOutcome.*

class PluginTest {
    @Test
    fun buildTestProject() {
        var dir = File ("test-project-files/")
        if(!dir.exists()) {
            dir = File ("objectbox-gradle-plugin/test-project-files/")
        }
        assertTrue(dir.absolutePath, dir.exists())
        val classpath = listOf(File("build/classes/main"),File("build/resources/main"))
        classpath.forEach { assertTrue(it.absolutePath, it.exists()) }
        val result = GradleRunner.create().withProjectDir(dir).withPluginClasspath(classpath).withArguments("objectbox").build()

//        assertTrue(result.getOutput().contains("Hello world!"))
        assertEquals(result.task(":objectbox").getOutcome(), SUCCESS)
    }
}