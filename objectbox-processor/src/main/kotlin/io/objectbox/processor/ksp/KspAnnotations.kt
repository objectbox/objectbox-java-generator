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

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Origin

/**
 * True if this is parsed from Kotlin (sources or libraries), false if from Java.
 */
internal fun KSDeclaration.isKotlinOrigin(): Boolean {
    return origin == Origin.KOTLIN || origin == Origin.KOTLIN_LIB
}

/**
 * Returns the annotation of the given class on this node, or null. See [isAnnotation].
 */
internal fun KSAnnotated.annotation(annotationClass: Class<out Annotation>): KSAnnotation? {
    return annotations.firstOrNull { it.isAnnotation(annotationClass) }
}

internal fun KSAnnotated.hasAnnotation(annotationClass: Class<out Annotation>): Boolean {
    return annotation(annotationClass) != null
}

/**
 * True if this is an annotation of the given class. Matches by simple name first to avoid
 * resolving the type of unrelated annotations.
 */
internal fun KSAnnotation.isAnnotation(annotationClass: Class<out Annotation>): Boolean {
    return shortName.asString() == annotationClass.simpleName
            && annotationType.resolve().declaration.qualifiedName?.asString() == annotationClass.canonicalName
}

/**
 * Returns the value of the argument with the given name, including default values.
 */
internal fun KSAnnotation.argumentValue(name: String): Any? {
    return arguments.firstOrNull { it.name?.asString() == name }?.value
}

internal fun KSAnnotation.stringArgument(name: String): String? = argumentValue(name) as? String

internal fun KSAnnotation.longArgument(name: String): Long? = (argumentValue(name) as? Number)?.toLong()

internal fun KSAnnotation.booleanArgument(name: String): Boolean? = argumentValue(name) as? Boolean

/**
 * Returns the simple name of an enum entry argument. KSP may represent enum values as the type or
 * the declaration of the entry, depending on origin.
 */
internal fun KSAnnotation.enumArgumentName(name: String): String? {
    return when (val value = argumentValue(name)) {
        is KSType -> value.declaration.simpleName.asString()
        is KSClassDeclaration -> value.simpleName.asString()
        else -> value?.toString()?.substringAfterLast('.')
    }
}

/**
 * Returns the [KSType] of a `KClass` valued argument (like `converter` of `@Convert`).
 */
internal fun KSAnnotation.typeArgument(name: String): KSType? = argumentValue(name) as? KSType
