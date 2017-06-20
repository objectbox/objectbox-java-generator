package io.objectbox.processor

import io.objectbox.generator.model.PropertyType
import java.util.*
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

/**
 * Helps translate processor types to objectbox types.
 */
class TypeHelper(val typeUtils: Types) {

    /**
     * Checks if the type name is equal to the given type name.
     */
    fun isTypeEqualTo(typeMirror: TypeMirror, otherType: String, eraseTypeParameters: Boolean = false): Boolean {
        if (eraseTypeParameters) {
            return otherType == typeUtils.erasure(typeMirror).toString()
        } else {
            return otherType == typeMirror.toString()
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
        if (isTypeEqualTo(typeMirror, java.lang.Short::class.java.name) || kind == TypeKind.SHORT) {
            return PropertyType.Short
        }
        if (isTypeEqualTo(typeMirror, java.lang.Integer::class.java.name) || kind == TypeKind.INT) {
            return PropertyType.Int
        }
        if (isTypeEqualTo(typeMirror, java.lang.Long::class.java.name) || kind == TypeKind.LONG) {
            return PropertyType.Long
        }

        if (isTypeEqualTo(typeMirror, java.lang.Float::class.java.name) || kind == TypeKind.FLOAT) {
            return PropertyType.Float
        }
        if (isTypeEqualTo(typeMirror, java.lang.Double::class.java.name) || kind == TypeKind.DOUBLE) {
            return PropertyType.Double
        }

        if (isTypeEqualTo(typeMirror, java.lang.Boolean::class.java.name) || kind == TypeKind.BOOLEAN) {
            return PropertyType.Boolean
        }
        if (isTypeEqualTo(typeMirror, java.lang.Byte::class.java.name) || kind == TypeKind.BYTE) {
            return PropertyType.Byte
        }
        if (isTypeEqualTo(typeMirror, Date::class.java.name)) {
            return PropertyType.Date
        }
        if (isTypeEqualTo(typeMirror, java.lang.String::class.java.name)) {
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
}
