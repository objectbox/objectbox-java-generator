/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
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

package io.objectbox.gradle

import org.gradle.api.Project

/**
 * Gradle plugin extension, which collects options for the plugin. Separate from annotation processor options!
 *
 * NOTE Requirements: open because Gradle inherits from it, Project as constructor param.
 */
open class PluginOptions(@Suppress("unused") val project: Project) {

    /** If detailed log output should be created. */
    var debug: Boolean = false

}

fun Project.getObjectBoxPluginOptions(): PluginOptions? {
    return extensions.findByType(PluginOptions::class.java)
}
