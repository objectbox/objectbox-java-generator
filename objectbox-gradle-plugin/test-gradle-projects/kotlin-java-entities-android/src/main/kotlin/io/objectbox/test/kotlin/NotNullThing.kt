package io.objectbox.test.kotlin

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Generated
import io.objectbox.annotation.Id
import io.objectbox.annotation.apihint.Internal

@Entity
data class NotNullThing (

    @Id
    var id: Long? = null,

    val nullableBoolean: Boolean?,
    var nullableInteger: Int?,

    val notNullBoolean: Boolean = false,
    var notNullInteger: Int = 0
)

