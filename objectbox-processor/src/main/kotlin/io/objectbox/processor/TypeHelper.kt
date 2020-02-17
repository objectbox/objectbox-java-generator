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

    /**
     * Checks if the type name is equal to the given type name.
     */
    fun isTypeEqualTo(typeMirror: TypeMirror, otherType: String, eraseTypeParameters: Boolean = false): Boolean {
        return if (eraseTypeParameters) {
            otherType == typeUtils.erasure(typeMirror).toString()
        } else {
            otherType == typeMirror.toString()
        }
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
        if (typeUtils.isSameType(typeMirror, typeShort) || kind == TypeKind.SHORT) {
            return PropertyType.Short
        }
        if (typeUtils.isSameType(typeMirror, typeInteger) || kind == TypeKind.INT) {
            return PropertyType.Int
        }
        if (typeUtils.isSameType(typeMirror, typeLong) || kind == TypeKind.LONG) {
            return PropertyType.Long
        }

        if (typeUtils.isSameType(typeMirror, typeFloat) || kind == TypeKind.FLOAT) {
            return PropertyType.Float
        }
        if (typeUtils.isSameType(typeMirror, typeDouble) || kind == TypeKind.DOUBLE) {
            return PropertyType.Double
        }

        if (typeUtils.isSameType(typeMirror, typeBoolean) || kind == TypeKind.BOOLEAN) {
            return PropertyType.Boolean
        }
        if (typeUtils.isSameType(typeMirror, typeByte) || kind == TypeKind.BYTE) {
            return PropertyType.Byte
        }
        if (typeUtils.isSameType(typeMirror, typeDate)) {
            return PropertyType.Date
        }
        if (typeUtils.isSameType(typeMirror, typeCharacter) || kind == TypeKind.CHAR) {
            return PropertyType.Char
        }
        if (typeUtils.isSameType(typeMirror, typeString)) {
            return PropertyType.String
        }

        if (kind == TypeKind.ARRAY) {
            val arrayType = typeMirror as ArrayType
            if (arrayType.componentType.kind == TypeKind.BYTE) {
                return PropertyType.ByteArray
            }
        }

        return null
    }

    private fun <T> Class<T>.getTypeMirror(): TypeMirror {
        return elementUtils.getTypeElement(canonicalName).asType()
    }
}
