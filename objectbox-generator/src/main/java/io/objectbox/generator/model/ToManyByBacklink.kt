/*
 * Copyright (C) 2020-2024 ObjectBox Ltd.
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

package io.objectbox.generator.model


class ToManyByBacklink(
    name: String,
    targetEntityName: String,
    val targetPropertyName: String?,
    isFieldAccessible: Boolean
) : ToManyBase(name, targetEntityName, isFieldAccessible) {

    var targetToOne: ToOne? = null
        set(value) {
            if (targetToMany != null) throw IllegalStateException("targetToMany must not be set.")
            field = value
        }

    var targetToMany: ToManyStandalone? = null
        set(value) {
            if (targetToOne != null) throw IllegalStateException("targetToOne must not be set.")
            field = value
        }

}