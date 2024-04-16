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

package io.objectbox.gradle.transform

import org.gradle.api.Project
import org.gradle.api.provider.Property

abstract class AndroidPluginCompat {

    abstract fun registerTransform(project: Project, debug: Property<Boolean>, hasKotlinPlugin: Boolean)

    /**
     * Returns the Android application ID of the first found build variant of the given project.
     *
     * Must be called after the project is evaluated as the Android plugin can change variants until then.
     */
    abstract fun getFirstApplicationId(project: Project): String?

}