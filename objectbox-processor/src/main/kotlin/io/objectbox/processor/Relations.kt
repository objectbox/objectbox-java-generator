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

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.TargetIdProperty
import io.objectbox.annotation.Uid
import io.objectbox.codemodifier.nullIfBlank
import io.objectbox.generator.IdUid
import io.objectbox.generator.TextUtil
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
import io.objectbox.generator.model.ToMany
import io.objectbox.generator.model.ToManyBase
import io.objectbox.generator.model.ToManyStandalone
import io.objectbox.generator.model.ToManyToMany
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType

data class ToOneRelation(
        val propertyName: String,
        val targetEntityName: String,
        var targetIdName: String? = null,
        val targetIdDbName: String? = null,
        val targetIdUid: Long? = null,
        val variableIsToOne: Boolean = false,
        val variableFieldAccessible: Boolean
)

data class ToManyRelation(
        val propertyName: String,
        val targetEntityName: String,
        val isBacklink: Boolean,
        var backlinkTo: String? = null,
        val fieldAccessible: Boolean,
        val nameInDb: String?,
        val uid: Long? = null
)

/**
 * Parses and keeps records of to-one and to-many relations of all parsed entities.
 */
class Relations(private val messages: Messages) {

    private val toOnesByEntity: MutableMap<Entity, MutableList<ToOneRelation>> = mutableMapOf()
    private val toManysByEntity: MutableMap<Entity, MutableList<ToManyRelation>> = mutableMapOf()

    fun hasRelations(entity: Entity) =
            (toOnesByEntity[entity]?.isNotEmpty() ?: false) || (toManysByEntity[entity]?.isNotEmpty() ?: false)

    private fun targetEntityNameOrError(field: VariableElement): String? {
        // assuming ToOne<TargetEntity>, List<TargetType> or ToMany<TargetType> field
        val fieldTypeMirror = field.asType() as DeclaredType
        val targetTypeMirror = fieldTypeMirror.typeArguments[0]
        return if (targetTypeMirror is DeclaredType) {
            // can simply get as element as code would not have compiled if target type is not known
            targetTypeMirror.asElement().simpleName.toString()
        } else {
            messages.error("Property '$field' can not have a relation to type $targetTypeMirror, " +
                    "specify an entity class instead")
            null
        }
    }

    fun parseToMany(entityModel: Entity, field: VariableElement) {
        val targetEntityName = targetEntityNameOrError(field) ?: return // skip faulty to-many

        val backlinkAnnotation = field.getAnnotation(Backlink::class.java)
        val isBacklink = backlinkAnnotation != null
        val nameInDb = field.getAnnotation(NameInDb::class.java)?.value
        val uid = field.getAnnotation(Uid::class.java)?.value
        if (isBacklink && nameInDb != null) messages.error("Backlinks are not allowed to have @NameInDb")
        if (isBacklink && uid != null) messages.error("Backlinks are not allowed to have @Uid")

        val toMany = ToManyRelation(
                propertyName = field.simpleName.toString(),
                targetEntityName = targetEntityName,
                isBacklink = isBacklink,
                backlinkTo = backlinkAnnotation?.to?.nullIfBlank(),
                fieldAccessible = !field.modifiers.contains(Modifier.PRIVATE),
                nameInDb = nameInDb,
                uid = uid.let { if (it == 0L) -1L else it }
        )

        collectToMany(entityModel, toMany)
    }

    fun parseToOne(entityModel: Entity, field: VariableElement) {
        val targetEntityName = targetEntityNameOrError(field) ?: return // skip faulty to-one

        if (field.getAnnotation(Backlink::class.java) != null) {
            messages.error("'$field' @Backlink can only be used on a ToMany relation")
        }

        val relationAnnotation = field.getAnnotation(TargetIdProperty::class.java)
        val targetIdName = relationAnnotation?.value?.nullIfBlank()
        val toOne = ToOneRelation(
                propertyName = field.simpleName.toString(),
                targetEntityName = targetEntityName,
                targetIdName = targetIdName,
                targetIdDbName = field.getAnnotation(NameInDb::class.java)?.value?.nullIfBlank(),
                targetIdUid = field.getAnnotation(Uid::class.java)?.value?.let { if (it == 0L) -1L else it },
                variableIsToOne = true,
                variableFieldAccessible = !field.modifiers.contains(Modifier.PRIVATE)
        )

        collectToOne(entityModel, toOne)
    }

    private fun collectToOne(entity: Entity, toOne: ToOneRelation) {
        var toOnes = toOnesByEntity[entity]
        if (toOnes == null) {
            toOnes = mutableListOf()
            toOnesByEntity[entity] = toOnes
        }
        toOnes.add(toOne)
    }

