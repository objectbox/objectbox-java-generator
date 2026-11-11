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

package io.objectbox.processor

import io.objectbox.annotation.Uid
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.model.PropertyType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Abstraction over the classes, fields and types of the annotation processing and KSP APIs:
 * shared parsing logic (like [Properties], [Relations] and the entity parsing of
 * [ObjectBoxProcessorCore]) works on these, the API specific implementations provide the
 * primitive operations.
 *
 * Note: to produce identical output regardless of the processor API and source language, the
 * implementations provide the Java view of the code: like the annotation processor sees Java
 * sources or the KAPT-generated Java stubs of Kotlin sources.
 *
 * To read annotation values, prefer the extension functions taking a member reference, like
 * `stringValueOf(Backlink::to)`, over passing the annotation class and member name.
 */
interface ProcessingAnnotated {

    fun hasAnnotation(annotationClass: Class<out Annotation>): Boolean

    fun stringValueOf(annotationClass: Class<out Annotation>, member: String): String?

    fun longValueOf(annotationClass: Class<out Annotation>, member: String): Long?

    fun booleanValueOf(annotationClass: Class<out Annotation>, member: String): Boolean?

    /**
     * The simple name of an enum value, like "REPLACE" of `ConflictStrategy.REPLACE`.
     */
    fun enumValueNameOf(annotationClass: Class<out Annotation>, member: String): String?

    /**
     * The type of a class value, like the `converter` of `@Convert`.
     */
    fun typeValueOf(annotationClass: Class<out Annotation>, member: String): ProcessingType?

    /**
     * An actual instance of the annotation, for passing along annotations with many members
     * (does not support class values).
     */
    fun <A : Annotation> annotationInstanceOf(annotationClass: Class<A>): A?

}

// Typed variants of the ProcessingAnnotated value functions: a member reference like
// Backlink::to provides the annotation class and the member name (the compiler replaces .name of a
// callable reference with a constant, so this does not need kotlin-reflect) and checks the value
// type at compile time.

inline fun <reified A : Annotation> ProcessingAnnotated.stringValueOf(member: KProperty1<A, String>): String? =
    stringValueOf(A::class.java, member.name)

inline fun <reified A : Annotation> ProcessingAnnotated.longValueOf(member: KProperty1<A, Long>): Long? =
    longValueOf(A::class.java, member.name)

inline fun <reified A : Annotation> ProcessingAnnotated.booleanValueOf(member: KProperty1<A, Boolean>): Boolean? =
    booleanValueOf(A::class.java, member.name)

/**
 * See [ProcessingAnnotated.enumValueNameOf].
 */
inline fun <reified A : Annotation> ProcessingAnnotated.enumValueNameOf(member: KProperty1<A, Enum<*>>): String? =
    enumValueNameOf(A::class.java, member.name)

/**
 * See [ProcessingAnnotated.typeValueOf].
 */
inline fun <reified A : Annotation> ProcessingAnnotated.typeValueOf(
    member: KProperty1<A, KClass<*>>
): ProcessingType? = typeValueOf(A::class.java, member.name)

/**
 * The value of [Uid], mapping the default value 0 (`@Uid` without a value) to [IdSync.UID_PRINT],
 * so IdSync prints instructions to rename or reset the UID.
 */
fun ProcessingAnnotated.uidValue(): Long? =
    longValueOf(Uid::value)?.let { if (it == 0L) IdSync.UID_PRINT else it }

/**
 * A class declaration. See [ProcessingAnnotated].
 */
interface ProcessingClass : ProcessingAnnotated {

    /** The underlying element of the processor API, to link error messages to. */
    val element: Any

    val simpleName: String

    val qualifiedName: String?

    val packageName: String

    /** If this is a class (and not an interface or other type of declaration). */
    val isClass: Boolean

