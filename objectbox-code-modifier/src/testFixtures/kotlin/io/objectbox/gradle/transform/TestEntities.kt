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

@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "unused", "UNUSED_PARAMETER")

package io.objectbox.gradle.transform

import io.objectbox.BoxStore
import io.objectbox.Cursor
import io.objectbox.EntityInfo
import io.objectbox.Property
import io.objectbox.annotation.Entity
import io.objectbox.internal.CursorFactory
import io.objectbox.internal.IdGetter
import io.objectbox.relation.RelationInfo
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne


@Entity
class EntityEmpty

@Entity
class EntityBoxStoreField {
    @JvmField // mimic generated Java code (transform adds field, not property with set/get)
    var __boxStore: BoxStore? = null
}

@Entity
class EntityToOne {
    val entityEmpty = ToOne<EntityEmpty>(this, null)
}

object EntityToOne_ : EntityInfo<EntityToOneLateInit>, EntityInfoStub<EntityToOneLateInit>() {
    @JvmField
    val entityEmpty = RelationInfo<EntityToOneLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToOneLateInit {
    lateinit var entityEmpty: ToOne<EntityEmpty>
}

object EntityToOneLateInit_ : EntityInfo<EntityToOneLateInit>, EntityInfoStub<EntityToOneLateInit>() {
    @JvmField
    val entityEmpty = RelationInfo<EntityToOneLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToOneSuffix {
    lateinit var entityEmptyToOne: ToOne<EntityEmpty>
}

object EntityToOneSuffix_ : EntityInfo<EntityToOneLateInit>, EntityInfoStub<EntityToOneLateInit>() {
    @JvmField
    val entityEmpty = RelationInfo<EntityToOneLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToMany {
    val entityEmpty = ToMany<EntityEmpty>(this, null)
    val entityEmptyList = listOf<EntityEmpty>()
}

object EntityToMany_ : EntityInfo<EntityToOneLateInit>, EntityInfoStub<EntityToOneLateInit>() {
    @JvmField
    val entityEmpty = RelationInfo<EntityToOneLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToManyLateInit {
    lateinit var entityEmpty: ToMany<EntityEmpty>
}

object EntityToManyLateInit_ : EntityInfo<EntityToOneLateInit>, EntityInfoStub<EntityToOneLateInit>() {
    @JvmField
    val entityEmpty = RelationInfo<EntityToManyLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToManySuffix {
    lateinit var entityEmptyToMany: ToMany<EntityEmpty>
}

object EntityToManySuffix_ : EntityInfo<EntityToOneLateInit>, EntityInfoStub<EntityToOneLateInit>() {
    @JvmField
    val entityEmpty = RelationInfo<EntityToOneLateInit, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToManyListLateInit {
    lateinit var typelessList: List<*>
    lateinit var entityEmpty: List<EntityEmpty>
}

object EntityToManyListLateInit_ : EntityInfo<EntityToOneLateInit>, EntityInfoStub<EntityToOneLateInit>() {
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

    @Deprecated(message = "non-ObjectBox annotation")
    val dummyWithAlienAnnotation: Boolean = false
}

object EntityTransientList_ : EntityInfo<EntityToOneLateInit>, EntityInfoStub<EntityToOneLateInit>() {
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

object EntityMultipleCtors_ : EntityInfo<EntityMultipleCtors>, EntityInfoStub<EntityMultipleCtors>() {
    @JvmField
    val toMany = RelationInfo<EntityMultipleCtors, EntityMultipleCtors>(null, null, null, null)
}

class TestCursor : Cursor<EntityBoxStoreField>(null, 0, null, null) {
    override fun getId(entity: EntityBoxStoreField): Long = throw NotImplementedError("Stub for testing")
    override fun put(entity: EntityBoxStoreField): Long = throw NotImplementedError("Stub for testing")
    private fun attachEntity(entity: EntityBoxStoreField) {}
}

class CursorExistingImplReads : Cursor<EntityBoxStoreField>(null, 0, null, null) {
    override fun getId(entity: EntityBoxStoreField): Long = throw NotImplementedError("Stub for testing")
    override fun put(entity: EntityBoxStoreField): Long = throw NotImplementedError("Stub for testing")
    private fun attachEntity(entity: EntityBoxStoreField) {
        println(entity.__boxStore)
    }
}

class CursorExistingImplWrites : Cursor<EntityBoxStoreField>(null, 0, null, null) {
    override fun getId(entity: EntityBoxStoreField): Long = throw NotImplementedError("Stub for testing")
    override fun put(entity: EntityBoxStoreField): Long = throw NotImplementedError("Stub for testing")
    private fun attachEntity(entity: EntityBoxStoreField) {
        entity.__boxStore = super.boxStoreForEntities!!
    }
}

class JustCopyMe

open class EntityInfoStub<T> : EntityInfo<T> {
    override fun getEntityName(): String = throw NotImplementedError("Stub for testing")
    override fun getDbName(): String = throw NotImplementedError("Stub for testing")
    override fun getEntityClass(): Class<T> = throw NotImplementedError("Stub for testing")
    override fun getEntityId(): Int = throw NotImplementedError("Stub for testing")
    override fun getAllProperties(): Array<Property<T>> = throw NotImplementedError("Stub for testing")
    override fun getIdProperty(): Property<T> = throw NotImplementedError("Stub for testing")
    override fun getIdGetter(): IdGetter<T> = throw NotImplementedError("Stub for testing")
    override fun getCursorFactory(): CursorFactory<T> = throw NotImplementedError("Stub for testing")
}
