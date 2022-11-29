package io.objectbox.gradle.util

import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import io.objectbox.gradle.transform.AndroidPlugin34
import io.objectbox.gradle.transform.AndroidPlugin72
import io.objectbox.gradle.transform.AndroidPluginCompat
import org.gradle.api.Project


object AndroidCompat {

    fun getPlugin(project: Project): AndroidPluginCompat {
        return try {
            getPluginByVersion(project)
        } catch (e: NoClassDefFoundError) {
            // Android Plugins before 7.0 do not have the AndroidComponentsExtension (or version API).
            AndroidPlugin34()
        }
    }

    private fun getPluginByVersion(project: Project): AndroidPluginCompat {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        return when {
            androidComponents.pluginVersion >= AndroidPluginVersion(7, 2, 0) -> AndroidPlugin72()
            else -> AndroidPlugin34()
        }
    }

    /**
     * Returns the plugin version string, like `7.0.0-alpha5`, if the Android Plugin is at least version 7.0 (previous
     * versions do not have an official version API), otherwise "pre-7.0".
     */
    fun getPluginVersion(project: Project): String {
        return try {
            val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
            androidComponents.pluginVersion.let {
                // Build string manually as toString includes more than just the version number.
                "${it.major}.${it.minor}.${it.micro}" +
                        (if (it.previewType != null) "-${it.previewType}" else "") +
                        (if (it.preview > 0) it.preview else "")
            }
        } catch (e: NoClassDefFoundError) {
            // Android Plugins before 7.0 do not have the AndroidComponentsExtension (or version API).
            "pre-7.0"
        }
    }

}