    private fun collectToMany(entity: Entity, toMany: ToManyRelation) {
        var toManys = toManysByEntity[entity]
        if (toManys == null) {
            toManys = mutableListOf()
            toManysByEntity[entity] = toManys
        }
        toManys.add(toMany)
    }

    fun ensureTargetIdProperties(entity: Entity) {
        val toOnes = toOnesByEntity[entity]
        // only if entity has to-one relations
        if (toOnes != null) {
            for (toOne in toOnes) {
                ensureTargetIdProperty(entity, toOne)
            }
        }
    }

    private fun ensureTargetIdProperty(entityModel: Entity, toOne: ToOneRelation) {
        if (toOne.targetIdName == null) {
            toOne.targetIdName = "${toOne.propertyName}Id"
        }

        val targetIdProperty = entityModel.findPropertyByName(toOne.targetIdName)
        if (targetIdProperty == null) {
            // target ID property not explicitly defined in entity, create a virtual one

            val propertyBuilder = entityModel.addProperty(PropertyType.Long, toOne.targetIdName)
            propertyBuilder.notNull()
            propertyBuilder.fieldAccessible()
            propertyBuilder.dbName(toOne.targetIdDbName)
            // just storing uid, id model sync will replace with correct id+uid
            if (toOne.targetIdUid != null) {
                propertyBuilder.modelId(IdUid(0, toOne.targetIdUid))
            }
            // TODO mj: ensure generator's ToOne uses the same targetName (ToOne.nameToOne)
            if (toOne.variableIsToOne) {
                val targetName = toOne.propertyName
                val targetValue =
                        if (toOne.variableFieldAccessible) targetName
                        else "get" + TextUtil.capFirst(targetName) + "()"
                propertyBuilder.virtualTargetValueExpression(targetValue).virtualTargetName(targetName)
            } else {
                val targetName = "${toOne.propertyName}ToOne"
                propertyBuilder.virtualTargetName(targetName)
            }
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
            toManys.filter { !it.isBacklink }.forEach { toMany ->
                if (!resolveToMany(schema, entity, toMany)) {
                    return false // resolving standalone to-many failed
                }
            }
        }

        // then resolve backlink to-many relations which depends on to-one and standalone to-many relations being resolved
        for ((entity, toManys) in toManysByEntity) {
            toManys.filter { it.isBacklink }.forEach { toMany ->
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
            messages.error("Relation target class '$targetEntityName' " +
                    "defined in class '${sourceEntity.className}' could not be found (is it an @Entity?)", sourceEntity)
        }
        return targetEntity
    }

    private fun resolveToOne(schema: Schema, entity: Entity, toOne: ToOneRelation): Boolean {
        val targetEntity = findTargetEntityOrRaiseError(schema, toOne.targetEntityName, entity) ?: return false

        val targetIdProperty = entity.findPropertyByName(toOne.targetIdName)
        if (targetIdProperty == null) {
            messages.error("Could not find property '${toOne.targetIdName}' in '${entity.className}'.", entity)
            return false
        }

        val name = toOne.propertyName
        val nameToOne = if (toOne.variableIsToOne) name else null

        entity.addToOne(targetEntity, targetIdProperty, name, nameToOne, toOne.variableFieldAccessible)
        return true
    }

    private fun addBacklinkToManyOrRaiseError(entityWithBacklink: Entity, targetEntity: Entity, backlinkToMany: ToManyRelation): ToManyBase? {
        val backlinkTo = backlinkToMany.backlinkTo
        if (backlinkTo.isNullOrEmpty()) {
            // no explicit target name: just ensure a single to-one or to-many relation, then use that
            val targetToOne = targetEntity.toOneRelations.filter {
                it.targetEntity == entityWithBacklink
            }
            val targetToMany = targetEntity.toManyRelations.filter {
                it.targetEntity == entityWithBacklink
            }
            return if (targetToOne.size == 1 && targetToMany.isEmpty()) {
                // back link from a ToOne
                val targetIdProperty = targetToOne[0].targetIdProperty
                entityWithBacklink.addBacklinkToToOneIfNoneExists(backlinkToMany, targetEntity, targetIdProperty)
            } else if (targetToOne.isEmpty() && targetToMany.size == 1) {
                // back link from a ToMany
                val targetToManyName = targetToMany[0].name
                entityWithBacklink.addBacklinkToToManyIfNoneExists(backlinkToMany, targetEntity, targetToManyName)
            } else if (targetToOne.isEmpty() && targetToMany.isEmpty()) {
                messages.error("Illegal @Backlink: no (to-one or to-many) relation found in '${targetEntity.className}'. " +
                        "Required by backlink to-many relation '${backlinkToMany.propertyName}' in '${entityWithBacklink.className}'.",
                        entityWithBacklink)
                null
            } else {
                messages.error("Set name of one to-one or to-many relation of '${targetEntity.className}' as @Backlink 'to' " +
                        "value to create the to-many relation '${backlinkToMany.propertyName}' in '${entityWithBacklink.className}'.",
                        entityWithBacklink)
                null
            }
        } else {
            // explicit target name: find the related to-one or to-many relation
            val targetToOne = targetEntity.toOneRelations.singleOrNull {
                it.targetEntity == entityWithBacklink
                        && (it.name == backlinkTo || it.targetIdProperty.propertyName == backlinkTo)
            }
            val targetToMany = targetEntity.toManyRelations.singleOrNull {
                it.targetEntity == entityWithBacklink && it.name == backlinkTo && it is ToManyStandalone
            }
            return if (targetToOne != null && targetToMany == null) {
                // back link from a ToOne
                val targetIdProperty = targetToOne.targetIdProperty
                entityWithBacklink.addBacklinkToToOneIfNoneExists(backlinkToMany, targetEntity, targetIdProperty)
            } else if (targetToOne == null && targetToMany != null) {
                // back link from a ToMany
                val targetToManyName = targetToMany.name
                entityWithBacklink.addBacklinkToToManyIfNoneExists(backlinkToMany, targetEntity, targetToManyName)
            } else if (targetToOne != null && targetToMany != null) {
                messages.error("Specify unique name for target property '$backlinkTo' in " +
                        "'${targetEntity.className}' of @Backlink in '${entityWithBacklink.className}'.",
                        entityWithBacklink)
                null
            } else {
                messages.error("Could not find target property '$backlinkTo' in " +
                        "'${targetEntity.className}' of @Backlink in '${entityWithBacklink.className}'.",
                        entityWithBacklink)
                null
            }
        }
    }

    private fun Entity.addBacklinkToToOneIfNoneExists(backlinkToMany: ToManyRelation, targetEntity: Entity,
                                                      targetIdProperty: Property): ToManyBase? {
        // check incoming to-many relations if they use the target property of the to-one
        val existingBacklink = targetEntity.incomingToManyRelations.find {
            it is ToMany && it.targetProperties != null
                    && it.targetProperties.let { it.size == 1 && it[0] == targetIdProperty }
        }
        if (existingBacklink != null) {
            errorOnlyOneBacklinkAllowed(this, backlinkToMany)
            return null // there is already a backlink to this to-one
        }

        return addToMany(targetEntity, targetIdProperty, backlinkToMany.propertyName)
    }

    private fun Entity.addBacklinkToToManyIfNoneExists(backlinkToMany: ToManyRelation, targetEntity: Entity,
                                                       targetToManyName: String): ToManyBase? {
        // check incoming to-many relations if they use the target property of the to-one
        val existingBacklink = targetEntity.incomingToManyRelations.find {
            it is ToManyToMany && it.linkedToManyName == targetToManyName
        }
        if (existingBacklink != null) {
            errorOnlyOneBacklinkAllowed(this, backlinkToMany)
            return null // there is already a backlink to this to-many
        }

        return addToMany(targetEntity, targetToManyName, backlinkToMany.propertyName)
    }

    private fun errorOnlyOneBacklinkAllowed(backlinkEntity: Entity, backlinkToMany: ToManyRelation) {
        messages.error("'${backlinkEntity.className}.${backlinkToMany.propertyName}' " +
                "Only one @Backlink per relation allowed. Remove all but one @Backlink.")
    }

    private fun resolveToMany(schema: Schema, entity: Entity, toMany: ToManyRelation): Boolean {
        val targetEntity = findTargetEntityOrRaiseError(schema, toMany.targetEntityName, entity) ?: return false

        val toManyModel: ToManyBase
        if (toMany.isBacklink) {
            // TODO ut why not directly add the linked to ToManyStandalone?
            toManyModel = addBacklinkToManyOrRaiseError(entity, targetEntity, toMany) ?: return false
        } else {
            val standalone = entity.addToManyStandalone(targetEntity, toMany.propertyName)
            if (toMany.uid != null) standalone.modelId = IdUid(0, toMany.uid)
            standalone.dbName = toMany.nameInDb
            toManyModel = standalone
        }

        toManyModel.isFieldAccessible = toMany.fieldAccessible
        return true
    }

}
