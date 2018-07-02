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

@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "unused")

package io.objectbox.gradle.transform

import io.objectbox.Cursor
import io.objectbox.EntityInfo
import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Entity
import io.objectbox.relation.RelationInfo
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import org.junit.Rule


@Entity
class EntityEmpty

@Entity
class EntityBoxStoreField {
    val __boxStore = Object()
}

@Entity
class EntityToOne {
    val entityEmpty = ToOne<EntityEmpty>(this, null)
}

object EntityToOne_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityToOneLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToOneLateInit {
    lateinit var entityEmpty: ToOne<EntityEmpty>
}

object EntityToOneLateInit_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityToOneLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToOneSuffix {
    lateinit var entityEmptyToOne: ToOne<EntityEmpty>
}

object EntityToOneSuffix_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityToOneLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToMany {
    val entityEmpty = ToMany<EntityEmpty>(this, null)
    val entityEmptyList = listOf<EntityEmpty>()
}

object EntityToMany_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityToOneLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToManyLateInit {
    lateinit var entityEmpty: ToMany<EntityEmpty>
}

object EntityToManyLateInit_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityToManyLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToManySuffix {
    lateinit var entityEmptyToMany: ToMany<EntityEmpty>
}

object EntityToManySuffix_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityToOneLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToManyListLateInit {
    lateinit var typelessList: List<*>
    lateinit var entityEmpty: List<EntityEmpty>
}

object EntityToManyListLateInit_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityToOneLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityTransientList {
    @Transient
    lateinit var transientList1: List<EntityEmpty>

    @io.objectbox.annotation.Transient
    lateinit var transientList2: List<EntityEmpty>

    lateinit var actualRelation: List<EntityEmpty>

    @Rule
    val dummyWithAlienAnnotation: Boolean = false
}

object EntityTransientList_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val actualRelation = RelationInfo<EntityToOneLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityMultipleCtors {
    lateinit var toMany: ToMany<EntityMultipleCtors>

    constructor()

    @Suppress("UNUSED_PARAMETER")
    constructor(someValue: String) : this()
}

object EntityMultipleCtors_ : EntityInfo<EntityMultipleCtors> {
    @JvmField
    val toMany = RelationInfo<EntityMultipleCtors, EntityMultipleCtors>(null, null, null, null)
}

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

@BaseEntity
open class EntityBase {
    var baseString: String? = null
}

interface EntityInterface {
    @Suppress("unused")
    fun foo() {}
}

@Entity
open class EntitySub : EntityBase(), EntityInterface {
    lateinit var entityEmptyToMany: ToMany<EntityEmpty>
    lateinit var entityEmptyToOne: ToOne<EntityEmpty>
    lateinit var entityEmptyList: List<EntityEmpty>
}

@Entity
class EntityRelationsInSuperEntity : EntitySub()

@Entity
class EntityRelationsInSuperBase : EntityBaseWithRelations()

class EntitySub_ : EntityInfo<EntitySub> {
    @JvmField
    val entityEmptyToOne = RelationInfo<EntitySub, EntityEmpty>(null, null, null, null)
    @JvmField
    val entityEmptyToMany = RelationInfo<EntitySub, EntityEmpty>(null, null, null, null)
}

class EntitySubCursor : Cursor<EntitySub>() {
    private fun attachEntity(@Suppress("UNUSED_PARAMETER") entity: EntitySub) {}
}

class TestCursor : Cursor<EntityBoxStoreField>() {
    private fun attachEntity(@Suppress("UNUSED_PARAMETER") entity: EntityBoxStoreField) {}
}

class CursorWithExistingImpl : Cursor<EntityBoxStoreField>() {
    private fun attachEntity(entity: EntityBoxStoreField) {
        System.out.println(entity)
    }
}

class JustCopyMe
