package io.objectbox.processor

import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.HasParsedElement
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.tools.Diagnostic

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
        var targetIdName: String? = null
)

class Relations(private val messager: Messager) {

    val toOneByEntity: MutableMap<io.objectbox.generator.model.Entity, MutableList<ToOneRelation>> = mutableMapOf()
    val toManyByEntity: MutableMap<io.objectbox.generator.model.Entity, MutableList<ToManyRelation>> = mutableMapOf()

    fun hasRelations() = toOneByEntity.isNotEmpty() || toManyByEntity.isNotEmpty()

    fun collectToOne(entity: Entity, toOne: ToOneRelation) {
        var toOnes = toOneByEntity[entity]
        if (toOnes == null) {
            toOnes = mutableListOf<ToOneRelation>()
            toOneByEntity.put(entity, toOnes)
        }
        toOnes.add(toOne)
    }

    fun collectToMany(entity: Entity, toMany: ToManyRelation) {
        var toManys = toManyByEntity[entity]
        if (toManys == null) {
            toManys = mutableListOf<ToManyRelation>()
            toManyByEntity.put(entity, toManys)
        }
        toManys.add(toMany)
    }

    fun ensureForeignKeys(entity: Entity) {
        val toOnes = toOneByEntity[entity]
        // only if entity has to-one relations
        if (toOnes != null) {
            for (toOne in toOnes) {
                ensureForeignKeys(entity, toOne)
            }
        }
    }

    private fun ensureForeignKeys(entityModel: io.objectbox.generator.model.Entity, toOne: ToOneRelation) {
        if (toOne.targetIdName == null) {
            toOne.targetIdName = "${toOne.propertyName}Id"
        }

        val foreignKeyProperty = entityModel.findPropertyByName(toOne.targetIdName)
        if (foreignKeyProperty == null) {
            // foreign key property not explicitly defined in entity, create a virtual one

            val propertyBuilder = entityModel.addProperty(PropertyType.Long, toOne.targetIdName)
            propertyBuilder.notNull()
            propertyBuilder.fieldAccessible()
            propertyBuilder.dbName(toOne.targetIdDbName)
            // just storing uid, id model sync will replace with correct id+uid
            if (toOne.targetIdUid != null) {
                propertyBuilder.modelId(IdUid(0, toOne.targetIdUid))
            }
            // TODO mj: ensure generator's ToOne uses the same targetName (ToOne.nameToOne)
            val targetName = if (toOne.variableIsToOne) toOne.propertyName else "${toOne.propertyName}ToOne"
            propertyBuilder.virtualTargetName(targetName)
        }
    }

    fun resolve(schema: Schema): Boolean {
        // resolve to-one relations
        for (entity in schema.entities) {
            val toOnes = toOneByEntity[entity]
            if (toOnes != null) {
                for (toOne in toOnes) {
                    if (!resolveToOne(schema, entity, toOne)) {
                        return false // resolving to-one failed
                    }
                }
            }
        }
        // then resolve to-many relations which depend on to-one relations being resolved
        for (entity in schema.entities) {
            val toManys = toManyByEntity[entity]
            if (toManys != null) {
                for (toMany in toManys) {
                    if (!resolveToMany(schema, entity, toMany)) {
                        return false // resolving to-many failed
                    }
                }
            }
        }
        return true
    }

    private fun resolveToOne(schema: Schema, entity: io.objectbox.generator.model.Entity, toOne: ToOneRelation): Boolean {
        val targetEntity = schema.entities.singleOrNull {
            it.className == toOne.targetEntityName
        }
        if (targetEntity == null) {
            error("Relation target class ${toOne.targetEntityName} " +
                    "defined in class ${entity.className} could not be found (is it an @Entity?)", entity)
            return false
        }

        val targetIdProperty = entity.findPropertyByName(toOne.targetIdName)
        if (targetIdProperty == null) {
            error("Could not find property ${toOne.targetIdName} in ${entity.className}.", entity)
            return false
        }

        val name = toOne.propertyName
        val nameToOne = if (toOne.variableIsToOne) name else null

        entity.addToOne(targetEntity, targetIdProperty, name, nameToOne, toOne.variableFieldAccessible)
        return true
    }

    private fun resolveToMany(schema: Schema, entity: io.objectbox.generator.model.Entity, toMany: ToManyRelation):
            Boolean {
        val targetEntity = schema.entities.singleOrNull {
            it.className == toMany.targetEntityName
        }
        if (targetEntity == null) {
            error("ToMany target class '${toMany.targetEntityName}' " +
                    "defined in class '${entity.className}' could not be found (is it an @Entity?)", entity)
            return false
        }

        val targetToOne = if (toMany.targetIdName.isNullOrEmpty()) {
            // no explicit target name: just ensure a single to-one relation, then use that
            val targetToOne = targetEntity.toOneRelations.filter {
                it.targetEntity == entity
            }
            if (targetToOne.isEmpty()) {
                error("A to-one relation must be added to '${targetEntity.className}' to create the to-many relation " +
                        "'${toMany.propertyName}' in '${entity.className}'.", entity)
                return false
            } else if (targetToOne.size > 1) {
                error("Set name of one to-one relation of '${targetEntity.className}' as @Backlink 'to' value to " +
                        "create the to-many relation '${toMany.propertyName}' in '${entity.className}'.", entity)
                return false
            }
            targetToOne[0]
        } else {
            // explicit target name: find the related to-one relation
            val targetToOne = targetEntity.toOneRelations.singleOrNull {
                it.targetEntity == entity && it.name == toMany.targetIdName
            }
            if (targetToOne == null) {
                error("Could not find property '${toMany.targetIdName}' in '${entity.className}'.", entity)
                return false
            }
            targetToOne
        }

        entity.addToMany(targetEntity, targetToOne.targetIdProperty, toMany.propertyName)
        return true
    }

    private fun error(message: String, elementHolder: HasParsedElement? = null) {
        val element: Element? = if (elementHolder?.parsedElement is Element) elementHolder.parsedElement as Element else null
        messager.printMessage(Diagnostic.Kind.ERROR, "ObjectBox processor: " + message, element)
    }

}
