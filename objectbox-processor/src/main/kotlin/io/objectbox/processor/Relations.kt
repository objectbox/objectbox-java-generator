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

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.ExternalName
import io.objectbox.annotation.ExternalPropertyType
import io.objectbox.annotation.ExternalType
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.TargetIdProperty
import io.objectbox.annotation.Uid
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.ExternalPropertyTypeMapper
import io.objectbox.generator.model.ModelException
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
import io.objectbox.generator.model.ToManyBase
import io.objectbox.generator.model.ToManyByBacklink
import io.objectbox.generator.model.ToManyStandalone
import io.objectbox.generator.model.ToOne

/**
 * Parses ([ProcessingField]), keeps records of and resolves to-one and to-many relations of all
 * parsed entities.
 */
class Relations(private val messages: CommonMessages) {

    private val toOnesByEntity: MutableMap<Entity, MutableList<ToOne>> = mutableMapOf()
    private val toManysByEntity: MutableMap<Entity, MutableList<ToManyBase>> = mutableMapOf()

    fun hasRelations(entity: Entity) =
        (toOnesByEntity[entity]?.isNotEmpty() ?: false) || (toManysByEntity[entity]?.isNotEmpty() ?: false)

    private fun targetEntityNameOrError(entityModel: Entity, field: ProcessingField, relationType: String): String? {
        // assuming ToOne<TargetEntity>, List<TargetType> or ToMany<TargetType> field
        // Note: Java allows to not specify a type parameter (ToOne instead of ToOne<Entity>), so check for one.
        val fieldType = field.type
        if (fieldType.typeArgumentCount == 0) {
            messages.error(
                "The generic $relationType property '${field.name}' in '${entityModel.className}' " +
                        "must have a type argument, e.g. $relationType<Entity>."
            )
            return null
        }
        val targetEntityName = fieldType.relationTargetClassName()
        if (targetEntityName == null) {
            messages.error(
                "Property '${field.name}' can not have a relation to type ${fieldType.relationTargetDisplayName()}, " +
                        "specify an entity class instead"
            )
        }
        return targetEntityName
    }

    fun parseToMany(entityModel: Entity, field: ProcessingField) {
        val targetEntityName = targetEntityNameOrError(entityModel, field, "ToMany")
            ?: return // skip faulty to-many

        val isBacklink = field.hasAnnotation(Backlink::class.java)
        val isFieldAccessible = field.isAccessibleField
        val nameInDb = field.stringValueOf(NameInDb::class.java)
        val uid = field.longValueOf(Uid::class.java)
        if (isBacklink && nameInDb != null) messages.error("Backlinks are not allowed to have @NameInDb")
        if (isBacklink && uid != null) messages.error("Backlinks are not allowed to have @Uid")

        val toMany = if (isBacklink) {
            ToManyByBacklink(
                name = field.name,
                targetEntityName = targetEntityName,
                targetPropertyName = field.stringValueOf(Backlink::class.java, "to")?.ifBlank { null },
                isFieldAccessible = isFieldAccessible
            )
        } else {
            val externalName = field.stringValueOf(ExternalName::class.java)
            // Note: for a standalone ToMany only vector based external types are allowed, but leaving checks to the
            // database to not duplicate them here.
            val externalType = field.enumValueNameOf(ExternalType::class.java)
                ?.let { ExternalPropertyType.valueOf(it) }

            ToManyStandalone(
                name = field.name,
                dbName = nameInDb,
                targetEntityName = targetEntityName,
                isFieldAccessible = isFieldAccessible,
                uid = uid.let { if (it == 0L) -1L else it },
                externalName = externalName,
                externalTypeId = externalType?.let { ExternalPropertyTypeMapper.toId(it) },
                externalTypeExpression = externalType?.let { ExternalPropertyTypeMapper.toExpression(it) }
            )
        }

        collectToMany(entityModel, toMany)
    }

    fun parseToOne(entityModel: Entity, field: ProcessingField) {
        val targetEntityName = targetEntityNameOrError(entityModel, field, "ToOne")
            ?: return // skip faulty to-one

        if (field.hasAnnotation(Backlink::class.java)) {
            messages.error("'${field.name}' @Backlink can only be used on a ToMany relation")
        }

        val toOne = ToOne(
            name = field.name,
            isFieldAccessible = field.isAccessibleField,
            idRefPropertyName = field.stringValueOf(TargetIdProperty::class.java)?.ifBlank { null },
            idRefPropertyNameInDb = field.stringValueOf(NameInDb::class.java)?.ifBlank { null },
            idRefPropertyUid = field.longValueOf(Uid::class.java)?.let { if (it == 0L) -1L else it },
            targetEntityName = targetEntityName
        )

        collectToOne(entityModel, toOne)
    }

    private fun collectToOne(entity: Entity, toOne: ToOne) {
        toOnesByEntity.getOrPut(entity) { mutableListOf() }.add(toOne)
    }

