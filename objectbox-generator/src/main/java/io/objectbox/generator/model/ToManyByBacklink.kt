package io.objectbox.generator.model


class ToManyByBacklink(
    name: String,
    targetEntityName: String,
    val targetPropertyName: String?,
    isFieldAccessible: Boolean
) : ToManyBase(name, targetEntityName, isFieldAccessible) {

    var targetToOne: ToOne? = null
        set(value) {
            if (targetToMany != null) throw IllegalStateException("targetToMany must not be set.")
            field = value
        }

    var targetToMany: ToManyStandalone? = null
        set(value) {
            if (targetToOne != null) throw IllegalStateException("targetToOne must not be set.")
            field = value
        }

}