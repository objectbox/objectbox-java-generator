/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2024 ObjectBox Ltd.
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

package io.objectbox.gradle.util

import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import io.objectbox.gradle.transform.AndroidPlugin72
import io.objectbox.gradle.transform.AndroidPluginCompat
import org.gradle.api.Project


object AndroidCompat {

    private const val ERROR_AGP_TOO_OLD = "The ObjectBox Gradle plugin requires Android Gradle Plugin 8.1 or newer"

    fun getPlugin(project: Project): AndroidPluginCompat {
        return try {
            getPluginByVersion(project)
        } catch (e: NoClassDefFoundError) {
            // Android Plugins before 7.0 do not have the AndroidComponentsExtension (or version API).
            error(ERROR_AGP_TOO_OLD)
        }
    }

    private fun getPluginByVersion(project: Project): AndroidPluginCompat {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        return when {
            // While this plugin supports 7.2 or higher, the ObjectBox Android library requires at least 8.1 since
            // release 4.2.0 (2025-03-04).
            androidComponents.pluginVersion >= AndroidPluginVersion(8, 1, 0) -> AndroidPlugin72()
            else -> error(ERROR_AGP_TOO_OLD)
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