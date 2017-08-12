package io.objectbox.gradle

import io.objectbox.codemodifier.nullIfBlank
import org.gradle.api.Project
import java.util.*

class ProjectEnv(val project: Project) {
    object Const {
        const val name: String = "objectbox"
        const val packageName: String = "io/objectbox"
    }

    /** Works only if first called inside a Gradle task NOT plugin! */
    val options by lazy { project.extensions.create(Const.name, ObjectBoxOptions::class.java, project)!! }

    val hasAndroidPlugin = listOf("android", "android-library", "com.android.application", "com.android.library")
            .any { project.plugins.hasPlugin(it) }

    val hasKotlinAndroidPlugin = project.plugins.hasPlugin("kotlin-android")
    val hasKotlinPlugin = project.plugins.hasPlugin("kotlin")
    val hasJavaPlugin = project.plugins.hasPlugin("java")

    val objectBoxVersion: String by lazy {
        val properties = Properties()
        val stream = javaClass.getResourceAsStream("/${Const.packageName}/gradle/version.properties")
        stream?.use {
            properties.load(it)
        }
        properties.getProperty("version").nullIfBlank()
                ?: throw RuntimeException("Version unavailable (bad Gradle build?)")
    }

    /**
     * See https://developer.android.com/studio/build/gradle-plugin-3-0-0-migration.html#new_configurations and
     * https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph
     */
    val dependencyScopeApiOrCompile: String by lazy {
        if (project.configurations.findByName("api") != null) "api" else "compile"
    }

    fun logDebug(msg: String) = project.logger.debug(msg)
    fun logInfo(msg: String) = project.logger.info(msg)
    fun logWarn(msg: String) = project.logger.warn(msg)
}