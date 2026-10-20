/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2026 ObjectBox Ltd.
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
import io.objectbox.annotation.ExternalPropertyType
import io.objectbox.annotation.ExternalType
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.IdCompanion
import io.objectbox.annotation.Index
import io.objectbox.annotation.IndexType
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.SyncClock
import io.objectbox.annotation.SyncPrecedence
import io.objectbox.annotation.Transient
import io.objectbox.annotation.Type
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

/**
 * Parses properties from the fields ([ProcessingField]) of an entity class and adds them to the
 * entity model.
 */
class Properties(
    private val messages: CommonMessages,
    private val relations: Relations,
    private val entityModel: Entity,
    entityClass: ProcessingClass,
    private val isSuperEntity: Boolean
) {

    private val fields: List<ProcessingField> = entityClass.declaredFields()

    fun hasBoxStoreField(): Boolean {
        return fields.find { it.name == BOXSTORE_FIELD_NAME } != null
    }

    fun parseFields() {
        for (field in fields) {
            parseField(field)
        }
    }

    private fun String.isReservedName(): Boolean {
        return this == BOXSTORE_FIELD_NAME
    }

    private fun parseField(field: ProcessingField) {
        // ignore static, transient or @Transient fields
        if (field.isStatic || field.isTransient || field.hasAnnotation(Transient::class.java)) {
            return
        }

        if (field.isKotlin) {
            // When using KAPT, the processor already fails on a delegated property as it is
            // compiled to a field of the delegate type (like 'name$delegate').
            // However, KSP sees only the property itself (like 'name'), so add an explicit check.
            if (field.isDelegated) {
                messages.error(
                    "Delegated properties are not supported, use @Transient or a regular property.",
                    field
                )
                return
            }
            // ignore Kotlin properties that do not compile to a field
            if (!field.hasBackingField) {
                return
            }
        }

        if (field.name.isReservedName()) {
            messages.error(
                "A property can not be named `__boxStore`. Adding a BoxStore field for relations? Annotate it with @Transient.",
                field
            )
            return
        }

        val fieldType = field.type
        if (fieldType.isToOne) {
            // ToOne<TARGET> property
            checkNotSuperEntity(field)
            checkNoIndexOrUniqueAnnotation(field, "ToOne")
            relations.parseToOne(entityModel, field)
        } else if (
            !field.hasAnnotation(Convert::class.java)
            && fieldType.isList
            && !fieldType.isStringList
        ) {
            // List<TARGET> property, except List<String>
            checkNotSuperEntity(field)
            checkNoIndexOrUniqueAnnotation(field, "List")
            relations.parseToMany(entityModel, field)
        } else if (fieldType.isToMany) {
            // ToMany<TARGET> property
            checkNotSuperEntity(field)
            checkNoIndexOrUniqueAnnotation(field, "ToMany")
            relations.parseToMany(entityModel, field)
        } else {
            // regular property
            parseProperty(field)
        }
    }

    private fun checkNotSuperEntity(field: ProcessingField) {
        if (isSuperEntity) {
            messages.error("A super class of an @Entity must not have a relation.", field)
        }
    }

    private fun checkNoIndexOrUniqueAnnotation(field: ProcessingField, relationType: String) {
        val hasIndex = field.hasAnnotation(Index::class.java)
        if (hasIndex || field.hasAnnotation(Unique::class.java)) {
            val annotationName = if (hasIndex) "Index" else "Unique"
            messages.error(
                "@$annotationName can not be used with a $relationType relation, remove @$annotationName.",
                field
            )
        }
    }

    private fun parseProperty(field: ProcessingField) {
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
                val propertyType = field.type.propertyTypeOrNull()
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
        val isPrivate = !field.isAccessibleField
        if (!isPrivate) {
            propertyBuilder.fieldAccessible()
        }
        // find getter method name
        val getterMethodName = field.getterNameOrNull(propertyBuilder.property.propertyType)
        propertyBuilder.getterMethodName(getterMethodName)

        // @Id
        val hasIdAnnotation = field.hasAnnotation(Id::class.java)
        if (hasIdAnnotation) {
            if (propertyBuilder.property.propertyType != PropertyType.Long) {
                messages.error("An @Id property must be a not-null long.", field)
            }
            if (isPrivate && getterMethodName == null) {
                messages.error("An @Id property must not be private or have a not-private getter and setter.", field)
            }
            propertyBuilder.primaryKey()
            if (field.booleanValueOf(Id::assignable) == true) {
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
        val nameInDb = field.stringValueOf(NameInDb::value)
        if (nameInDb != null && nameInDb.isNotEmpty()) {
            propertyBuilder.dbName(nameInDb)
        }

        // @ExternalName
        field.stringValueOf(ExternalName::value)
            ?.also { propertyBuilder.externalName(it) }

        // @ExternalType
        // Note: not validating the external type for the property type here. Instead, let the database do
        // it at runtime as this can be complex. And we don't want to maintain checks in multiple places.
        field.enumValueNameOf(ExternalType::value)
            ?.also { propertyBuilder.externalType(ExternalPropertyType.valueOf(it)) }

        // @Index, @Unique
        parseIndexAndUniqueAnnotations(field, propertyBuilder, hasIdAnnotation)

        // @HnswIndex
        // Note: using other index annotations on FloatArray currently
        // errors, so no need to integrate with regular index processing.
        parseHnswIndexAnnotation(field, propertyBuilder)

        // @Uid
        field.uidValue()?.also {
            // just storing uid, id model sync will replace with correct id+uid
            propertyBuilder.modelId(IdUid(0, it))
        }

        // @SyncClock, @SyncPrecedence
        parseSyncClockAndSyncPrecedenceAnnotations(field, propertyBuilder)
    }

    private fun parseHnswIndexAnnotation(field: ProcessingField, propertyBuilder: Property.PropertyBuilder) {
        if (!field.hasAnnotation(HnswIndex::class.java)) return
        val propertyType = propertyBuilder.property.propertyType
        if (propertyType != PropertyType.FloatArray) {
            messages.error("@HnswIndex is only supported for float vector properties.")
        }
        // The model builder takes the annotation class, so get an actual instance.
        val hnswIndexAnnotation = field.annotationInstanceOf(HnswIndex::class.java) ?: return
        try {
            propertyBuilder.hnswParams(hnswIndexAnnotation)
        } catch (e: ModelException) {
            messages.error(e.message!!, field)
        }
    }

    private fun parseIndexAndUniqueAnnotations(
        field: ProcessingField, propertyBuilder: Property.PropertyBuilder,
        hasIdAnnotation: Boolean
    ) {
        val hasIndexAnnotation = field.hasAnnotation(Index::class.java)
        val hasUniqueAnnotation = field.hasAnnotation(Unique::class.java)
        if (!hasIndexAnnotation && !hasUniqueAnnotation) {
            return
        }

        // determine index type
        val propertyType = propertyBuilder.property.propertyType
        val supportsHashIndex = propertyType == PropertyType.String
        // || propertyType == PropertyType.ByteArray // Not yet supported for byte[]
        val indexType = if (hasIndexAnnotation) {
            field.enumValueNameOf(Index::type)
                ?.let { IndexType.valueOf(it) }
        } else {
            null
        } ?: IndexType.DEFAULT

        // error if HASH or HASH64 is not supported by property type
        if (!supportsHashIndex && (indexType == IndexType.HASH || indexType == IndexType.HASH64)) {
            messages.error("IndexType.$indexType is not supported for $propertyType.", field)
        }

        // error if used with @Id
        if (hasIdAnnotation) {
            val annotationName = if (hasIndexAnnotation) "Index" else "Unique"
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
            val annotationName = if (hasIndexAnnotation) "Index" else "Unique"
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
        if (hasUniqueAnnotation) {
            indexFlags = indexFlags or PropertyFlags.UNIQUE
            // determine unique conflict resolution
            if (field.enumValueNameOf(Unique::onConflict) == ConflictStrategy.REPLACE.name) {
                indexFlags = indexFlags or PropertyFlags.UNIQUE_ON_CONFLICT_REPLACE
            }
        }

        propertyBuilder.index(indexFlags, 0)
    }

    private fun parseSyncClockAndSyncPrecedenceAnnotations(
        field: ProcessingField,
        propertyBuilder: Property.PropertyBuilder
    ) {
        val hasSyncClock = field.hasAnnotation(SyncClock::class.java)
        val hasSyncPrecedence = field.hasAnnotation(SyncPrecedence::class.java)

        if (!hasSyncClock && !hasSyncPrecedence) return

        // Cannot have both on the same property
        if (hasSyncClock && hasSyncPrecedence) {
            messages.error("@SyncClock and @SyncPrecedence cannot be used on the same property", field)
            return
        }

        val annotationName = if (hasSyncClock) "@SyncClock" else "@SyncPrecedence"

        // Must be on a synced entity
        if (!entityModel.isSyncEnabled) {
            messages.error(
                "$annotationName can only be used on a property of a synced entity (annotated with @Sync)",
                field
            )
            return
        }

        // Must be a 64-bit integer property
        val property = propertyBuilder.property
        if (property.propertyType != PropertyType.Long) {
            messages.error("$annotationName can only be used on Long (OBXPropertyType.Long) properties", field)
            return
        }

        // Only one per entity
        // Note: the property being built is already added to the entity model, but it won't have the flag set, yet
        val existing = entityModel.properties
            .filter {
                if (hasSyncClock) it.isSyncClock else it.isSyncPrecedence
            }
        if (existing.isNotEmpty()) {
            val names = existing.map { it.propertyName }
                .plus(property.propertyName)
                .joinToString()
            messages.error(
                "only one property can be annotated with $annotationName, but found multiple: $names",
                field
            )
            return
        }

        // Add the flag
        if (hasSyncClock) {
            propertyBuilder.syncClock()
        } else {
            propertyBuilder.syncPrecedence()
        }
    }

    private fun defaultValuePropertyBuilderOrNull(field: ProcessingField): Property.PropertyBuilder? {
        if (field.hasAnnotation(Convert::class.java)) {
            messages.error("Can not use both @Convert and @DefaultValue.", field)
            return null
        }

        when (field.stringValueOf(DefaultValue::value)) {
            "" -> {
                val propertyType = field.type.propertyTypeOrNull()
                if (propertyType != PropertyType.String) {
                    messages.error("For @DefaultValue(\"\") property must be String.", field)
                    return null
                }

                val builder = try {
                    entityModel.addProperty(propertyType, field.name)
                } catch (e: Exception) {
                    messages.error("Could not add property: ${e.message}", field)
                    if (e is ModelException) return null else throw e
                }
                builder.customType(field.type.erasedJavaTypeName(), NullToEmptyStringConverter::class.java.canonicalName)

                return builder
            }

            else -> {
                messages.error("Only @DefaultValue(\"\") is supported.", field)
                return null
            }
        }
    }

    private fun customPropertyBuilderOrNull(field: ProcessingField): Property.PropertyBuilder? {
        // extract @Convert annotation member values
        // as they are class values, need to access them as types
        // converter and dbType value existence guaranteed by compiler
        val converter = field.typeValueOf(Convert::converter)!!
        val dbType = field.typeValueOf(Convert::dbType)!!

        // Detect property type based on dbType of annotation.
        val propertyType = dbType.propertyTypeOrNull()
        if (propertyType == null) {
            messages.error("@Convert dbType type is not supported, use a Java primitive wrapper class.", field)
            return null
        }
        val propertyDbType = determinePropertyDatabaseType(
            field,
            propertyType
        ) ?: return null

        // may be a parameterized type like List<CustomType>, so erase any type parameters
        val customType = field.type.erasedJavaTypeName()

        val propertyBuilder = entityModel.tryToAddProperty(propertyDbType, field) ?: return null

        propertyBuilder.customType(customType, converter.typeNameForCode())
        // Flag custom type properties as non-primitive to the database
        propertyBuilder.nonPrimitiveFlag()
        return propertyBuilder
    }

    /**
     * Parses the [field] and tries to add the property to the entity model, returns the started builder.
     * If adding the property to the model fails, prints an error and returns null.
     */
    private fun supportedPropertyBuilderOrNull(
        field: ProcessingField,
        propertyType: PropertyType
    ): Property.PropertyBuilder? {
        val propertyDbType = determinePropertyDatabaseType(
            field,
            propertyType
        ) ?: return null

        val propertyBuilder = entityModel.tryToAddProperty(propertyDbType, field) ?: return null

        val fieldType = field.type
        val isPrimitive = fieldType.isPrimitiveLike
        // Flag wrapper types (Long, Integer, ...) of scalar types and String list as non-primitive to the database
        if (!isPrimitive && (propertyDbType.isScalar || fieldType.isStringList)) {
            propertyBuilder.nonPrimitiveFlag()
        }
        // For String vectors, indicate if the Java type is a List (or otherwise an array).
        if (propertyType == PropertyType.StringArray && fieldType.isStringList) {
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
    private fun determinePropertyDatabaseType(field: ProcessingField, propertyType: PropertyType): PropertyType? {
        if (field.hasAnnotation(Type::class.java)) {
            val databaseType = field.enumValueNameOf(Type::value)
                ?.let { DatabaseType.valueOf(it) }
            return when (databaseType) {
                DatabaseType.DateNano -> {
                    if (propertyType == PropertyType.Long) {
                        PropertyType.DateNano
                    } else {
                        messages.error("@Type(DateNano) only supports properties with type Long.", field)
                        null
                    }
                }

                null -> {
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
    private fun autoConvertedPropertyBuilderOrNull(field: ProcessingField): Property.PropertyBuilder? {
        val fieldType = field.type

        if (fieldType.isStringStringMap) {
            return addAutoConvertedMapProperty(field, StringMapConverter::class.java.canonicalName)
        }

        if (fieldType.isStringLongMap) {
            return addAutoConvertedMapProperty(field, StringLongMapConverter::class.java.canonicalName)
        }

        if (fieldType.isStringMap) {
            return addAutoConvertedMapProperty(field, StringFlexMapConverter::class.java.canonicalName)
        }

        if (fieldType.isIntegerLongMap) {
            return addAutoConvertedMapProperty(field, IntegerLongMapConverter::class.java.canonicalName)
        }

        if (fieldType.isIntegerMap) {
            return addAutoConvertedMapProperty(field, IntegerFlexMapConverter::class.java.canonicalName)
        }

        if (fieldType.isLongLongMap) {
            return addAutoConvertedMapProperty(field, LongLongMapConverter::class.java.canonicalName)
        }

        if (fieldType.isLongMap) {
            return addAutoConvertedMapProperty(field, LongFlexMapConverter::class.java.canonicalName)
        }

        if (fieldType.isObject) {
            val builder = entityModel.tryToAddProperty(PropertyType.Flex, field)
                ?: return null
            val converterCanonicalName = FlexObjectConverter::class.java.canonicalName
            builder.customType(fieldType.erasedJavaTypeName(), converterCanonicalName)
            messages.info("Using $converterCanonicalName to convert Object property '${field.name}' in '${entityModel.className}', to change this use @Convert.")
            return builder
        }

        messages.error(
            "Field type \"$fieldType\" is not supported. Consider making the target an @Entity, " +
                    "or using @Convert or @Transient on the field (see docs).", field
        )
        return null
    }

    private fun addAutoConvertedMapProperty(
        field: ProcessingField,
        converterCanonicalName: String
    ): Property.PropertyBuilder? {
        val builder = entityModel.tryToAddProperty(PropertyType.Flex, field)
            ?: return null

        // Is Map<K, V>, so erase type params (-> Map) as generator model does not support them.
        val plainMapType = field.type.erasedJavaTypeName()

        builder.customType(plainMapType, converterCanonicalName)
        messages.info("Using $converterCanonicalName to convert map property '${field.name}' in '${entityModel.className}', to change this use @Convert.")

        return builder
    }

    private fun Entity.tryToAddProperty(propertyType: PropertyType, field: ProcessingField): Property.PropertyBuilder? {
        return try {
            addProperty(propertyType, field.name)
        } catch (e: Exception) {
            messages.error("Could not add property: ${e.message}", field)
            if (e is ModelException) null else throw e
        }
    }

    companion object {
        private const val BOXSTORE_FIELD_NAME = "__boxStore"
    }

}
