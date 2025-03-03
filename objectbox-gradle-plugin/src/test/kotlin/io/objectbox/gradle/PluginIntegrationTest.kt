/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
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
@Ignore("Hen and egg problem when bumping the version; TODO: fix or just use real integration tests")
class PluginIntegrationTest {

    @Test
    fun buildTestProjectJava() {
        val args = listOf("--stacktrace", "clean", "build")
        buildTestProject("java", args, "io/objectbox/test/entityannotation", "apt/main/")
    }

    @Test
    fun buildTestProjectJavaAndroid() {
        val args = listOf("--stacktrace", "clean", "build", "-xlint")
        buildTestProject("java-android", args, "io/objectbox/test", "apt/release/", true)
    }

//    @Test
//    @Ignore("FIXME kapt does not run, works fine if used as standalone project")
//    fun buildTestProjectKotlinAndroid() {
//        // Disable Lint, fails with kotlin-android
//        val args = listOf("--stacktrace", "clean", "build", "-xlint")
//        buildTestProject("kotlin-android", args, "io/objectbox/test/kotlin", "kapt/release/", true)
//    }

    private fun buildTestProject(
        name: String, args: List<String>, expectedPackageDir: String, genDirPath: String,
        generateBuildFile: Boolean = false
    ) {
        var dir = File("test-gradle-projects/$name")
        if (!dir.exists()) {
            dir = File("objectbox-gradle-plugin/test-gradle-projects/$name")
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

        if (generateBuildFile) {
            // add buildscript block to build file template to support adding plugins using 'apply'
            // this is required so the Android plugin is applied before the ObjectBox plugin
            val classpathString = classpath.joinToString("', '", "'", "'").replace("\\", "\\\\")
            val buildFile = File(dir, "build.gradle")
            val buildFileTemplate = File(dir, "build.gradle.template")
            buildFile.delete()
            buildFile.appendText(
                """buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath files($classpathString)
    }
}"""
            )
            buildFile.appendText(buildFileTemplate.readText())
        }

        val result = GradleRunner.create()
            .withProjectDir(dir)
            // to do: Make this work some time
//          .withPluginClasspath()
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