    /**
     * The fields declared by this class. For Kotlin sources only properties that compile to a field
     * (so not like `val x get() = ...`), and delegated properties (to report them as unsupported).
     */
    fun declaredFields(): List<ProcessingField>

    fun constructors(): List<ProcessingConstructor>

    /**
     * The declared super types (classes and interfaces, excluding the top-most type
     * `java.lang.Object`/`kotlin.Any`).
     */
    fun superTypes(): List<ProcessingClass>

}

/**
 * A field of a class; for Kotlin sources a property (as it compiles to a field with accessors).
 * See [ProcessingAnnotated].
 */
interface ProcessingField : ProcessingAnnotated {

    /** The underlying element of the processor API, to link error messages to. */
    val element: Any

    val name: String

    val type: ProcessingType

    val enclosingQualifiedName: String?

    val isStatic: Boolean

    /** Has the Java transient modifier (also set for Kotlin properties annotated @Transient). */
    val isTransient: Boolean

    /**
     * If the (compiled) field can be accessed directly: it is not private and, for Kotlin
     * properties, is exposed as a field (lateinit or @JvmField).
     */
    val isAccessibleField: Boolean

    /**
     * Tries to find a getter method name for this field that returns its type. Returns null if
     * there is none (generated code falls back to expecting a regular getter or field access).
     *
     * Prefers `isPropertyName` over `getPropertyName` if the name starts with 'is' followed by an
     * uppercase letter, or for boolean properties (JavaBeans spec). For Kotlin properties the
     * accessors generated by the compiler follow the same rules.
     */
    fun getterNameOrNull(propertyType: PropertyType?): String?

    /**
     * If this field can not be used as a property because of how it compiles (like a Kotlin
     * delegated property, or a property of an inline value class type without a getter callable
     * from Java), describes why. Otherwise null.
     */
    val unsupportedReason: String?

}

/**
 * A constructor of a class.
 */
interface ProcessingConstructor {

    /** For debug output, like `Example(long,java.lang.String)`. */
    val description: String

    val parameters: List<ProcessingParameter>

}

/**
 * A parameter of a constructor.
 */
interface ProcessingParameter {

    val name: String?

    val type: ProcessingType

    /** If this parameter has a default value (Kotlin only). */
    val hasDefault: Boolean

}

/**
 * The type of a field or constructor parameter. See [ProcessingApi].
 */
interface ProcessingType {

    /** If this type compiles to a Java primitive. */
    val isPrimitiveLike: Boolean

    /**
     * The property type this maps to, or null if it is not directly supported by the database.
     */
    fun propertyTypeOrNull(): PropertyType?

    val isToOne: Boolean
    val isToMany: Boolean
    val isList: Boolean
    val isStringList: Boolean
    val isStringMap: Boolean
    val isStringLongMap: Boolean
    val isStringStringMap: Boolean
    val isIntegerMap: Boolean
    val isIntegerLongMap: Boolean
    val isLongMap: Boolean
    val isLongLongMap: Boolean
    val isObject: Boolean

    /**
     * The Java type name without type parameters, primitives as primitives, like `int` or
     * `java.util.List` (used for custom/converted types, which do not support type parameters).
     */
    fun erasedJavaTypeName(): String

    /**
     * The Java type name to reference this type in generated code, like the fully qualified name
     * of a converter class.
     */
    fun typeNameForCode(): String

    /**
     * Compares like javac compares the (stub) types: by their Java view, ignoring Kotlin
     * nullability (but not boxing).
     */
    fun isSameJavaTypeAs(other: ProcessingType): Boolean

    val typeArgumentCount: Int

    /**
     * The simple name of the class the first type argument points to, or null if it is not a
     * class (like a type parameter `T`). For relation properties, like `ToOne<Target>`.
     */
    fun relationTargetClassName(): String?

    /**
     * How the first type argument is displayed in error messages, like `T`.
     */
    fun relationTargetDisplayName(): String

    /** How this type is displayed in error messages. */
    override fun toString(): String

}
