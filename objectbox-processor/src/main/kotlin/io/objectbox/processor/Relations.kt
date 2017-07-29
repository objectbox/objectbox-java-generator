package io.objectbox.processor

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.NameInDb
import io.objectbox.annotation.Relation
import io.objectbox.annotation.Uid
import io.objectbox.codemodifier.nullIfBlank
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema
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
        var targetIdName: String? = null,
        val fieldAccessible: Boolean
)

/**
 * Parses and keeps records of to-one and to-many relations of all parsed entities.
 */
class Relations(private val messages: Messages) {

    val toOneByEntity: MutableMap<Entity, MutableList<ToOneRelation>> = mutableMapOf()
    val toManyByEntity: MutableMap<Entity, MutableList<ToManyRelation>> = mutableMapOf()

    fun hasRelations(entity: Entity) =
            (toOneByEntity[entity]?.isNotEmpty() ?: false) || (toManyByEntity[entity]?.isNotEmpty() ?: false)

    fun parseToMany(entityModel: Entity, field: VariableElement) {
        // assuming List<TargetType> or ToMany<TargetType>
        val toManyTypeMirror = field.asType() as DeclaredType
        val targetTypeMirror = toManyTypeMirror.typeArguments[0] as DeclaredType
        // can simply get as element as code would not have compiled if target type is not known
        val targetEntityName = targetTypeMirror.asElement().simpleName

        val backlinkAnnotation = field.getAnnotation(Backlink::class.java)

        val toMany = ToManyRelation(
                propertyName = field.simpleName.toString(),
                targetEntityName = targetEntityName.toString(),
                targetIdName = backlinkAnnotation.to.nullIfBlank(),
                fieldAccessible = !field.modifiers.contains(Modifier.PRIVATE)
        )

        collectToMany(entityModel, toMany)
    }

    fun parseRelation(entityModel: Entity, field: VariableElement) {
        val targetTypeMirror = field.asType() as DeclaredType
        val relationAnnotation = field.getAnnotation(Relation::class.java)
        val targetIdName = if (relationAnnotation.idProperty.isBlank()) null else relationAnnotation.idProperty
        val toOne = buildToOneRelation(field, targetTypeMirror, targetIdName, false)

        collectToOne(entityModel, toOne)
    }

    fun parseToOne(entityModel: Entity, field: VariableElement) {
        // assuming ToOne<TargetType>
        val toOneTypeMirror = field.asType() as DeclaredType
        val targetTypeMirror = toOneTypeMirror.typeArguments[0] as DeclaredType
        val toOne = buildToOneRelation(field, targetTypeMirror, null, true)

        collectToOne(entityModel, toOne)
    }

    private fun buildToOneRelation(field: VariableElement, targetType: DeclaredType, targetIdName: String?,
                                   isExplicitToOne: Boolean): ToOneRelation {
        return ToOneRelation(
                propertyName = field.simpleName.toString(),
                // can simply get as element as code would not have compiled if target type is not known
                targetEntityName = targetType.asElement().simpleName.toString(),
                targetIdName = targetIdName,
                targetIdDbName = field.getAnnotation(NameInDb::class.java)?.value?.nullIfBlank(),
                targetIdUid = field.getAnnotation(Uid::class.java)?.value?.let { if (it == 0L) null else it },
                variableIsToOne = isExplicitToOne,
                variableFieldAccessible = !field.modifiers.contains(Modifier.PRIVATE)
        )
    }

    private fun collectToOne(entity: Entity, toOne: ToOneRelation) {
        var toOnes = toOneByEntity[entity]
        if (toOnes == null) {
            toOnes = mutableListOf<ToOneRelation>()
            toOneByEntity.put(entity, toOnes)
        }
        toOnes.add(toOne)
    }

    private fun collectToMany(entity: Entity, toMany: ToManyRelation) {
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

    private fun ensureForeignKeys(entityModel: Entity, toOne: ToOneRelation) {
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

    /** Once all entities are parsed, relations are resolved and checked against their target entities.*/
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

    private fun resolveToMany(schema: Schema, entity: Entity, toMany: ToManyRelation):
            Boolean {
        val targetEntity = findTargetEntityOrRaiseError(schema, toMany.targetEntityName, entity) ?: return false

        val targetToOne = if (toMany.targetIdName.isNullOrEmpty()) {
            // no explicit target name: just ensure a single to-one relation, then use that
            val targetToOne = targetEntity.toOneRelations.filter {
                it.targetEntity == entity
            }
            if (targetToOne.isEmpty()) {
                messages.error("A to-one relation must be added to '${targetEntity.className}' to create the to-many " +
                        "relation '${toMany.propertyName}' in '${entity.className}'.", entity)
                return false
            } else if (targetToOne.size > 1) {
                messages.error("Set name of one to-one relation of '${targetEntity.className}' as @Backlink 'to' " +
                        "value to create the to-many relation '${toMany.propertyName}' in '${entity.className}'.",
                        entity)
                return false
            }
            targetToOne[0]
        } else {
            // explicit target name: find the related to-one relation
            val targetToOne = targetEntity.toOneRelations.singleOrNull {
                it.targetEntity == entity && it.targetIdProperty.propertyName == toMany.targetIdName
            }
            if (targetToOne == null) {
                messages.error("Could not find target property '${toMany.targetIdName}' in " +
                        "'${targetEntity.className}' of @Backlink in '${entity.className}'.", entity)
                return false
            }
            targetToOne
        }

        val toManyModel = entity.addToMany(targetEntity, targetToOne.targetIdProperty, toMany.propertyName)
        toManyModel.isFieldAccessible = toMany.fieldAccessible
        return true
    }

}
