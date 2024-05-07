/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2024 ObjectBox Ltd.
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

package io.objectbox.gradle

import org.gradle.api.provider.Property

/**
 * Gradle plugin extension, which collects options for the plugin. Separate from annotation processor options!
 *
 * NOTE Requirements: abstract because Gradle inherits from it.
 * https://docs.gradle.org/current/userguide/custom_plugins.html#sec:getting_input_from_the_build
 */
abstract class ObjectBoxPluginExtension {

    /** If detailed log output should be created. */
    abstract val debug: Property<Boolean>

    init {
        @Suppress("LeakingThis") // Gradle docs ask to set it this way.
        debug.convention(false)
    }

}
