/*
 * ObjectBox Build Tools
 * Copyright (C) 2026 ObjectBox Ltd.
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

package io.objectbox.processor.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Nullability
import io.objectbox.generator.model.PropertyType

/**
 * Helps translate KSP types to objectbox types.
 *
 * KSP maps Java types to their Kotlin equivalents (like `java.lang.String` to `kotlin.String`,
 * `int` and `java.lang.Integer` to `kotlin.Int`), so type checks are based on Kotlin names.
 * Java primitives map to not-null types, boxed types to platform types, which mirrors how the
 * annotation processor distinguishes primitive and wrapper types.
 */
class KspTypeHelper {

    fun qualifiedName(type: KSType?): String? = type?.declaration?.qualifiedName?.asString()

    fun isToOne(type: KSType): Boolean = qualifiedName(type) == "io.objectbox.relation.ToOne"

    fun isToMany(type: KSType): Boolean = qualifiedName(type) == "io.objectbox.relation.ToMany"

    fun isList(type: KSType): Boolean = qualifiedName(type) in LIST_TYPES

    fun isStringList(type: KSType): Boolean {
        if (!isList(type)) return false
        if (type.arguments.size != 1) return false
        // Note: like the annotation processor sees types after KAPT stub generation, ignore
        // nullability of the type argument (List<String?> maps to List<String> in Java).
        return qualifiedName(type.arguments[0].type?.resolve()) == "kotlin.String"
    }

    private fun KSType.isMapOf(expectedKeyType: String, expectedValueType: String? = null): Boolean {
        if (qualifiedName(this) !in MAP_TYPES) return false
        if (arguments.size != 2) return false
        val keyTypeMatches = qualifiedName(arguments[0].type?.resolve()) == expectedKeyType
        if (!keyTypeMatches || expectedValueType == null) {
            return keyTypeMatches
        }
        return qualifiedName(arguments[1].type?.resolve()) == expectedValueType
    }

    fun isStringMap(type: KSType): Boolean = type.isMapOf("kotlin.String")

    fun isStringLongMap(type: KSType): Boolean = type.isMapOf("kotlin.String", "kotlin.Long")

    fun isStringStringMap(type: KSType): Boolean = type.isMapOf("kotlin.String", "kotlin.String")

    fun isIntegerMap(type: KSType): Boolean = type.isMapOf("kotlin.Int")

    fun isIntegerLongMap(type: KSType): Boolean = type.isMapOf("kotlin.Int", "kotlin.Long")

    fun isLongMap(type: KSType): Boolean = type.isMapOf("kotlin.Long")

    fun isLongLongMap(type: KSType): Boolean = type.isMapOf("kotlin.Long", "kotlin.Long")

    fun isObject(type: KSType): Boolean = qualifiedName(type) == "kotlin.Any"

    /**
     * True if the type is a Kotlin inline value class (like `UInt` or a custom `@JvmInline value class`).
     */
    fun isValueClass(type: KSType): Boolean {
        val declaration = type.declaration as? KSClassDeclaration ?: return false
        // Only custom inline value classes have the "value" modifier,
        // the stdlib types, like UInt, appear to use "inline".
        return declaration.modifiers.contains(Modifier.VALUE) || declaration.modifiers.contains(Modifier.INLINE)
    }

    /**
     * Returns the type a not-null inline value class type compiles to (the type of its single
     * underlying property, recursively if that is a value class itself), like `kotlin.Byte` for
     * `kotlin.UByte`. Returns other types (including nullable value class types, which compile to
     * the boxed value class) as is.
     */
    fun toJavaViewType(type: KSType): KSType {
        if (type.nullability != Nullability.NOT_NULL || !isValueClass(type)) return type
        val declaration = type.declaration as KSClassDeclaration
        val underlyingType = declaration.primaryConstructor?.parameters?.singleOrNull()?.type?.resolve()
            ?: return type
        return toJavaViewType(underlyingType)
    }

    /**
     * True if the type compiles to a Java primitive: a Kotlin scalar type that is not-null
     * (Java primitives map to not-null types, boxed types to platform types).
     */
    fun isPrimitiveLike(type: KSType): Boolean {
        return type.nullability == Nullability.NOT_NULL && qualifiedName(type) in SCALAR_TYPES
    }

