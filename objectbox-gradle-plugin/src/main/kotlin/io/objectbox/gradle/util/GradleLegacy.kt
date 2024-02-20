/*
 * Copyright (C) 2022-2023 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle.util

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer

open class GradleLegacy : GradleCompat() {

    override fun getJavaPluginSourceSets(project: Project): SourceSetContainer {
        // Replaced by JavaPluginExtension, see Gradle71. Scheduled for removal in Gradle 9.0.
        @Suppress("DEPRECATION")
        return project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
    }
}