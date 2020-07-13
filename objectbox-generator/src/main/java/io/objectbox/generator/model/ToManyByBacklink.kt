package io.objectbox.generator.model


class ToManyByBacklink(
    name: String,
    targetEntityName: String,
    val targetPropertyName: String?,
    isFieldAccessible: Boolean
) : ToManyBase(name, targetEntityName, isFieldAccessible) {



}