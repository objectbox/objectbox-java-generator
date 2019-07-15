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

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.IndexType
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.Transient
import io.objectbox.annotation.Uid
import io.objectbox.annotation.Unique
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType
import io.objectbox.model.PropertyFlags
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

/**
 * Parses properties from fields for a given entity and adds them to the entity model.
 */
class Properties(val elementUtils: Elements, val typeUtils: Types, val messages: Messages,
                 val relations: Relations, val entityModel: Entity, entityElement: Element) {

    val typeHelper = TypeHelper(typeUtils)

    val fields: List<VariableElement> = ElementFilter.fieldsIn(entityElement.enclosedElements)
    val methods: List<ExecutableElement> = ElementFilter.methodsIn(entityElement.enclosedElements)

    fun hasBoxStoreField(): Boolean {
        return fields.find { it.simpleName.toString() == "__boxStore" } != null
    }

    fun parseFields() {
        for (field in fields) {
            parseField(field)
        }
    }

    private fun parseField(field: VariableElement) {
        // ignore static, transient or @Transient fields
        val modifiers = field.modifiers
        if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.TRANSIENT)
                || field.hasAnnotation(Transient::class.java)) {
            return
        }

        if (typeHelper.isTypeEqualTo(field.asType(), ToOne::class.java.name, eraseTypeParameters = true)) {
            // ToOne<TARGET> property
            errorIfIndexOrUniqueAnnotation(field, "ToOne")
            relations.parseToOne(entityModel, field)
        } else if (!field.hasAnnotation(Convert::class.java)
                && typeHelper.isTypeEqualTo(field.asType(), List::class.java.name, eraseTypeParameters = true)) {
            // List<TARGET> property
            errorIfIndexOrUniqueAnnotation(field, "List")
            relations.parseToMany(entityModel, field)
        } else if (typeHelper.isTypeEqualTo(field.asType(), ToMany::class.java.name, eraseTypeParameters = true)) {
            // ToMany<TARGET> property
            errorIfIndexOrUniqueAnnotation(field, "ToMany")
            relations.parseToMany(entityModel, field)
        } else {
            // regular property
            parseProperty(field)
        }
    }

    private fun errorIfIndexOrUniqueAnnotation(field: VariableElement, relationType: String) {
        val hasIndex = field.hasAnnotation(Index::class.java)
        if (hasIndex || field.hasAnnotation(Unique::class.java)) {
            val annotationName = if (hasIndex) "Index" else "Unique"
            messages.error("'$field' @$annotationName can not be used with a $relationType relation.")
        }
    }

    private fun parseProperty(field: VariableElement) {
        // Why nullable? A property might not be parsed due to an error. We do not throw here.
        val propertyBuilder: Property.PropertyBuilder = (if (field.hasAnnotation(Convert::class.java)) {
            // verify @Convert custom type
            parseCustomProperty(field)
        } else {
            // verify that supported type is used
            parseSupportedProperty(field)
        }) ?: return

        propertyBuilder.property.parsedElement = field

        // checks if field is accessible
        val isPrivate = field.modifiers.contains(Modifier.PRIVATE)
        if (!isPrivate) {
            propertyBuilder.fieldAccessible()
        }
        // find getter method name
        val getterMethodName = getGetterMethodNameFor(propertyBuilder.property)
        propertyBuilder.getterMethodName(getterMethodName)

        // @Id
        val idAnnotation = field.getAnnotation(Id::class.java)
        val hasIdAnnotation = idAnnotation != null
        if (hasIdAnnotation) {
            if (propertyBuilder.property.propertyType != PropertyType.Long) {
                messages.error("An @Id property has to be of type Long.", field)
            }
            if (isPrivate && getterMethodName == null) {
                messages.error("An @Id property can not be private, or add a getter and setter.")
            }
            propertyBuilder.primaryKey()
            if (idAnnotation.assignable) {
                propertyBuilder.idAssignable()
            }
        }

        // @NameInDb
        val nameInDbAnnotation = field.getAnnotation(NameInDb::class.java)
        if (nameInDbAnnotation != null) {
            val name = nameInDbAnnotation.value
            if (name.isNotEmpty()) {
                propertyBuilder.dbName(name)
            }
        }

        // @Index
        parseIndexAndUniqueAnnotations(field, propertyBuilder, hasIdAnnotation)

        // @Uid
        val uidAnnotation = field.getAnnotation(Uid::class.java)
        if (uidAnnotation != null) {
            // just storing uid, id model sync will replace with correct id+uid
            // Note: UID values 0 and -1 are special: print current value and fail later
            val uid = if (uidAnnotation.value == 0L) -1 else uidAnnotation.value
            propertyBuilder.modelId(IdUid(0, uid))
        }
    }

    private fun parseIndexAndUniqueAnnotations(field: VariableElement, propertyBuilder: Property.PropertyBuilder,
            hasIdAnnotation: Boolean) {
        val indexAnnotation = field.getAnnotation(Index::class.java)
        val uniqueAnnotation = field.getAnnotation(Unique::class.java)
        if (indexAnnotation == null && uniqueAnnotation == null) {
            return
        }

        // determine index type
        val propertyType = propertyBuilder.property.propertyType
        val supportsHashIndex = propertyType == PropertyType.String
                // || propertyType == PropertyType.ByteArray // Not yet supported for byte[]
        val indexType = indexAnnotation?.type ?: IndexType.DEFAULT
        val indexFlags: Int = when (indexType) {
            IndexType.VALUE -> PropertyFlags.INDEXED
            IndexType.HASH -> PropertyFlags.INDEX_HASH
            IndexType.HASH64 -> PropertyFlags.INDEX_HASH64
            IndexType.DEFAULT -> {
                // auto detect
                if (supportsHashIndex) {
                    PropertyFlags.INDEX_HASH // String and byte[] like HASH
                } else {
                    PropertyFlags.INDEXED // others like VALUE
                }
            }
        }

        // error if HASH or HASH64 is not supported by property type
        if (!supportsHashIndex && (indexType == IndexType.HASH || indexType == IndexType.HASH64)) {
            messages.error("'$field' IndexType.$indexType is not supported for $propertyType.")
        }

        // error if used with @Id
        if (hasIdAnnotation) {
            val annotationName =  if(indexAnnotation != null) "Index" else "Unique"
            messages.error("'$field' @$annotationName can not be used with @Id.")
        }

        // error if unsupported property type
        if (propertyType == PropertyType.ByteArray || propertyType == PropertyType.Float ||
                propertyType == PropertyType.Double) {
            val annotationName =  if(indexAnnotation != null) "Index" else "Unique"
            messages.error("'$field' @$annotationName is not supported for $propertyType. Remove @$annotationName.")
        }

        propertyBuilder.indexAsc(null, indexFlags, 0, uniqueAnnotation != null)
    }

    private fun parseCustomProperty(field: VariableElement): Property.PropertyBuilder? {
        // extract @Convert annotation member values
        // as they are types, need to access them via annotation mirrors
        val annotationMirror = getAnnotationMirror(field, Convert::class.java) ?: return null // did not find @Convert mirror

        // converter and dbType value existence guaranteed by compiler
        val converter = getAnnotationValueType(annotationMirror, "converter")
        val dbType = getAnnotationValueType(annotationMirror, "dbType")

        val propertyType = typeHelper.getPropertyType(dbType)
        if (propertyType == null) {
            messages.error("@Convert dbType type is not supported, use a Java primitive wrapper class.", field)
            return null
        }

        // may be a parameterized type like List<CustomType>, so erase any type parameters
        val customType = typeUtils.erasure(field.asType())

        val propertyBuilder: Property.PropertyBuilder
        try {
            propertyBuilder = entityModel.addProperty(propertyType, field.simpleName.toString())
        } catch (e: RuntimeException) {
            messages.error("Could not add field: ${e.message}")
            return null
        }
        propertyBuilder.customType(customType.toString(), converter.toString())
        // note: custom types are already assumed non-primitive by Property#isNonPrimitiveType()
        return propertyBuilder
    }

    private fun parseSupportedProperty(field: VariableElement): Property.PropertyBuilder? {
        val typeMirror = field.asType()
        val propertyType = typeHelper.getPropertyType(typeMirror)
        if (propertyType == null) {
            messages.error("Field type \"$typeMirror\" is not supported. Consider making the target an @Entity, " +
                    "or using @Convert or @Transient on the field (see docs).", field)
            return null
        }

        val propertyBuilder: Property.PropertyBuilder
        try {
            propertyBuilder = entityModel.addProperty(propertyType, field.simpleName.toString())
        } catch (e: RuntimeException) {
            messages.error("Could not add field: ${e.message}")
            return null
        }

        val isPrimitive = typeMirror.kind.isPrimitive
        if (isPrimitive) {
            // treat primitive types as non-null
            propertyBuilder.notNull()
        } else if (propertyType.isScalar) {
            // treat wrapper types (Long, Integer, ...) of scalar types as non-primitive
            propertyBuilder.nonPrimitiveType()
        }

        return propertyBuilder
    }

    private fun getAnnotationMirror(element: Element, annotationClass: Class<*>): AnnotationMirror? {
        val annotationMirrors = element.annotationMirrors
        for (annotationMirror in annotationMirrors) {
            val annotationType = annotationMirror.annotationType
            val convertType = elementUtils.getTypeElement(annotationClass.canonicalName).asType()
            if (typeUtils.isSameType(annotationType, convertType)) {
                return annotationMirror
            }
        }
        return null
    }

    private fun getAnnotationValueType(annotationMirror: AnnotationMirror, memberName: String): TypeMirror? {
        val elementValues = annotationMirror.elementValues
        for ((key, value) in elementValues) {
            val elementName = key.simpleName.toString()
            if (elementName == memberName) {
                // this is a shortcut instead of using entry.getValue().accept(visitor, null)
                return value.value as TypeMirror
            }
        }
        return null
    }

    fun <A : Annotation> Element.hasAnnotation(annotationType: Class<A>): Boolean {
        return getAnnotation(annotationType) != null
    }

    /**
     * Tries to find a getter method name for the given property.
     * Prefers isPropertyName over getPropertyName if property starts with 'is' then uppercase letter.
     * Prefers isPropertyName over getPropertyName if property is Boolean.
     * Otherwise looks for getPropertyName method.
     * If none is found, returns null.
     */
    private fun getGetterMethodNameFor(property: Property): String? {
        val propertyName = property.propertyName
        val propertyNameCapitalized = propertyName.capitalize()

        // https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#properties
        // Kotlin: 'isProperty' (but not 'isproperty').
        if (propertyName.startsWith("is") && propertyName[2].isUpperCase()) {
            methods.find { it.simpleName.toString() == propertyName }?.let {
                return it.simpleName.toString() // Getter is called 'isProperty' (setter 'setProperty').
            }
        }

        // https://docs.oracle.com/javase/tutorial/javabeans/writing/properties.html
        // Java: 'isProperty' for booleans (JavaBeans spec).
        if (property.propertyType == PropertyType.Boolean) {
            methods.find { it.simpleName.toString() == "is$propertyNameCapitalized" }?.let {
                return it.simpleName.toString() // Getter is called 'isPropertyName'.
            }
        }

        // At last, check for regular getter.
        return methods.find { it.simpleName.toString() == "get$propertyNameCapitalized" }?.simpleName?.toString()
    }

    companion object {
        private const val INDEX_MAX_VALUE_LENGTH_MAX = 450
    }

}
