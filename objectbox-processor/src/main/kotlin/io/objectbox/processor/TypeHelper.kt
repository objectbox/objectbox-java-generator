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

package io.objectbox.processor

import io.objectbox.generator.model.PropertyType
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import java.util.*
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
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
    private val typeObject = java.lang.Object::class.java.getTypeMirror()

    private val typeMap = java.util.Map::class.java.getTypeMirror(eraseTypeParameters = true)

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

    fun isStringList(typeMirror: TypeMirror): Boolean {
        if (!isList(typeMirror)) return false

        val typeArguments = (typeMirror as DeclaredType).typeArguments
        // Map must have 1, verify anyhow.
        if (typeArguments.size != 1) return false

        return typeArguments[0].isSameTypeAs(typeString)
    }

    private fun TypeMirror.isMapOf(
        expectedKeyType: TypeMirror,
        expectedValueType: TypeMirror? = null
    ): Boolean {
        if (!isSameTypeAs(typeMap, eraseTypeParameters = true)) return false

        val typeArguments = (this as DeclaredType).typeArguments
        // Map must have 2, verify anyhow.
        if (typeArguments.size != 2) return false

        val keyTypeMatches = typeArguments[0].isSameTypeAs(expectedKeyType)
        if (!keyTypeMatches || expectedValueType == null) {
            return keyTypeMatches
        }
        return typeArguments[1].isSameTypeAs(expectedValueType)
    }

    fun isStringMap(typeMirror: TypeMirror): Boolean {
        return typeMirror.isMapOf(typeString)
    }

    fun isStringLongMap(typeMirror: TypeMirror): Boolean {
        return typeMirror.isMapOf(typeString, typeLong)
    }

    fun isStringStringMap(typeMirror: TypeMirror): Boolean {
        return typeMirror.isMapOf(typeString, typeString)
    }

    fun isIntegerMap(typeMirror: TypeMirror): Boolean {
        return typeMirror.isMapOf(typeInteger)
    }

    fun isIntegerLongMap(typeMirror: TypeMirror): Boolean {
        return typeMirror.isMapOf(typeInteger, typeLong)
    }

    fun isLongMap(typeMirror: TypeMirror): Boolean {
        return typeMirror.isMapOf(typeLong)
    }

    fun isLongLongMap(typeMirror: TypeMirror): Boolean {
        return typeMirror.isMapOf(typeLong, typeLong)
    }

    fun isObject(typeMirror: TypeMirror): Boolean {
        return typeMirror.isSameTypeAs(typeObject)
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
            if (arrayComponentType.kind == TypeKind.BOOLEAN) {
                return PropertyType.BooleanArray
            }
            if (arrayComponentType.kind == TypeKind.BYTE) {
                return PropertyType.ByteArray
            }
            if (arrayComponentType.kind == TypeKind.SHORT) {
                return PropertyType.ShortArray
            }
            if (arrayComponentType.kind == TypeKind.CHAR) {
                return PropertyType.CharArray
            }
            if (arrayComponentType.kind == TypeKind.INT) {
                return PropertyType.IntArray
            }
            if (arrayComponentType.kind == TypeKind.LONG) {
                return PropertyType.LongArray
            }
            if (arrayComponentType.kind == TypeKind.FLOAT) {
                return PropertyType.FloatArray
            }
            if (arrayComponentType.kind == TypeKind.DOUBLE) {
                return PropertyType.DoubleArray
            }
            if (arrayComponentType.isSameTypeAs(typeString)) {
                return PropertyType.StringArray
            }
        }

        if (isStringList(typeMirror)) {
            return PropertyType.StringArray
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
