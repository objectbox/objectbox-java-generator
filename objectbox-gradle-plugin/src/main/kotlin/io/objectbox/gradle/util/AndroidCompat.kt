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

}