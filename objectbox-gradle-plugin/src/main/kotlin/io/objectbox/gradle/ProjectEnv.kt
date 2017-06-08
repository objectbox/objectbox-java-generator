package io.objectbox.gradle

import io.objectbox.codemodifier.nullIfBlank
import org.gradle.api.Project
import java.util.*

class ProjectEnv(val project: Project) {
    object Const {
        const val name: String = "objectbox"
        const val packageName: String = "io/objectbox"
    }

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

    fun logDebug(msg: String) = project.logger.debug(msg)
    fun logInfo(msg: String) = project.logger.info(msg)
}