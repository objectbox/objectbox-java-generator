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
package io.objectbox.generator.model

import io.objectbox.generator.IdUid

/**
 * To-many relationship from a source entity to many target entities.
 */
class ToManyStandalone(
    name: String,
    dbName: String?,
    targetEntityName: String,
    isFieldAccessible: Boolean,
    uid: Long?
) : ToManyBase(name, targetEntityName, isFieldAccessible) {

    var modelId: IdUid? = null
    val dbName: String = dbName ?: name

    init {
        if (uid != null) {
            modelId = IdUid(0, uid)
        }
    }

}