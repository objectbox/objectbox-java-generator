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

@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "unused")

package io.objectbox.gradle.transform

import io.objectbox.Cursor
import io.objectbox.EntityInfo
import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Entity
import io.objectbox.relation.RelationInfo
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne

@BaseEntity
open class EntityBase {
    var baseString: String? = null
}

interface EntityInterface {
    @Suppress("unused")
    fun foo() {
    }
}

@Entity
open class EntitySub : EntityBase(), EntityInterface {
    lateinit var entityEmptyToMany: ToMany<EntityEmpty>
    lateinit var entityEmptyToOne: ToOne<EntityEmpty>
    lateinit var entityEmptyList: List<EntityEmpty>
}

class EntitySub_ : EntityInfo<EntitySub>, EntityInfoStub<EntitySub>() {
    @JvmField
    val entityEmptyToOne = RelationInfo<EntitySub, EntityEmpty>(null, null, null, null)

    @JvmField
    val entityEmptyToMany = RelationInfo<EntitySub, EntityEmpty>(null, null, null, null)

    @JvmField
    val entityEmptyList = RelationInfo<EntitySub, EntityEmpty>(null, null, null, null)
}

class EntitySubCursor : Cursor<EntitySub>(null, 0, null, null) {
    override fun getId(entity: EntitySub): Long = throw NotImplementedError("Stub for testing")
    override fun put(entity: EntitySub): Long = throw NotImplementedError("Stub for testing")
    private fun attachEntity(@Suppress("UNUSED_PARAMETER") entity: EntitySub) {}
}

@Entity
class EntityRelationsInSuperEntity : EntitySub()

@BaseEntity
open class EntityBaseWithRelations {
    lateinit var entityBaseToMany: ToMany<EntityEmpty>
    lateinit var entityBaseToOne: ToOne<EntityEmpty>
    lateinit var entityBaseList: List<EntityEmpty>
}

open class EntityBaseNoAnnotation : EntityBaseWithRelations() {
    lateinit var entityNoBaseToMany: ToMany<EntityEmpty>
    lateinit var entityNoBaseToOne: ToOne<EntityEmpty>
    lateinit var entityNoBaseList: List<EntityEmpty>
}

@Entity
class EntityRelationsInSuperBase : EntityBaseWithRelations()
