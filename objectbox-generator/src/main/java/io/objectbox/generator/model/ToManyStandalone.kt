/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
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
    uid: Long?,
    /**
     * The [io.objectbox.annotation.ExternalName] of this ToMany.
     */
    val externalName: String?,
    /**
     * The ID of the [io.objectbox.annotation.ExternalType] of this ToMany.
     *
     * See [ExternalPropertyTypeMapper.toId].
     */
    val externalTypeId: Short?,
    /**
     * The code expression to use in generated [io.objectbox.ModelBuilder] code that sets the
     * [io.objectbox.annotation.ExternalType] of this ToMany.
     *
     * See [ExternalPropertyTypeMapper.toExpression].
     */
    val externalTypeExpression: String?
) : ToManyBase(name, targetEntityName, isFieldAccessible) {

    var modelId: IdUid? = null
    val dbName: String = dbName ?: name

    init {
        if (uid != null) {
            modelId = IdUid(0, uid)
        }
    }

}