package io.objectbox.processor

import io.objectbox.generator.model.PropertyType
import java.util.*
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types
import javax.tools.Diagnostic

val VariableElement.qualifiedName: String
    get() {
        val enclosingElement = enclosingElement as TypeElement
        val fieldName = simpleName
        return "${enclosingElement.qualifiedName}.$fieldName"
    }

fun <A : Annotation> Element.hasAnnotation(annotationType: Class<A>): Boolean {
    return getAnnotation(annotationType) != null
}

/**
 * Checks if the type name is equal to the given type name.
 */
fun TypeMirror.isTypeEqualTo(typeUtils: Types, otherType: String, eraseTypeParameters: Boolean = false): Boolean {
    if (eraseTypeParameters) {
        return otherType == typeUtils.erasure(this).toString()
    } else {
        return otherType == toString()
    }
}

/**
 * Tries to return a matching property type.
 */
fun TypeMirror.getPropertyType(typeUtils: Types): PropertyType? {
    // also handles Kotlin types as they are mapped to Java primitive (wrapper) types at compile time
    if (isTypeEqualTo(typeUtils, java.lang.Short::class.java.name) || kind == TypeKind.SHORT) {
        return PropertyType.Short
    }
    if (isTypeEqualTo(typeUtils, java.lang.Integer::class.java.name) || kind == TypeKind.INT) {
        return PropertyType.Int
    }
    if (isTypeEqualTo(typeUtils, java.lang.Long::class.java.name) || kind == TypeKind.LONG) {
        return PropertyType.Long
    }

    if (isTypeEqualTo(typeUtils, java.lang.Float::class.java.name) || kind == TypeKind.FLOAT) {
        return PropertyType.Float
    }
    if (isTypeEqualTo(typeUtils, java.lang.Double::class.java.name) || kind == TypeKind.DOUBLE) {
        return PropertyType.Double
    }

    if (isTypeEqualTo(typeUtils, java.lang.Boolean::class.java.name) || kind == TypeKind.BOOLEAN) {
        return PropertyType.Boolean
    }
    if (isTypeEqualTo(typeUtils, java.lang.Byte::class.java.name) || kind == TypeKind.BYTE) {
        return PropertyType.Byte
    }
    if (isTypeEqualTo(typeUtils, Date::class.java.name)) {
        return PropertyType.Date
    }
    if (isTypeEqualTo(typeUtils, java.lang.String::class.java.name)) {
        return PropertyType.String
    }

    if (kind == TypeKind.ARRAY) {
        val arrayType = this as ArrayType
        if (arrayType.componentType.kind == TypeKind.BYTE) {
            return PropertyType.ByteArray
        }
    }

    return null
}
