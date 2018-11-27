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

    val toOne = "io.objectbox.relation.ToOne"
    val toOneDescriptor = "Lio/objectbox/relation/ToOne;"

    val toMany = "io.objectbox.relation.ToMany"
    val toManyDescriptor = "Lio/objectbox/relation/ToMany;"

    val entityInfo = "io.objectbox.EntityInfo"
    val relationInfo = "io.objectbox.relation.RelationInfo"

    val boxStoreFieldName = "__boxStore"
    val boxStoreClass = "io.objectbox.BoxStore"

    val cursorClass = "io.objectbox.Cursor"
    val cursorAttachEntityMethodName = "attachEntity"

    val listDescriptor = "Ljava/util/List;"

}