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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Nullability
import io.objectbox.generator.model.PropertyType
import io.objectbox.processor.ProcessingAnnotated
import io.objectbox.processor.ProcessingClass
import io.objectbox.processor.ProcessingConstructor
import io.objectbox.processor.ProcessingField
import io.objectbox.processor.ProcessingParameter
import io.objectbox.processor.ProcessingType
import java.util.*

/**
 * [ProcessingClass] and friends implemented with the KSP API, providing the Java view of Kotlin
 * declarations (like the annotation processor sees in KAPT-generated stubs of Kotlin sources).
 */
class KspProcessing {

    private val typeHelper = KspTypeHelper()

    fun wrap(declaration: KSClassDeclaration): ProcessingClass = KspClass(declaration)

    private abstract class KspAnnotated(
        private val annotated: com.google.devtools.ksp.symbol.KSAnnotated
    ) : ProcessingAnnotated {

        override fun hasAnnotation(annotationClass: Class<out Annotation>): Boolean {
            return annotated.hasAnnotation(annotationClass)
        }

        override fun stringValueOf(annotationClass: Class<out Annotation>, member: String): String? {
            return annotated.annotation(annotationClass)?.stringArgument(member)
        }

        override fun longValueOf(annotationClass: Class<out Annotation>, member: String): Long? {
            return annotated.annotation(annotationClass)?.longArgument(member)
        }

        override fun booleanValueOf(annotationClass: Class<out Annotation>, member: String): Boolean? {
            return annotated.annotation(annotationClass)?.booleanArgument(member)
        }

        override fun enumValueNameOf(annotationClass: Class<out Annotation>, member: String): String? {
            return annotated.annotation(annotationClass)?.enumArgumentName(member)
        }

        abstract fun wrapType(type: KSType): ProcessingType

        override fun typeValueOf(annotationClass: Class<out Annotation>, member: String): ProcessingType? {
            return annotated.annotation(annotationClass)?.typeArgument(member)?.let { wrapType(it) }
        }

        @OptIn(KspExperimental::class)
        override fun <A : Annotation> annotationInstanceOf(annotationClass: Class<A>): A? {
            return annotated.getAnnotationsByType(annotationClass.kotlin).firstOrNull()
        }
    }

    private inner class KspClass(private val declaration: KSClassDeclaration) :
        KspAnnotated(declaration), ProcessingClass {

        override fun wrapType(type: KSType): ProcessingType = KspType(type)

        override val element: Any
            get() = declaration

        override val simpleName: String
            get() = declaration.simpleName.asString()

        override val qualifiedName: String?
            get() = declaration.qualifiedName?.asString()

        override val packageName: String
            get() = declaration.packageName.asString()

        override val isClass: Boolean
            get() = declaration.classKind == ClassKind.CLASS

        override fun declaredFields(): List<ProcessingField> {
            val functions = declaration.getDeclaredFunctions().filter { !it.isConstructor() }.toList()
            return declaration.getDeclaredProperties()
                // Like KAPT-generated stubs, leave out Kotlin properties that do not compile to a
                // field (like `val x get() = ...`). But keep delegated properties (they compile to a
                // field of the delegate type), to report them as not supported.
                .filter { !it.isKotlinOrigin() || it.hasBackingField || it.isDelegated() }
                .map { KspField(it, functions) }
                .toList()
        }

        override fun constructors(): List<ProcessingConstructor> {
            return declaration.getConstructors().map { KspConstructor(it) }.toList()
        }

        override fun superTypes(): List<ProcessingClass> {
            return declaration.superTypes
                .mapNotNull { it.resolve().declaration as? KSClassDeclaration }
                .filter {
                    // Skip if the top-most type (also for interfaces) is reached.
                    val superName = it.qualifiedName?.asString()
                    superName != null && superName != "kotlin.Any" && superName != "java.lang.Object"
                }
                .map { KspClass(it) }
                .toList()
        }
    }

