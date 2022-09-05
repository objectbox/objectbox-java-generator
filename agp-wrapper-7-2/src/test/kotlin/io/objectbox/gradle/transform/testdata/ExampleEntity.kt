@file:Suppress("unused")

package io.objectbox.gradle.transform.testdata

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.gradle.transform.EntityEmpty
import io.objectbox.gradle.transform.EntityEmptyConverter
import io.objectbox.gradle.transform.EntityInfoStub
import io.objectbox.relation.RelationInfo
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import kotlin.jvm.Transient
import io.objectbox.annotation.Transient as ObxTransient

@Entity
data class ExampleEntity(
    @Id var id: Long = 0,
    @Transient var transientProperty: ToOne<ExampleEntity>? = null,
    @ObxTransient var transientProperty2: ToMany<ExampleEntity>? = null,
    // Note: add an object default value to create a synthetic constructor
    // where an INVOKESPECIAL op occurs before the INVOKESPECIAL op initializing this.
    @Convert(converter = EntityEmptyConverter::class, dbType = String::class)
    var convertProperty: List<EntityEmpty> = listOf(EntityEmpty())
) {

    // Note: add a constructor initializing an object parameter to create
    // an INVOKESPECIAL op that occurs before the INVOKESPECIAL op initializing this.
    constructor() : this(convertProperty = listOf(EntityEmpty()))

    lateinit var toOneProperty: ToOne<ExampleEntity>
    val toManyProperty = ToMany(this, ExampleEntity_.toManyProperty)
    lateinit var toManyListProperty: MutableList<ExampleEntity>
}

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
object ExampleEntity_ : EntityInfoStub<ExampleEntity>() {
    @JvmField
    val toManyProperty = RelationInfo<ExampleEntity, ExampleEntity>(null, null, null, null)

    @JvmField
    val __INSTANCE = this
}
