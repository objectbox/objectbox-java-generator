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
            GradleVersion.current() >= GradleVersion.version("7.1") -> {
                Gradle71()
            }
            // Using moshi 1.13.0+ which contains a multi-release JAR which contains Java 16 byte code.
            // Gradle does not support multi-release JARs until 7.6.2 https://github.com/gradle/gradle/issues/24390,
            // however, Java 16 is already supported since Gradle 7.0, so just require 7.0.
            // Keep up-to-date with README.
            GradleVersion.current() >= GradleVersion.version("7.0") -> {
                GradleLegacy()
            }

            else -> {
                error("Gradle 7.0 or newer is required.")
            }
        }

        fun get(): GradleCompat {
            return instance
        }
    }

    abstract fun getJavaPluginSourceSets(project: Project): SourceSetContainer
}