    private inner class KspField(
        private val property: KSPropertyDeclaration,
        private val functionsOfClass: List<KSFunctionDeclaration>
    ) : KspAnnotated(property), ProcessingField {

        override fun wrapType(type: KSType): ProcessingType = KspType(type)

        override val element: Any
            get() = property

        override val name: String
            get() = property.simpleName.asString()

        /** The declared type, for Kotlin properties of inline value class types the value class. */
        private val declaredType: KSType by lazy { property.type.resolve() }

        /**
         * The Java view of the type: like for KAPT-generated stubs, inline value classes are
         * replaced by their underlying type (like `byte` for `UByte`).
         *
         * Note: in contrast, constructor parameters keep their value class type. Constructors with
         * value class parameters are mangled and not callable from Java, so they never match the
         * properties as an all-args constructor.
         */
        override val type: ProcessingType by lazy { KspType(typeHelper.toJavaViewType(declaredType)) }

        override val enclosingQualifiedName: String?
            get() = (property.parentDeclaration as? KSClassDeclaration)?.qualifiedName?.asString()

        override val isStatic: Boolean
            get() = property.modifiers.contains(Modifier.JAVA_STATIC)

        override val isTransient: Boolean
            get() = property.modifiers.contains(Modifier.JAVA_TRANSIENT)
                    // A Kotlin property annotated @Transient compiles to a transient field.
                    || property.hasAnnotation(Transient::class.java)

        /** If this is parsed from Kotlin sources (or libraries). */
        private val isKotlin: Boolean
            get() = property.isKotlinOrigin()

        override val isAccessibleField: Boolean
            get() = if (isKotlin) {
                // Like KAPT-generated stubs, where the backing field is private, unless the
                // property is exposed as a field (lateinit or @JvmField).
                !property.modifiers.contains(Modifier.PRIVATE)
                        && (property.modifiers.contains(Modifier.LATEINIT)
                        || property.hasAnnotation(JvmField::class.java))
            } else {
                !property.modifiers.contains(Modifier.PRIVATE)
            }

        override fun getterNameOrNull(propertyType: PropertyType?): String? {
            val propertyName = name
            val propertyNameCapitalized = propertyName.replaceFirstChar { it.titlecase(Locale.getDefault()) }

            if (isKotlin) {
                if (property.hasAnnotation(JvmField::class.java)) {
                    return null // no accessors generated
                }
                getterJvmNameOrNull()?.let { return it }
                // https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#properties
                return if (propertyName.startsWith("is") && propertyName.length > 2 && propertyName[2].isUpperCase()) {
                    propertyName // Getter is called 'isProperty' (setter 'setProperty').
                } else {
                    "get$propertyNameCapitalized"
                }
            }

            val fieldType = property.type.resolve()

            // https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#properties
            // Kotlin style: 'isProperty' (but not 'isproperty').
            if (propertyName.startsWith("is") && propertyName.length > 2 && propertyName[2].isUpperCase()) {
                functionsOfClass.find {
                    it.simpleName.asString() == propertyName && it.returnsType(fieldType)
                }?.let {
                    return it.simpleName.asString() // Getter is called 'isProperty' (setter 'setProperty').
                }
            }

            // https://docs.oracle.com/javase/tutorial/javabeans/writing/properties.html
            // Java: 'isProperty' for booleans (JavaBeans spec).
            if (propertyType == PropertyType.Boolean) {
                functionsOfClass.find {
                    it.simpleName.asString() == "is$propertyNameCapitalized" && it.returnsType(fieldType)
                }?.let {
                    return it.simpleName.asString() // Getter is called 'isPropertyName'.
                }
            }

            // At last, check for regular getter.
            return functionsOfClass.find {
                it.simpleName.asString() == "get$propertyNameCapitalized" && it.returnsType(fieldType)
            }?.simpleName?.asString()
        }

        override val unsupportedReason: String?
            get() {
                if (!isKotlin) return null
                // When using KAPT, the processor already fails on a delegated property as it is
                // compiled to a field of the delegate type (like 'name$delegate').
                // However, KSP sees only the property itself (like 'name'), so add an explicit check.
                if (property.isDelegated()) {
                    return "Delegated properties are not supported, use @Transient or a regular property."
                }
                if (typeHelper.isValueClass(declaredType)) {
                    if (declaredType.nullability != Nullability.NOT_NULL) {
                        return "Nullable inline value class types are not supported, use a not-null type."
                    }
                    // The accessors of properties of value class types get mangled names that are not
                    // callable from Java, unless renamed with @JvmName.
                    if (!isAccessibleField && getterJvmNameOrNull() == null) {
                        val propertyNameCapitalized = name.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                        return "The getter of a property with an inline value class type has a mangled name, rename it" +
                                " like @get:JvmName(\"get$propertyNameCapitalized\")."
                    }
                }
                return null
            }

        /**
         * The name of the getter if renamed with `@get:JvmName`, or null.
         */
        private fun getterJvmNameOrNull(): String? {
            val jvmName = property.getter?.annotation(JvmName::class.java)
                ?: property.annotations.firstOrNull {
                    it.useSiteTarget == AnnotationUseSiteTarget.GET && it.isAnnotation(JvmName::class.java)
                }
            return jvmName?.stringArgument(JvmName::name.name)
        }

        /**
         * Like the annotation processor compares the (stub) return type with the field type,
         * compare the Java view of both types.
         */
        private fun KSFunctionDeclaration.returnsType(fieldType: KSType): Boolean {
            val returnType = this.returnType?.resolve() ?: return false
            return returnType.isSameJavaTypeAs(fieldType, typeHelper)
        }
    }