    private fun collectToMany(entity: Entity, toMany: ToManyBase) {
        toManysByEntity.getOrPut(entity) { mutableListOf() }.add(toMany)
    }

    fun ensureToOneIdRefProperties(entity: Entity) {
        val toOnes = toOnesByEntity[entity]
        // only if entity has to-one relations
        if (toOnes != null) {
            for (toOne in toOnes) {
                ensureToOneIdRefProperty(entity, toOne)
            }
        }
    }

    private fun ensureToOneIdRefProperty(entityModel: Entity, toOne: ToOne) {
        val idRefProperty = entityModel.findPropertyByName(toOne.idRefPropertyName)
        if (idRefProperty == null) {
            // Target ID reference property not explicitly defined in entity, create a virtual one.

            val propertyBuilder = entityModel.addProperty(PropertyType.Long, toOne.idRefPropertyName)
            propertyBuilder.typeNotNull()
            propertyBuilder.fieldAccessible()
            propertyBuilder.dbName(toOne.idRefPropertyNameInDb)
            // just storing uid, id model sync will replace with correct id+uid
            val uid = toOne.idRefPropertyUid
            if (uid != null) {
                propertyBuilder.modelId(IdUid(0, uid))
            }
            propertyBuilder.virtualTargetName(toOne.name)
            propertyBuilder.virtualTargetValueExpression(toOne.toOneValueExpression)

            toOne.idRefProperty = propertyBuilder.property
        } else {
            // Target ID reference property explicitly defined (it's name matches the naming convention
            // or was given using the @TargetIdProperty annotation), check type is valid.
            if (idRefProperty.propertyType != PropertyType.Long) {
                messages.error(
                    "The target ID property '${toOne.idRefPropertyName}' for ToOne relation '${toOne.name}' in '${entityModel.className}' must be long.",
                    idRefProperty
                )
            }
            toOne.idRefProperty = idRefProperty
        }
    }

    /** Once all entities are parsed, relations are resolved and checked against their target entities.*/
    fun resolve(schema: Schema): Boolean {
        // resolve to-one relations first
        for ((entity, toOnes) in toOnesByEntity) {
            for (toOne in toOnes) {
                if (!resolveToOne(schema, entity, toOne)) {
                    return false // resolving to-one failed
                }
            }
        }

        // then resolve standalone to-many relations
        for ((entity, toManys) in toManysByEntity) {
            toManys.filterIsInstance<ToManyStandalone>().forEach { toMany ->
                if (!resolveToMany(schema, entity, toMany)) {
                    return false // resolving standalone to-many failed
                }
            }
        }

        // then resolve backlink to-many relations which depends on to-one and standalone to-many relations being resolved
        for ((entity, toManys) in toManysByEntity) {
            toManys.filterIsInstance<ToManyByBacklink>().forEach { toMany ->
                if (!resolveToMany(schema, entity, toMany)) {
                    return false // resolving backlink to-many failed
                }
            }
        }
        return true
    }

    private fun findTargetEntityOrRaiseError(schema: Schema, targetEntityName: String, sourceEntity: Entity): Entity? {
        val targetEntity = schema.entities.singleOrNull {
            it.className == targetEntityName
        }
        if (targetEntity == null) {
            messages.error(
                "Relation target class '$targetEntityName' " +
                        "defined in class '${sourceEntity.className}' could not be found (is it an @Entity?)",
                sourceEntity
            )
        }
        return targetEntity
    }

    private fun resolveToOne(schema: Schema, entity: Entity, toOne: ToOne): Boolean {
        val targetEntity = findTargetEntityOrRaiseError(schema, toOne.targetEntityName, entity) ?: return false

        return try {
            entity.addToOne(toOne, targetEntity)
            true
        } catch (e: Exception) {
            messages.error("Could not add ToOne relation: ${e.message}")
            if (e is ModelException) false else throw e
        }
    }

