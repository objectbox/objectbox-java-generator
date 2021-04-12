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

package io.objectbox.processor

import io.objectbox.generator.model.PropertyType
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import java.util.*
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * Helps translate processor types to objectbox types.
 */
class TypeHelper(
    private val elementUtils: Elements,
    private val typeUtils: Types
) {

    // The following Java platform types should always exist, hence always return an element (see getTypeMirror()).
    private val typeShort = java.lang.Short::class.java.getTypeMirror()
    private val typeInteger = java.lang.Integer::class.java.getTypeMirror()
    private val typeLong = java.lang.Long::class.java.getTypeMirror()
    private val typeFloat = java.lang.Float::class.java.getTypeMirror()
    private val typeDouble = java.lang.Double::class.java.getTypeMirror()
    private val typeBoolean = java.lang.Boolean::class.java.getTypeMirror()
    private val typeByte = java.lang.Byte::class.java.getTypeMirror()
    private val typeDate = Date::class.java.getTypeMirror()
    private val typeCharacter = java.lang.Character::class.java.getTypeMirror()
    private val typeString = java.lang.String::class.java.getTypeMirror()

    // The ToOne and ToMany ObjectBox types should exist if there are @Entity classes (Java lib must be in classpath).
    private val typeToOne = ToOne::class.java.getTypeMirror(eraseTypeParameters = true)
    private val typeToMany = ToMany::class.java.getTypeMirror(eraseTypeParameters = true)
    private val typeList = List::class.java.getTypeMirror(eraseTypeParameters = true)

    /**
     * Checks if this [TypeMirror] is the same type as [otherType]. If [eraseTypeParameters] is set,
     * erases type parameters before comparing (e.g. `ToOne<Example>` as `ToOne`).
     */
    private fun TypeMirror.isSameTypeAs(otherType: TypeMirror, eraseTypeParameters: Boolean = false): Boolean {
        return typeUtils.isSameType(if (eraseTypeParameters) typeUtils.erasure(this) else this, otherType)
    }

    fun isToOne(typeMirror: TypeMirror): Boolean {
        return typeMirror.isSameTypeAs(typeToOne, eraseTypeParameters = true)
    }

    fun isToMany(typeMirror: TypeMirror): Boolean {
        return typeMirror.isSameTypeAs(typeToMany, eraseTypeParameters = true)
    }

    fun isList(typeMirror: TypeMirror): Boolean {
        return typeMirror.isSameTypeAs(typeList, eraseTypeParameters = true)
    }

    /**
     * Tries to return a matching property type.
     */
    fun getPropertyType(typeMirror: TypeMirror?): PropertyType? {
        if (typeMirror == null) {
            return null
        }

        val kind = typeMirror.kind

        // also handles Kotlin types as they are mapped to Java primitive (wrapper) types at compile time
        if (typeMirror.isSameTypeAs(typeShort) || kind == TypeKind.SHORT) {
            return PropertyType.Short
        }
        if (typeMirror.isSameTypeAs(typeInteger) || kind == TypeKind.INT) {
            return PropertyType.Int
        }
        if (typeMirror.isSameTypeAs(typeLong) || kind == TypeKind.LONG) {
            return PropertyType.Long
        }

        if (typeMirror.isSameTypeAs(typeFloat) || kind == TypeKind.FLOAT) {
            return PropertyType.Float
        }
        if (typeMirror.isSameTypeAs(typeDouble) || kind == TypeKind.DOUBLE) {
            return PropertyType.Double
        }

        if (typeMirror.isSameTypeAs(typeBoolean) || kind == TypeKind.BOOLEAN) {
            return PropertyType.Boolean
        }
        if (typeMirror.isSameTypeAs(typeByte) || kind == TypeKind.BYTE) {
            return PropertyType.Byte
        }
        if (typeMirror.isSameTypeAs(typeDate)) {
            return PropertyType.Date
        }
        if (typeMirror.isSameTypeAs(typeCharacter) || kind == TypeKind.CHAR) {
            return PropertyType.Char
        }
        if (typeMirror.isSameTypeAs(typeString)) {
            return PropertyType.String
        }

        if (kind == TypeKind.ARRAY) {
            val arrayComponentType = (typeMirror as ArrayType).componentType
            if (arrayComponentType.kind == TypeKind.BYTE) {
                return PropertyType.ByteArray
            }
            if (arrayComponentType.isSameTypeAs(typeString)) {
                return PropertyType.StringArray
            }
        }

        return null
    }

    /**
     * Returns the [TypeMirror] of this class by finding its
     * type element in the current processor environment.
     *
     * Note: do not use for classes that might not exist in the processor environment.
     */
    private fun <T> Class<T>.getTypeMirror(eraseTypeParameters: Boolean = false): TypeMirror {
        val type = elementUtils.getTypeElement(canonicalName)!!.asType()
        return if (eraseTypeParameters) {
            typeUtils.erasure(type)
        } else {
            type
        }
    }
}
