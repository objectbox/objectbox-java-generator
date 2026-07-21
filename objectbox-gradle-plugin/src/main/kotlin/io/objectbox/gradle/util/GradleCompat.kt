/*
 * ObjectBox Build Tools
 * Copyright (C) 2019-2025 ObjectBox Ltd.
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

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.util.GradleVersion

// Note: would move implementations into separate modules, but the gradleApi() dependency version
// is determined by the Gradle version used to build the project.
abstract class GradleCompat {

    companion object {
        private val instance = when {
            // This plugin is compiled with a Kotlin API level only supported by Gradle 8.3 or newer
            // (see root build script), so require at least Gradle 8.3.
            // Keep up-to-date with README.
            GradleVersion.current() >= GradleVersion.version("8.3") -> {
                Gradle71()
            }

            else -> {
                error("Gradle 8.3 or newer is required.")
            }
        }

        fun get(): GradleCompat {
            return instance
        }
    }

    abstract fun getJavaPluginSourceSets(project: Project): SourceSetContainer
}