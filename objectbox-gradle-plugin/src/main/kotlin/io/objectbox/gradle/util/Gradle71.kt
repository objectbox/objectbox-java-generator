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

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer


/**
 * Gradle 7.1 introduces API to get source sets from the
 * [Java plugin extension](https://docs.gradle.org/current/userguide/java_plugin.html#sec:java-extension).
 */
class Gradle71 : GradleCompat() {

    override fun getJavaPluginSourceSets(project: Project): SourceSetContainer {
        val javaExtension = project.extensions.findByType(JavaPluginExtension::class.java)
            ?: error("The Java plugin extension was not found.")
        return javaExtension.sourceSets
    }

}