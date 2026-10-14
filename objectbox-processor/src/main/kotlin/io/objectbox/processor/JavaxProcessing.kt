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

import io.objectbox.generator.model.PropertyType
import java.util.*
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * [ProcessingClass] and friends implemented with the annotation processing API
 * (javax.lang.model).
 */
class JavaxProcessing(
    private val elementUtils: Elements,
    private val typeUtils: Types
) {

    private val typeHelper = TypeHelper(elementUtils, typeUtils)

    fun wrap(element: Element): ProcessingClass = JavaxClass(element)

    private abstract inner class JavaxAnnotated(private val annotatedElement: Element) : ProcessingAnnotated {

        override fun hasAnnotation(annotationClass: Class<out Annotation>): Boolean {
            return annotationInstanceOf(annotationClass) != null
        }

        override fun stringValueOf(annotationClass: Class<out Annotation>, member: String): String? {
            return annotationValueOf(annotationClass, member) as? String
        }

        override fun longValueOf(annotationClass: Class<out Annotation>, member: String): Long? {
            return (annotationValueOf(annotationClass, member) as? Number)?.toLong()
        }

        override fun booleanValueOf(annotationClass: Class<out Annotation>, member: String): Boolean? {
            return annotationValueOf(annotationClass, member) as? Boolean
        }

        override fun enumValueNameOf(annotationClass: Class<out Annotation>, member: String): String? {
            // Enum values are represented as the VariableElement of the enum constant.
            return (annotationValueOf(annotationClass, member) as? VariableElement)?.simpleName?.toString()
        }

        override fun typeValueOf(annotationClass: Class<out Annotation>, member: String): ProcessingType? {
            // Class values are represented as their TypeMirror (annotationClass.value would throw).
            return (annotationValueOf(annotationClass, member) as? TypeMirror)?.let { JavaxType(it) }
        }

        override fun <A : Annotation> annotationInstanceOf(annotationClass: Class<A>): A? {
            return annotatedElement.getAnnotation(annotationClass)
        }

        private fun annotationMirrorOf(annotationClass: Class<out Annotation>): AnnotationMirror? {
            return annotatedElement.annotationMirrors.firstOrNull {
                (it.annotationType.asElement() as TypeElement).qualifiedName
                    .contentEquals(annotationClass.canonicalName)
            }
        }

        private fun annotationValueOf(annotationClass: Class<out Annotation>, member: String): Any? {
            val annotationMirror = annotationMirrorOf(annotationClass) ?: return null
            return elementUtils.getElementValuesWithDefaults(annotationMirror).entries
                .firstOrNull { it.key.simpleName.contentEquals(member) }
                ?.value?.value
        }
    }

    private inner class JavaxClass(override val element: Element) : JavaxAnnotated(element), ProcessingClass {

        override val simpleName: String
            get() = element.simpleName.toString()

        override val qualifiedName: String?
            get() = (element as? TypeElement)?.qualifiedName?.toString()

        override val packageName: String
            get() = elementUtils.getPackageOf(element).qualifiedName.toString()

        override val isClass: Boolean
            get() = element.kind == ElementKind.CLASS

        override fun declaredFields(): List<ProcessingField> {
            val methods = ElementFilter.methodsIn(element.enclosedElements)
            return ElementFilter.fieldsIn(element.enclosedElements).map { JavaxField(it, methods) }
        }

        override fun constructors(): List<ProcessingConstructor> {
            return ElementFilter.constructorsIn(element.enclosedElements).map { JavaxConstructor(it) }
        }

        override fun superTypes(): List<ProcessingClass> {
            // Note: Why can there be multiple super types? Because interfaces are considered super types.
            return typeUtils.directSupertypes(element.asType())
                .mapNotNull { (it as? DeclaredType)?.asElement() }
                .filter {
                    // Skip if the top-most type (java.lang.Object, also for interfaces) is reached.
                    !((it as? TypeElement)?.qualifiedName?.contentEquals("java.lang.Object") ?: false)
                }
                .map { JavaxClass(it) }
        }
    }

    private inner class JavaxField(
        private val fieldElement: VariableElement,
        private val methodsOfClass: List<ExecutableElement>
    ) : JavaxAnnotated(fieldElement), ProcessingField {

        override val element: Any
            get() = fieldElement

        override val name: String
            get() = fieldElement.simpleName.toString()

        override val type: ProcessingType by lazy { JavaxType(fieldElement.asType()) }

        override val enclosingQualifiedName: String?
            get() = (fieldElement.enclosingElement as? TypeElement)?.qualifiedName?.toString()

        override val isStatic: Boolean
            get() = fieldElement.modifiers.contains(Modifier.STATIC)

        override val isTransient: Boolean
            get() = fieldElement.modifiers.contains(Modifier.TRANSIENT)

        override val isKotlin: Boolean
            get() = false

        override val isDelegated: Boolean
            get() = false

        override val hasBackingField: Boolean
            get() = true

        override val isAccessibleField: Boolean
            get() = !fieldElement.modifiers.contains(Modifier.PRIVATE)

        override fun getterNameOrNull(propertyType: PropertyType?): String? {
            val fieldType = fieldElement.asType()
            val propertyName = name
            val propertyNameCapitalized = propertyName.replaceFirstChar { it.titlecase(Locale.getDefault()) }

            // https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#properties
            // Kotlin: 'isProperty' (but not 'isproperty').
            if (propertyName.startsWith("is") && propertyName.length > 2 && propertyName[2].isUpperCase()) {
                methodsOfClass.find {
                    it.simpleName.toString() == propertyName && typeUtils.isSameType(it.returnType, fieldType)
                }?.let {
                    return it.simpleName.toString() // Getter is called 'isProperty' (setter 'setProperty').
                }
            }

            // https://docs.oracle.com/javase/tutorial/javabeans/writing/properties.html
            // Java: 'isProperty' for booleans (JavaBeans spec).
            if (propertyType == PropertyType.Boolean) {
                methodsOfClass.find {
                    it.simpleName.toString() == "is$propertyNameCapitalized"
                            && typeUtils.isSameType(it.returnType, fieldType)
                }?.let {
                    return it.simpleName.toString() // Getter is called 'isPropertyName'.
                }
            }

            // At last, check for regular getter.
            return methodsOfClass.find {
                it.simpleName.toString() == "get$propertyNameCapitalized"
                        && typeUtils.isSameType(it.returnType, fieldType)
            }?.simpleName?.toString()
        }
    }

    private inner class JavaxConstructor(private val constructor: ExecutableElement) : ProcessingConstructor {

        override val description: String
            get() = constructor.toString()

        override val parameters: List<ProcessingParameter>
            get() = constructor.parameters.map { JavaxParameter(it) }
    }

    private inner class JavaxParameter(private val parameter: VariableElement) : ProcessingParameter {

        override val name: String
            get() = parameter.simpleName.toString()

        override val type: ProcessingType
            get() = JavaxType(parameter.asType())

        override val hasDefault: Boolean
            get() = false
    }

    private inner class JavaxType(private val typeMirror: TypeMirror) : ProcessingType {

        override val isPrimitiveLike: Boolean
            get() = typeMirror.kind.isPrimitive

        override fun propertyTypeOrNull(): PropertyType? = typeHelper.getPropertyType(typeMirror)

        override val isToOne: Boolean get() = typeHelper.isToOne(typeMirror)
        override val isToMany: Boolean get() = typeHelper.isToMany(typeMirror)
        override val isList: Boolean get() = typeHelper.isList(typeMirror)
        override val isStringList: Boolean get() = typeHelper.isStringList(typeMirror)
        override val isStringMap: Boolean get() = typeHelper.isStringMap(typeMirror)
        override val isStringLongMap: Boolean get() = typeHelper.isStringLongMap(typeMirror)
        override val isStringStringMap: Boolean get() = typeHelper.isStringStringMap(typeMirror)
        override val isIntegerMap: Boolean get() = typeHelper.isIntegerMap(typeMirror)
        override val isIntegerLongMap: Boolean get() = typeHelper.isIntegerLongMap(typeMirror)
        override val isLongMap: Boolean get() = typeHelper.isLongMap(typeMirror)
        override val isLongLongMap: Boolean get() = typeHelper.isLongLongMap(typeMirror)
        override val isObject: Boolean get() = typeHelper.isObject(typeMirror)

        override fun erasedJavaTypeName(): String {
            return typeUtils.erasure(typeMirror).toString()
        }

        override fun typeNameForCode(): String {
            return typeMirror.toString()
        }

        override fun isSameJavaTypeAs(other: ProcessingType): Boolean {
            return other is JavaxType && typeUtils.isSameType(typeMirror, other.typeMirror)
        }

        override val typeArgumentCount: Int
            get() = (typeMirror as? DeclaredType)?.typeArguments?.size ?: 0

        override fun relationTargetClassName(): String? {
            val targetTypeMirror = (typeMirror as? DeclaredType)?.typeArguments?.firstOrNull() ?: return null
            return if (targetTypeMirror is DeclaredType) {
                // can simply get as element as code would not have compiled if target type is not known
                targetTypeMirror.asElement().simpleName.toString()
            } else {
                null
            }
        }

        override fun relationTargetDisplayName(): String {
            return (typeMirror as? DeclaredType)?.typeArguments?.firstOrNull()?.toString() ?: typeMirror.toString()
        }

        override fun toString(): String = typeMirror.toString()
    }

}