    private inner class KspConstructor(private val constructor: KSFunctionDeclaration) : ProcessingConstructor {

        override val description: String
            get() {
                val className = (constructor.parentDeclaration as? KSClassDeclaration)?.simpleName?.asString() ?: ""
                return "$className(${constructor.parameters.joinToString(",") { it.type.toString() }})"
            }

        override val parameters: List<ProcessingParameter>
            get() = constructor.parameters.map { KspParameter(it) }
    }

    private inner class KspParameter(private val parameter: KSValueParameter) : ProcessingParameter {

        override val name: String?
            get() = parameter.name?.asString()

        override val type: ProcessingType
            get() = KspType(parameter.type.resolve())

        override val hasDefault: Boolean
            get() = parameter.hasDefault
    }

    private inner class KspType(private val type: KSType) : ProcessingType {

        override val isPrimitiveLike: Boolean
            get() = typeHelper.isPrimitiveLike(type)

        override fun propertyTypeOrNull(): PropertyType? = typeHelper.getPropertyType(type)

        override val isToOne: Boolean get() = typeHelper.isToOne(type)
        override val isToMany: Boolean get() = typeHelper.isToMany(type)
        override val isList: Boolean get() = typeHelper.isList(type)
        override val isStringList: Boolean get() = typeHelper.isStringList(type)
        override val isStringMap: Boolean get() = typeHelper.isStringMap(type)
        override val isStringLongMap: Boolean get() = typeHelper.isStringLongMap(type)
        override val isStringStringMap: Boolean get() = typeHelper.isStringStringMap(type)
        override val isIntegerMap: Boolean get() = typeHelper.isIntegerMap(type)
        override val isIntegerLongMap: Boolean get() = typeHelper.isIntegerLongMap(type)
        override val isLongMap: Boolean get() = typeHelper.isLongMap(type)
        override val isLongLongMap: Boolean get() = typeHelper.isLongLongMap(type)
        override val isObject: Boolean get() = typeHelper.isObject(type)

        override fun erasedJavaTypeName(): String {
            return typeHelper.toErasedJavaTypeName(type)
        }

        override fun typeNameForCode(): String {
            return typeHelper.qualifiedName(type) ?: type.toString()
        }

        override fun isSameJavaTypeAs(other: ProcessingType): Boolean {
            return other is KspType && type.isSameJavaTypeAs(other.type, typeHelper)
        }

        override val typeArgumentCount: Int
            get() = type.arguments.size

        override fun relationTargetClassName(): String? {
            val targetDeclaration = type.arguments.firstOrNull()?.type?.resolve()?.declaration
            return (targetDeclaration as? KSClassDeclaration)?.simpleName?.asString()
        }

        override fun relationTargetDisplayName(): String {
            val targetDeclaration = type.arguments.firstOrNull()?.type?.resolve()?.declaration
            return targetDeclaration?.simpleName?.asString()
                ?: type.arguments.firstOrNull()?.toString() ?: type.toString()
        }

        override fun toString(): String {
            // A readable Java-like type name for error messages.
            val erased = typeHelper.toErasedJavaTypeName(type)
            if (type.arguments.isEmpty()) return erased
            return erased + type.arguments.joinToString(",", prefix = "<", postfix = ">") {
                it.type?.resolve()?.let { argType -> typeHelper.toErasedJavaTypeName(argType) } ?: it.toString()
            }
        }
    }

}
