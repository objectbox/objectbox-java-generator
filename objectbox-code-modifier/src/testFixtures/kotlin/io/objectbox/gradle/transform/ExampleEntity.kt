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

@file:Suppress("unused")

package io.objectbox.gradle.transform

import io.objectbox.EntityInfo
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.RelationInfo
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import io.objectbox.annotation.Transient as ObxTransient

@Entity
data class ExampleEntity(
    @Id var id: Long = 0,
    // Note: add an object default value to create a synthetic constructor
    // where an INVOKESPECIAL op occurs before the INVOKESPECIAL op initializing this.
    @Convert(converter = EntityEmptyConverter::class, dbType = String::class)
    var convertProperty: List<EntityEmpty> = listOf(EntityEmpty())
) {

    // Note: add a constructor initializing an object parameter to create
    // an INVOKESPECIAL op that occurs before the INVOKESPECIAL op initializing this.
    constructor() : this(convertProperty = listOf(EntityEmpty()))

    // Note: do not initialize transient properties,
    // so transformer would try to initialize them (if working incorrectly).
    @Transient
    lateinit var transientProperty: ToOne<ExampleEntity>

    @ObxTransient
    lateinit var transientProperty2: ToMany<ExampleEntity>

    lateinit var toOneProperty: ToOne<ExampleEntity>
    val toManyProperty = ToMany(this, ExampleEntity_.toManyProperty)
    lateinit var toManyListProperty: MutableList<ExampleEntity>
}

/**
 * These are required for the legacy ClassTransformer as Javassist compiles the code
 * it inserts (and the initializer code accesses these fields).
 */
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
object ExampleEntity_ : EntityInfo<ExampleEntity>, EntityInfoStub<ExampleEntity>() {

    @JvmField
    val toOneProperty = RelationInfo<ExampleEntity, ExampleEntity>(null, null, null, null)

    @JvmField
    val toManyProperty = RelationInfo<ExampleEntity, ExampleEntity>(null, null, null, null)

    @JvmField
    val toManyListProperty = RelationInfo<ExampleEntity, ExampleEntity>(null, null, null, null)

    @JvmField
    val __INSTANCE = this
}