    /**
     * Tries to return a matching property type.
     */
    fun getPropertyType(type: KSType?): PropertyType? {
        if (type == null) return null

        // Note: also check Java type names: class values of annotations (like the dbType of
        // @Convert) in Java sources are not mapped to their Kotlin equivalents.
        when (qualifiedName(type)) {
            "kotlin.Short", "java.lang.Short" -> return PropertyType.Short
            "kotlin.Int", "java.lang.Integer" -> return PropertyType.Int
            "kotlin.Long", "java.lang.Long" -> return PropertyType.Long
            "kotlin.Float", "java.lang.Float" -> return PropertyType.Float
            "kotlin.Double", "java.lang.Double" -> return PropertyType.Double
            "kotlin.Boolean", "java.lang.Boolean" -> return PropertyType.Boolean
            "kotlin.Byte", "java.lang.Byte" -> return PropertyType.Byte
            "java.util.Date" -> return PropertyType.Date
            "kotlin.Char", "java.lang.Character" -> return PropertyType.Char
            "kotlin.String", "java.lang.String" -> return PropertyType.String

            "kotlin.BooleanArray" -> return PropertyType.BooleanArray
            "kotlin.ByteArray" -> return PropertyType.ByteArray
            "kotlin.ShortArray" -> return PropertyType.ShortArray
            "kotlin.CharArray" -> return PropertyType.CharArray
            "kotlin.IntArray" -> return PropertyType.IntArray
            "kotlin.LongArray" -> return PropertyType.LongArray
            "kotlin.FloatArray" -> return PropertyType.FloatArray
            "kotlin.DoubleArray" -> return PropertyType.DoubleArray

            "kotlin.Array" -> {
                val argumentName = if (type.arguments.size == 1) {
                    qualifiedName(type.arguments[0].type?.resolve())
                } else null
                if (argumentName == "kotlin.String" || argumentName == "java.lang.String") {
                    return PropertyType.StringArray
                }
            }
        }

        if (isStringList(type)) {
            return PropertyType.StringArray
        }

        return null
    }

    /**
     * Returns the Java type name of this type like the annotation processor would see it in
     * KAPT-generated stubs: primitives for not-null Kotlin scalar types, boxed and built-in Java
     * types otherwise. Type parameters are left out (erasure), as callers only use this for
     * custom/converted types where the generator model does not support them.
     */
    fun toErasedJavaTypeName(type: KSType): String {
        val qualifiedName = qualifiedName(type) ?: return type.toString()
        if (type.nullability == Nullability.NOT_NULL) {
            KOTLIN_TO_JAVA_PRIMITIVE[qualifiedName]?.let { return it }
        }
        return KOTLIN_TO_JAVA_TYPES[qualifiedName] ?: qualifiedName
    }

    companion object {
        private val LIST_TYPES = setOf("kotlin.collections.List", "kotlin.collections.MutableList")
        private val MAP_TYPES = setOf("kotlin.collections.Map", "kotlin.collections.MutableMap")
        private val SCALAR_TYPES = setOf(
            "kotlin.Boolean", "kotlin.Byte", "kotlin.Short", "kotlin.Int", "kotlin.Long",
            "kotlin.Char", "kotlin.Float", "kotlin.Double"
        )

        private val KOTLIN_TO_JAVA_PRIMITIVE = mapOf(
            "kotlin.Boolean" to "boolean",
            "kotlin.Byte" to "byte",
            "kotlin.Short" to "short",
            "kotlin.Int" to "int",
            "kotlin.Long" to "long",
            "kotlin.Char" to "char",
            "kotlin.Float" to "float",
            "kotlin.Double" to "double"
        )

        private val KOTLIN_TO_JAVA_TYPES = mapOf(
            "kotlin.Boolean" to "java.lang.Boolean",
            "kotlin.Byte" to "java.lang.Byte",
            "kotlin.Short" to "java.lang.Short",
            "kotlin.Int" to "java.lang.Integer",
            "kotlin.Long" to "java.lang.Long",
            "kotlin.Char" to "java.lang.Character",
            "kotlin.Float" to "java.lang.Float",
            "kotlin.Double" to "java.lang.Double",
            "kotlin.String" to "java.lang.String",
            "kotlin.Any" to "java.lang.Object",
            "kotlin.collections.List" to "java.util.List",
            "kotlin.collections.MutableList" to "java.util.List",
            "kotlin.collections.Map" to "java.util.Map",
            "kotlin.collections.MutableMap" to "java.util.Map",
            "kotlin.collections.Set" to "java.util.Set",
            "kotlin.collections.MutableSet" to "java.util.Set"
        )
    }
}

/**
 * Compares two types like javac would compare the types in KAPT-generated stubs: by declaration,
 * primitive-ness (not-null scalar) and erased type arguments, ignoring nullability otherwise.
 */
internal fun KSType.isSameJavaTypeAs(other: KSType, typeHelper: KspTypeHelper): Boolean {
    if (typeHelper.qualifiedName(this) != typeHelper.qualifiedName(other)) return false
    if (typeHelper.isPrimitiveLike(this) != typeHelper.isPrimitiveLike(other)) return false
    if (arguments.size != other.arguments.size) return false
    for (i in arguments.indices) {
        val thisArg = arguments[i].type?.resolve()
        val otherArg = other.arguments[i].type?.resolve()
        if (thisArg == null || otherArg == null) {
            if (thisArg !== otherArg) return false
            continue
        }
        if (typeHelper.qualifiedName(thisArg) != typeHelper.qualifiedName(otherArg)) return false
    }
    return true
}
