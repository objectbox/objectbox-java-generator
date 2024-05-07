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

package io.objectbox.gradle.transform

import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Transient

/**
 * Expected string representations of types, fields and methods used for probing and transforming entity and cursor
 * byte code.
 *
 * @see ClassProber
 * @see ClassTransformer
 */
object ClassConst {
    val entityAnnotationName = Entity::class.qualifiedName!!
    val baseEntityAnnotationName = BaseEntity::class.qualifiedName!!
    val transientAnnotationName = Transient::class.qualifiedName!!
    val convertAnnotationName = Convert::class.qualifiedName!!

    const val toOne = "io.objectbox.relation.ToOne"
    const val toOneDescriptor = "Lio/objectbox/relation/ToOne;"

    const val toMany = "io.objectbox.relation.ToMany"
    const val toManyDescriptor = "Lio/objectbox/relation/ToMany;"

    const val entityInfo = "io.objectbox.EntityInfo"
    const val relationInfo = "io.objectbox.relation.RelationInfo"

    const val boxStoreFieldName = "__boxStore"
    const val boxStoreClass = "io.objectbox.BoxStore"

    const val cursorClass = "io.objectbox.Cursor"
    const val cursorAttachEntityMethodName = "attachEntity"
    const val cursorBoxStoreFieldName = "boxStoreForEntities"

    const val listDescriptor = "Ljava/util/List;"

}