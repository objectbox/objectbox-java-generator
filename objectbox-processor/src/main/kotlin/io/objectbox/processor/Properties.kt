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

import io.objectbox.annotation.ConflictStrategy
import io.objectbox.annotation.Convert
import io.objectbox.annotation.DatabaseType
import io.objectbox.annotation.DefaultValue
import io.objectbox.annotation.ExternalName
import io.objectbox.annotation.ExternalType
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.IdCompanion
import io.objectbox.annotation.Index
import io.objectbox.annotation.IndexType
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.Transient
import io.objectbox.annotation.Type
import io.objectbox.annotation.Uid
import io.objectbox.annotation.Unique
import io.objectbox.annotation.Unsigned
import io.objectbox.converter.FlexObjectConverter
import io.objectbox.converter.IntegerFlexMapConverter
import io.objectbox.converter.IntegerLongMapConverter
import io.objectbox.converter.LongFlexMapConverter
import io.objectbox.converter.LongLongMapConverter
import io.objectbox.converter.NullToEmptyStringConverter
import io.objectbox.converter.StringFlexMapConverter
import io.objectbox.converter.StringLongMapConverter
import io.objectbox.converter.StringMapConverter
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.ModelException
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType
import io.objectbox.model.PropertyFlags
import java.util.*
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
class Properties(
    private val elementUtils: Elements,
    private val typeUtils: Types,
    private val messages: Messages,
    private val relations: Relations,
    private val entityModel: Entity,
    entityElement: Element,
    private val isSuperEntity: Boolean
) {

    private val typeHelper = TypeHelper(elementUtils, typeUtils)

    private val fields: List<VariableElement> = ElementFilter.fieldsIn(entityElement.enclosedElements)
    private val methods: List<ExecutableElement> = ElementFilter.methodsIn(entityElement.enclosedElements)

    fun hasBoxStoreField(): Boolean {
        return fields.find { it.simpleName.toString() == BOXSTORE_FIELD_NAME } != null
    }

    fun parseFields() {
        for (field in fields) {
            parseField(field)
        }
    }

    private fun String.isReservedName(): Boolean {
        return this == BOXSTORE_FIELD_NAME
    }

    private fun parseField(field: VariableElement) {
        // ignore static, transient or @Transient fields
        val modifiers = field.modifiers
        if (modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.TRANSIENT)
            || field.hasAnnotation(Transient::class.java)
        ) {
            return
        }

        if (field.simpleName.toString().isReservedName()) {
            messages.error(
                "A property can not be named `__boxStore`. Adding a BoxStore field for relations? Annotate it with @Transient.",
                field
            )
            return
        }

        if (typeHelper.isToOne(field.asType())) {
            // ToOne<TARGET> property
            checkNotSuperEntity(field)
            checkNoIndexOrUniqueAnnotation(field, "ToOne")
            relations.parseToOne(entityModel, field)
        } else if (
            !field.hasAnnotation(Convert::class.java)
            && typeHelper.isList(field.asType())
            && !typeHelper.isStringList(field.asType())
        ) {
            // List<TARGET> property, except List<String>
            checkNotSuperEntity(field)
            checkNoIndexOrUniqueAnnotation(field, "List")
            relations.parseToMany(entityModel, field)
        } else if (typeHelper.isToMany(field.asType())) {
            // ToMany<TARGET> property
            checkNotSuperEntity(field)
            checkNoIndexOrUniqueAnnotation(field, "ToMany")
            relations.parseToMany(entityModel, field)
        } else {
            // regular property
            parseProperty(field)
        }
    }

    private fun checkNotSuperEntity(field: VariableElement) {
        if (isSuperEntity) {
            messages.error("A super class of an @Entity must not have a relation.", field)
        }
    }

    private fun checkNoIndexOrUniqueAnnotation(field: VariableElement, relationType: String) {
        val hasIndex = field.hasAnnotation(Index::class.java)
        if (hasIndex || field.hasAnnotation(Unique::class.java)) {
            val annotationName = if (hasIndex) "Index" else "Unique"
            messages.error(
                "@$annotationName can not be used with a $relationType relation, remove @$annotationName.",
                field
            )
        }
    }

    private fun parseProperty(field: VariableElement) {
        // Why nullable? A property might not be parsed due to an error. Do not throw here.
        val propertyBuilder: Property.PropertyBuilder = when {
            field.hasAnnotation(DefaultValue::class.java) -> {
                defaultValuePropertyBuilderOrNull(field)
            }

            field.hasAnnotation(Convert::class.java) -> {
                // verify @Convert custom type
                customPropertyBuilderOrNull(field)
            }

            else -> {
                // Is it a type directly supported by the database?
                val propertyType = typeHelper.getPropertyType(field.asType())
                if (propertyType != null) {
                    supportedPropertyBuilderOrNull(field, propertyType)
                } else {
                    // Maybe a built-in converter can be used?
                    autoConvertedPropertyBuilderOrNull(field)
                }
            }
        } ?: return

        propertyBuilder.property.parsedElement = field

        // checks if field is accessible
        val isPrivate = field.modifiers.contains(Modifier.PRIVATE)
        if (!isPrivate) {
            propertyBuilder.fieldAccessible()
        }
        // find getter method name
        val getterMethodName = getGetterMethodNameFor(field.asType(), propertyBuilder.property)
        propertyBuilder.getterMethodName(getterMethodName)

        // @Id
        val idAnnotation = field.getAnnotation(Id::class.java)
        val hasIdAnnotation = idAnnotation != null
        if (hasIdAnnotation) {
            if (propertyBuilder.property.propertyType != PropertyType.Long) {
                messages.error("An @Id property must be a not-null long.", field)
            }
            if (isPrivate && getterMethodName == null) {
                messages.error("An @Id property must not be private or have a not-private getter and setter.", field)
            }
            propertyBuilder.primaryKey()
            if (idAnnotation.assignable) {
                propertyBuilder.idAssignable()
            }
        }

        // @IdCompanion
        if (field.hasAnnotation(IdCompanion::class.java)) {
            // Ensure there is at most one @IdCompanion.
            val existing = entityModel.properties.find { it.isIdCompanion }
            if (existing != null) {
                messages.error("'${existing.propertyName}' is already an @IdCompanion property, there can only be one.")
            } else {
                // Only Date or DateNano are supported.
                if (propertyBuilder.property.propertyType != PropertyType.Date
                    && propertyBuilder.property.propertyType != PropertyType.DateNano
                ) {
                    messages.error(
                        "@IdCompanion has to be of type Date or a long annotated with @Type(DateNano).",
                        field
                    )
                } else {
                    propertyBuilder.idCompanion()
                }
            }
        }

        // @Unsigned
        if (field.hasAnnotation(Unsigned::class.java)) {
            val type = propertyBuilder.property.propertyType
            if (type != PropertyType.Byte && type != PropertyType.Short && type != PropertyType.Int
                && type != PropertyType.Long && type != PropertyType.Char
            ) {
                messages.error("@Unsigned is only supported for integer properties.")
            } else if (hasIdAnnotation) {
                messages.error("@Unsigned can not be used with @Id. ID properties are unsigned internally by default.")
            } else {
                propertyBuilder.unsigned()
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

        // @ExternalName
        field.getAnnotation(ExternalName::class.java)?.value
            ?.also { propertyBuilder.externalName(it) }

        // @ExternalType
        // Note: not validating the external type for the property type here. Instead, let the database do
        // it at runtime as this can be complex. And we don't want to maintain checks in multiple places.
        field.getAnnotation(ExternalType::class.java)?.value
            ?.also { propertyBuilder.externalType(it) }

        // @Index, @Unique
        parseIndexAndUniqueAnnotations(field, propertyBuilder, hasIdAnnotation)

        // @HnswIndex
        // Note: using other index annotations on FloatArray currently
        // errors, so no need to integrate with regular index processing.
        parseHnswIndexAnnotation(field, propertyBuilder)

        // @Uid
        val uidAnnotation = field.getAnnotation(Uid::class.java)
        if (uidAnnotation != null) {
            // just storing uid, id model sync will replace with correct id+uid
            // Note: UID values 0 and -1 are special: print current value and fail later
            val uid = if (uidAnnotation.value == 0L) -1 else uidAnnotation.value
            propertyBuilder.modelId(IdUid(0, uid))
        }
    }

    private fun parseHnswIndexAnnotation(field: VariableElement, propertyBuilder: Property.PropertyBuilder) {
        val hnswIndexAnnotation = field.getAnnotation(HnswIndex::class.java) ?: return
        val propertyType = propertyBuilder.property.propertyType
        if (propertyType != PropertyType.FloatArray) {
            messages.error("@HnswIndex is only supported for float vector properties.")
        }
        try {
            propertyBuilder.hnswParams(hnswIndexAnnotation)
        } catch (e: ModelException) {
            messages.error(e.message!!, field)
        }
    }

    private fun parseIndexAndUniqueAnnotations(
        field: VariableElement, propertyBuilder: Property.PropertyBuilder,
        hasIdAnnotation: Boolean
    ) {
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

        // error if HASH or HASH64 is not supported by property type
        if (!supportsHashIndex && (indexType == IndexType.HASH || indexType == IndexType.HASH64)) {
            messages.error("IndexType.$indexType is not supported for $propertyType.", field)
        }

        // error if used with @Id
        if (hasIdAnnotation) {
            val annotationName = if (indexAnnotation != null) "Index" else "Unique"
            messages.error("@Id property is unique and indexed by default, remove @$annotationName.", field)
        }

        // error if unsupported property type
        if (propertyType == PropertyType.Float ||
            propertyType == PropertyType.Double ||
            propertyType == PropertyType.BooleanArray ||
            propertyType == PropertyType.ByteArray ||
            propertyType == PropertyType.ShortArray ||
            propertyType == PropertyType.CharArray ||
            propertyType == PropertyType.IntArray ||
            propertyType == PropertyType.LongArray ||
            propertyType == PropertyType.FloatArray ||
            propertyType == PropertyType.DoubleArray ||
            propertyType == PropertyType.StringArray
        ) {
            val annotationName = if (indexAnnotation != null) "Index" else "Unique"
            messages.error("@$annotationName is not supported for $propertyType, remove @$annotationName.", field)
        }

        // compute actual property flags for model
        var indexFlags: Int = when (indexType) {
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
        if (uniqueAnnotation != null) {
            indexFlags = indexFlags or PropertyFlags.UNIQUE
            // determine unique conflict resolution
            if (uniqueAnnotation.onConflict == ConflictStrategy.REPLACE) {
                indexFlags = indexFlags or PropertyFlags.UNIQUE_ON_CONFLICT_REPLACE
            }
        }

        propertyBuilder.index(indexFlags, 0)
    }

    private fun defaultValuePropertyBuilderOrNull(field: VariableElement): Property.PropertyBuilder? {
        if (field.hasAnnotation(Convert::class.java)) {
            messages.error("Can not use both @Convert and @DefaultValue.", field)
            return null
        }

        when (field.getAnnotation(DefaultValue::class.java).value) {
            "" -> {
                val propertyType = typeHelper.getPropertyType(field.asType())
                if (propertyType != PropertyType.String) {
                    messages.error("For @DefaultValue(\"\") property must be String.", field)
                    return null
                }

                val builder = try {
                    entityModel.addProperty(propertyType, field.simpleName.toString())
                } catch (e: Exception) {
                    messages.error("Could not add property: ${e.message}", field)
                    if (e is ModelException) return null else throw e
                }
                builder.customType(field.asType().toString(), NullToEmptyStringConverter::class.java.canonicalName)

                return builder
            }

            else -> {
                messages.error("Only @DefaultValue(\"\") is supported.", field)
                return null
            }
        }
    }

    private fun customPropertyBuilderOrNull(field: VariableElement): Property.PropertyBuilder? {
        // extract @Convert annotation member values
        // as they are types, need to access them via annotation mirrors
        val annotationMirror = getAnnotationMirror(field, Convert::class.java)
            ?: return null // did not find @Convert mirror

        // converter and dbType value existence guaranteed by compiler
        val converter = getAnnotationValueType(annotationMirror, "converter")!!
        val dbType = getAnnotationValueType(annotationMirror, "dbType")!!

        // Detect property type based on dbType of annotation.
        val propertyType = typeHelper.getPropertyType(dbType)
        if (propertyType == null) {
            messages.error("@Convert dbType type is not supported, use a Java primitive wrapper class.", field)
            return null
        }
        val propertyDbType = determinePropertyDatabaseType(
            field,
            propertyType
        ) ?: return null

        // may be a parameterized type like List<CustomType>, so erase any type parameters
        val customType = typeUtils.erasure(field.asType())

        val propertyBuilder = entityModel.tryToAddProperty(propertyDbType, field) ?: return null

        propertyBuilder.customType(customType.toString(), converter.toString())
        // Flag custom type properties as non-primitive to the database
        propertyBuilder.nonPrimitiveFlag()
        return propertyBuilder
    }

    /**
     * Parses the [field] and tries to add the property to the entity model, returns the started builder.
     * If adding the property to the model fails, prints an error and returns null.
     */
    private fun supportedPropertyBuilderOrNull(
        field: VariableElement,
        propertyType: PropertyType
    ): Property.PropertyBuilder? {
        val propertyDbType = determinePropertyDatabaseType(
            field,
            propertyType
        ) ?: return null

        val propertyBuilder = entityModel.tryToAddProperty(propertyDbType, field) ?: return null

        val typeMirror = field.asType()
        val isPrimitive = typeMirror.kind.isPrimitive
        // Flag wrapper types (Long, Integer, ...) of scalar types and String list as non-primitive to the database
        if (!isPrimitive && (propertyDbType.isScalar || typeHelper.isStringList(typeMirror))) {
            propertyBuilder.nonPrimitiveFlag()
        }
        // For String vectors, indicate if the Java type is a List (or otherwise an array).
        if (propertyType == PropertyType.StringArray && typeHelper.isStringList(typeMirror)) {
            propertyBuilder.isList()
        }
        // Only Java primitive types can never be null
        if (isPrimitive) propertyBuilder.typeNotNull()

        return propertyBuilder
    }

    /**
     * If property type is overridden using a `@Type` annotation, returns the new property type.
     * Errors and returns null if `@Type` is used incorrectly.
     */
    private fun determinePropertyDatabaseType(field: VariableElement, propertyType: PropertyType): PropertyType? {
        val typeAnnotation = field.getAnnotation(Type::class.java)
        if (typeAnnotation != null) {
            return when (typeAnnotation.value) {
                DatabaseType.DateNano -> {
                    if (propertyType == PropertyType.Long) {
                        PropertyType.DateNano
                    } else {
                        messages.error("@Type(DateNano) only supports properties with type Long.", field)
                        null
                    }
                }

                else -> {
                    messages.error("@Type does not support the given type.", field)
                    null
                }
            }
        }

        // Not overridden, use property type detected based on field type.
        return propertyType
    }

    /**
     * If [field] has a type for which a built-in converter is available,
     * prints which converter is used and returns a builder. Otherwise, prints an error and returns null.
     */
    private fun autoConvertedPropertyBuilderOrNull(field: VariableElement): Property.PropertyBuilder? {
        val fieldType = field.asType()

        if (typeHelper.isStringStringMap(fieldType)) {
            return addAutoConvertedMapProperty(field, StringMapConverter::class.java.canonicalName)
        }

        if (typeHelper.isStringLongMap(fieldType)) {
            return addAutoConvertedMapProperty(field, StringLongMapConverter::class.java.canonicalName)
        }

        if (typeHelper.isStringMap(fieldType)) {
            return addAutoConvertedMapProperty(field, StringFlexMapConverter::class.java.canonicalName)
        }

        if (typeHelper.isIntegerLongMap(fieldType)) {
            return addAutoConvertedMapProperty(field, IntegerLongMapConverter::class.java.canonicalName)
        }

        if (typeHelper.isIntegerMap(fieldType)) {
            return addAutoConvertedMapProperty(field, IntegerFlexMapConverter::class.java.canonicalName)
        }

        if (typeHelper.isLongLongMap(fieldType)) {
            return addAutoConvertedMapProperty(field, LongLongMapConverter::class.java.canonicalName)
        }

        if (typeHelper.isLongMap(fieldType)) {
            return addAutoConvertedMapProperty(field, LongFlexMapConverter::class.java.canonicalName)
        }

        if (typeHelper.isObject(fieldType)) {
            val builder = entityModel.tryToAddProperty(PropertyType.Flex, field)
                ?: return null
            val converterCanonicalName = FlexObjectConverter::class.java.canonicalName
            builder.customType(field.asType().toString(), converterCanonicalName)
            messages.info("Using $converterCanonicalName to convert Object property '${field.simpleName}' in '${entityModel.className}', to change this use @Convert.")
            return builder
        }

        messages.error(
            "Field type \"$fieldType\" is not supported. Consider making the target an @Entity, " +
                    "or using @Convert or @Transient on the field (see docs).", field
        )
        return null
    }

    private fun addAutoConvertedMapProperty(
        field: VariableElement,
        converterCanonicalName: String
    ): Property.PropertyBuilder? {
        val builder = entityModel.tryToAddProperty(PropertyType.Flex, field)
            ?: return null

        // Is Map<K, V>, so erase type params (-> Map) as generator model does not support them.
        val plainMapType = typeUtils.erasure(field.asType()).toString()

        builder.customType(plainMapType, converterCanonicalName)
        messages.info("Using $converterCanonicalName to convert map property '${field.simpleName}' in '${entityModel.className}', to change this use @Convert.")

        return builder
    }

    private fun Entity.tryToAddProperty(propertyType: PropertyType, field: VariableElement): Property.PropertyBuilder? {
        return try {
            addProperty(propertyType, field.simpleName.toString())
        } catch (e: Exception) {
            messages.error("Could not add property: ${e.message}", field)
            if (e is ModelException) null else throw e
        }
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

    private fun <A : Annotation> Element.hasAnnotation(annotationType: Class<A>): Boolean {
        return getAnnotation(annotationType) != null
    }

    /**
     * Tries to find a getter method name for the given property that returns the given type.
     * Prefers isPropertyName over getPropertyName if property starts with 'is' then uppercase letter.
     * Prefers isPropertyName over getPropertyName if property is Boolean.
     * Otherwise, looks for getPropertyName method.
     * If none is found, returns null (Property falls back to expecting regular getter).
     */
    private fun getGetterMethodNameFor(fieldType: TypeMirror, property: Property): String? {
        val propertyName = property.propertyName
        val propertyNameCapitalized = propertyName.replaceFirstChar { it.titlecase(Locale.getDefault()) }

        // https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#properties
        // Kotlin: 'isProperty' (but not 'isproperty').
        if (propertyName.startsWith("is") && propertyName[2].isUpperCase()) {
            methods.find {
                it.simpleName.toString() == propertyName && typeUtils.isSameType(it.returnType, fieldType)
            }?.let {
                return it.simpleName.toString() // Getter is called 'isProperty' (setter 'setProperty').
            }
        }

        // https://docs.oracle.com/javase/tutorial/javabeans/writing/properties.html
        // Java: 'isProperty' for booleans (JavaBeans spec).
        if (property.propertyType == PropertyType.Boolean) {
            methods.find {
                it.simpleName.toString() == "is$propertyNameCapitalized" && typeUtils.isSameType(
                    it.returnType,
                    fieldType
                )
            }?.let {
                return it.simpleName.toString() // Getter is called 'isPropertyName'.
            }
        }

        // At last, check for regular getter.
        return methods.find {
            it.simpleName.toString() == "get$propertyNameCapitalized" && typeUtils.isSameType(it.returnType, fieldType)
        }?.simpleName?.toString()
    }

    companion object {
        @Suppress("unused") // Preserved for future use.
        private const val INDEX_MAX_VALUE_LENGTH_MAX = 450

        private const val BOXSTORE_FIELD_NAME = "__boxStore"
    }

}