    private fun addToManyByBacklinkOrRaiseError(
        entityWithBacklink: Entity,
        targetEntity: Entity,
        backlinkToMany: ToManyByBacklink
    ): ToManyBase? {
        val backlinkTo = backlinkToMany.targetPropertyName
        if (backlinkTo.isNullOrEmpty()) {
            // no explicit target name: just ensure a single to-one or to-many relation, then use that
            val targetToOne = targetEntity.toOneRelations.filter {
                it.targetEntity == entityWithBacklink
            }
            val targetToMany = targetEntity.toManyRelations
                .filterIsInstance<ToManyStandalone>()
                .filter { it.targetEntity == entityWithBacklink }
            return if (targetToOne.size == 1 && targetToMany.isEmpty()) {
                // back link from a ToOne
                entityWithBacklink.addBacklinkToToOneIfNoneExists(backlinkToMany, targetEntity, targetToOne[0])
            } else if (targetToOne.isEmpty() && targetToMany.size == 1) {
                // back link from a ToMany
                entityWithBacklink.addBacklinkToToManyIfNoneExists(backlinkToMany, targetEntity, targetToMany[0])
            } else if (targetToOne.isEmpty() && targetToMany.isEmpty()) {
                messages.error(
                    "Illegal @Backlink: no (to-one or to-many) relation found in '${targetEntity.className}'. " +
                            "Required by backlink to-many relation '${backlinkToMany.name}' in '${entityWithBacklink.className}'.",
                    entityWithBacklink
                )
                null
            } else {
                messages.error(
                    "Set name of one to-one or to-many relation of '${targetEntity.className}' as @Backlink 'to' " +
                            "value to create the to-many relation '${backlinkToMany.name}' in '${entityWithBacklink.className}'.",
                    entityWithBacklink
                )
                null
            }
        } else {
            // explicit target name: find the related to-one or to-many relation
            val targetToOne = targetEntity.toOneRelations.singleOrNull {
                it.targetEntity == entityWithBacklink
                        && (it.name == backlinkTo || it.idRefProperty?.propertyName == backlinkTo)
            }
            val targetToMany = targetEntity.toManyRelations
                .filterIsInstance<ToManyStandalone>()
                .singleOrNull { it.targetEntity == entityWithBacklink && it.name == backlinkTo }
            return if (targetToOne != null && targetToMany == null) {
                // back link from a ToOne
                entityWithBacklink.addBacklinkToToOneIfNoneExists(backlinkToMany, targetEntity, targetToOne)
            } else if (targetToOne == null && targetToMany != null) {
                // back link from a ToMany
                entityWithBacklink.addBacklinkToToManyIfNoneExists(backlinkToMany, targetEntity, targetToMany)
            } else if (targetToOne != null && targetToMany != null) {
                messages.error(
                    "Specify unique name for target property '$backlinkTo' in " +
                            "'${targetEntity.className}' of @Backlink in '${entityWithBacklink.className}'.",
                    entityWithBacklink
                )
                null
            } else {
                messages.error(
                    "Could not find target property '$backlinkTo' in " +
                            "'${targetEntity.className}' of @Backlink in '${entityWithBacklink.className}'.",
                    entityWithBacklink
                )
                null
            }
        }
    }

    private fun Entity.addBacklinkToToOneIfNoneExists(
        backlinkToMany: ToManyByBacklink, targetEntity: Entity,
        targetToOne: ToOne
    ): ToManyBase? {
        // Check if the target already has an incoming to-many relation based on this to-one relation.
        val existingBacklink = targetEntity.incomingToManyRelations.find { toManyBase ->
            toManyBase is ToManyByBacklink
                    && toManyBase.targetToOne != null
                    && toManyBase.targetToOne == targetToOne
        }
        if (existingBacklink != null) {
            errorOnlyOneBacklinkAllowed(this, backlinkToMany)
            return null // there is already a backlink to this to-one
        }

        return addToManyByToOneBacklink(backlinkToMany, targetEntity, targetToOne)
    }

    private fun Entity.addBacklinkToToManyIfNoneExists(
        backlinkToMany: ToManyByBacklink, targetEntity: Entity,
        targetToMany: ToManyStandalone
    ): ToManyBase? {
        // Check if the target already has an incoming to-many relation based on this to-many relation.
        val existingBacklink = targetEntity.incomingToManyRelations.find {
            it is ToManyByBacklink
                    && it.targetToMany != null
                    && it.targetToMany == targetToMany
        }
        if (existingBacklink != null) {
            errorOnlyOneBacklinkAllowed(this, backlinkToMany)
            return null // there is already a backlink to this to-many
        }

        return addToManyByToManyBacklink(backlinkToMany, targetEntity, targetToMany)
    }

    private fun errorOnlyOneBacklinkAllowed(backlinkEntity: Entity, backlinkToMany: ToManyByBacklink) {
        messages.error(
            "'${backlinkEntity.className}.${backlinkToMany.name}' " +
                    "Only one @Backlink per relation allowed. Remove all but one @Backlink."
        )
    }

    private fun resolveToMany(schema: Schema, entity: Entity, toMany: ToManyBase): Boolean {
        val targetEntity = findTargetEntityOrRaiseError(schema, toMany.targetEntityName, entity) ?: return false

        try {
            return when (toMany) {
                is ToManyByBacklink -> {
                    val addedToMany = addToManyByBacklinkOrRaiseError(entity, targetEntity, toMany)
                    addedToMany != null
                }

                is ToManyStandalone -> {
                    entity.addToMany(toMany, targetEntity)
                    true
                }

                else -> false
            }
        } catch (e: Exception) {
            messages.error("Could not add ToMany relation: ${e.message}")
            if (e is ModelException) return false else throw e
        }
    }

}
