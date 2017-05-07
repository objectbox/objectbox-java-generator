package io.objectbox.codemodifier

import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.model.*

object GreendaoModelTranslator {
    // TODO types seems not consistent? (thus listing both here)
    var WRAPPER_TYPES = listOf("Boolean", "Byte", "Character", "Short", "Integer", "Long", "Float", "Double",
            "java.lang.Boolean", "java.lang.Byte", "java.lang.Character", "java.lang.Short", "java.lang.Integer",
            "java.lang.Long", "java.lang.Float", "java.lang.Double")

    /**
     * Modifies provided schema object according to entities list
     * @return mapping EntityClass to Entity
     * */
    fun convert(parsedEntities: Iterable<ParsedEntity>, schema: Schema, daoPackage: String?, idSync: IdSync)
            : Map<ParsedEntity, Entity> {
        val mapping = convertEntities(parsedEntities, schema, daoPackage, idSync)

        // Have the entities ready before parsing relations because target entities are required
        convertToOnes(parsedEntities, mapping, schema)

        // ToMany may depend on ToOne Backlinks, so ensure ToOnes were processed before
        convertToManys(parsedEntities, mapping, schema)

        return mapping
    }

    private fun convertEntities(parsedEntities: Iterable<ParsedEntity>, schema: Schema, daoPackage: String?,
                                idSync: IdSync)
            : Map<ParsedEntity, Entity> {
        return parsedEntities.map { parsedEntity ->
            val entity = schema.addEntity(parsedEntity.name)
            addBasicProperties(daoPackage, parsedEntity, entity)
            if (parsedEntity.dbName != null) entity.dbName = parsedEntity.dbName
            entity.isSkipCreationInDb = !parsedEntity.createInDb
            entity.javaPackage = parsedEntity.packageName

            val idSyncEntity = idSync.get(parsedEntity)
            entity.modelUid = idSyncEntity.uid
            entity.modelId = idSyncEntity.modelId
            entity.lastPropertyId = idSyncEntity.lastPropertyId

            convertProperties(parsedEntity, entity, idSync)

            // trigger creation of an additional protobuf dao
            if (parsedEntity.protobufClassName != null) {
                val protobufEntity = schema.addProtobufEntity(parsedEntity.protobufClassName.substringAfterLast("."))
                addBasicProperties(daoPackage, parsedEntity, protobufEntity)
                protobufEntity.dbName = entity.dbName // table name is required (checked in annotation visitor)
                protobufEntity.active = false
                protobufEntity.isSkipCreationInDb = true // table creation/deletion is handled by the original DAO
                protobufEntity.javaPackage = parsedEntity.protobufClassName.substringBeforeLast(".")
                convertProperties(parsedEntity, protobufEntity, idSync)
            }
            entity.parsedElement = parsedEntity.node

            parsedEntity to entity
        }.toMap()
    }

    private fun addBasicProperties(daoPackage: String?, it: ParsedEntity, entity: Entity) {
        entity.isConstructors = it.generateConstructors
        entity.javaPackageDao = daoPackage ?: it.packageName
        entity.javaPackageTest = daoPackage ?: it.packageName
        entity.isSkipGeneration = true
    }

    private fun convertProperties(parsedEntity: ParsedEntity, entity: Entity, idSync: IdSync) {
        parsedEntity.properties.forEach {
            try {
                convertProperty(entity, it, idSync.get(it))
            } catch (e: Exception) {
                throw RuntimeException("Can't add property '${it.variable}' to entity ${parsedEntity.name} " +
                        "due to: ${e.message}", e)
            }
        }
    }

    private fun convertToOnes(parsedEntities: Iterable<ParsedEntity>, mapping: Map<ParsedEntity, Entity>, schema: Schema) {
        for (parsedEntity in parsedEntities) {
            try {
                val entity = mapping[parsedEntity]!!
                for (toOne in parsedEntity.toOneRelations) {
                    convertToOne(toOne, parsedEntity, entity, schema)
                }
            } catch (e: Exception) {
                throw RuntimeException("Can't process ${parsedEntity.name}: ${e.message}", e)
            }
        }
    }

    private fun convertToOne(toOne: ToOneRelation, parsedEntity: ParsedEntity, entity: Entity, schema: Schema) {
        val targetEntity: Entity = schema.entities.singleOrNull() {
            it.className == toOne.targetType.simpleName
        } ?: throw RuntimeException("Relation target class ${toOne.variable.type.name} " +
                "defined in class ${parsedEntity.name} could not be found (is it an @Entity?)")

        val toOneConverted: ToOne
        val targetIdProperty: Property = entity.findPropertyByNameOrThrow(toOne.targetIdName)!!
        val name = toOne.variable.name
        val nameToOne = if (toOne.variableIsToOne) name else null
        toOneConverted = entity.addToOne(targetEntity, targetIdProperty, name, nameToOne)

        toOneConverted.parsedElement = toOne.astNode
    }

    private fun convertToManys(parsedEntities: Iterable<ParsedEntity>, mapping: Map<ParsedEntity, Entity>, schema: Schema) {
        for (parsedEntity in parsedEntities) {
            try {
                val entity = mapping[parsedEntity]!!
                for (toMany in parsedEntity.toManyRelations) {
                    convertToMany(toMany, parsedEntity, entity, schema)
                }
            } catch (e: Exception) {
                throw RuntimeException("Can't process ${parsedEntity.name}: ${e.message}", e)
            }
        }
    }

    private fun convertToMany(toMany: ToManyRelation, parsedEntity: ParsedEntity, entity: Entity, schema: Schema) {
        if (toMany.variable.type.name != "java.util.List") {
            throw RuntimeException("Can't create to-many relation for ${parsedEntity.name} " +
                    "on ${toMany.variable.type.name} ${toMany.variable.name}: " +
                    "use java.util.List<T>")
        }
        val targetType = toMany.variable.type.typeArguments?.singleOrNull()
                ?: throw RuntimeException("Can't create to-many relation on ${toMany.variable.name}. " +
                "ToMany type should have specified exactly one type argument")

        val targetEntity: Entity = schema.entities.singleOrNull() {
            it.className == targetType.simpleName
        } ?: throw RuntimeException("${targetType.name} is not an entity, but it is referenced " +
                "for @Relation relation (field: ${toMany.variable.name})")

        var backlinkName = toMany.backlinkName
        if (backlinkName == null) {
            // TODO test
            val backlinks = targetEntity.toOneRelations.filter {
                it.targetEntity == entity
            }
            if (backlinks.isEmpty()) {
                throw RuntimeException("Can't create to-many relation on ${toMany.variable.name}: create a ToOne on" +
                        "the target side first: only backlink to-many relations are supported at the moment")
            } else if (backlinks.size > 1) {
                throw RuntimeException("Can't create to-many relation on ${toMany.variable.name}:" +
                        "more than one possible backlink detected. use " +
                        "@Relation(idProperty=\"...\") with idProperty being a to-one @Relation in the " +
                        "target entity (to-many relations are \"backlinks\" of to-one relations)")
            }
            backlinkName = backlinks.single().name
        }

        // Currently not support in ObjectBox:
        val options = if (backlinkName != null) 1 else 0 + if (toMany.joinOnProperties.isNotEmpty()) 1 else 0
        if (options != 1) {
            throw RuntimeException("Can't create to-many relation on ${toMany.variable.name}. " +
                    "Either referencedJoinProperty, joinProperties or @JoinEntity must be used to describe the relation")
        }
        val toManyConverted = when {
        // ObjectBox currently only supports "mappedBy"
            backlinkName != null -> {
                val backlinkToOne = targetEntity.toOneRelations.singleOrNull {
                    // it.nameToOne may not be available yet, so also check + "ToOne" (quick hack)
                    it.targetEntity == entity && (it.name == backlinkName || it.name + "ToOne" == backlinkName)
                }
                val toOneTargetIdProperty = backlinkToOne?.targetIdProperty
                val backlinkProperty = toOneTargetIdProperty ?: targetEntity.findPropertyByNameOrThrow(backlinkName)
                val converted = entity.addToMany(targetEntity, backlinkProperty, toMany.variable.name)
                converted.parsedElement = toMany.astNode
                converted
            }
        // Currently not supported by ObjectBox
            toMany.joinOnProperties.isNotEmpty() -> {
                val joinOn = toMany.joinOnProperties
                entity.addToMany(
                        joinOn.map { entity.findPropertyByNameOrThrow(it.source) }.toTypedArray(),
                        targetEntity,
                        joinOn.map { targetEntity.findPropertyByNameOrThrow(it.target) }.toTypedArray()
                ).apply {
                    name = toMany.variable.name
                }
            }
            else -> throw RuntimeException("Insufficient relation info for $toMany")
        }
        if (toMany.order != null) {
            if (toMany.order.size > 0) {
                toMany.order.forEach {
                    val propertyInTarget = targetEntity.findPropertyByNameOrThrow(it.name)
                    when (it.order) {
                        Order.ASC -> toManyConverted.orderAsc(propertyInTarget)
                        Order.DESC -> toManyConverted.orderDesc(propertyInTarget)
                    }
                }
            } else {
                val pkProperty = targetEntity.properties.find { it.isPrimaryKey }
                        ?: throw RuntimeException("@OrderBy used to order by primary key of " +
                        "entity (${targetEntity.className}) without primary key")
                toManyConverted.orderAsc(pkProperty)
            }
        }
    }

    private fun convertProperty(entity: Entity, property: ParsedProperty, modelIds: io.objectbox.generator.idsync.Property) {
        val propertyType = convertPropertyType((property.customType?.columnJavaType ?: property.variable.type).name)
        val propertyBuilder = entity.addProperty(propertyType, property.variable.name)
        propertyBuilder.modelId(modelIds.id)
        if (modelIds.indexId != null) {
            propertyBuilder.modelIndexId(modelIds.indexId)
        }

        if (property.variable.type.isPrimitive) {
            propertyBuilder.notNull()
        } else if (WRAPPER_TYPES.contains(property.variable.type.name)) {
            propertyBuilder.nonPrimitiveType()
        }
        if (property.isNotNull) propertyBuilder.notNull()
        if (property.unique && property.index != null) {
            throw RuntimeException("Having unique constraint and index on the field " +
                    "at the same time is redundant. Either @Unique or @Index should be used")
        }
        if (property.unique) {
            propertyBuilder.unique()
        }
        if (property.index != null) {
            propertyBuilder.indexAsc(property.index!!.name, property.index!!.unique)
        }
        if (property.idParams != null) {
            propertyBuilder.primaryKey()
            if (property.idParams.autoincrement) propertyBuilder.autoincrement()
            if (property.idParams.assignable) propertyBuilder.idAssignable()
        }
        if (property.dbName != null) {
            propertyBuilder.dbName(property.dbName)
        } else if (property.idParams != null && propertyType == PropertyType.Long) {
            propertyBuilder.dbName("_id")
        }
        if (property.customType != null) {
            propertyBuilder.customType(property.variable.type.name, property.customType.converterClassName)
        }
        if (property.fieldAccessible) {
            propertyBuilder.fieldAccessible()
        }
        if (property.virtualTargetName != null) {
            propertyBuilder.virtualTargetName(property.virtualTargetName)
        }
        propertyBuilder.property.parsedElement = property.astNode
    }

    private fun convertPropertyType(javaTypeName: String): PropertyType = when (javaTypeName) {
        "boolean", "java.lang.Boolean", "Boolean" -> PropertyType.Boolean
        "byte", "java.lang.Byte", "Byte" -> PropertyType.Byte
        "int", "java.lang.Integer", "Integer" -> PropertyType.Int
        "long", "java.lang.Long", "Long" -> PropertyType.Long
        "float", "java.lang.Float", "Float" -> PropertyType.Float
        "double", "java.lang.Double", "Double" -> PropertyType.Double
        "short", "java.lang.Short", "Short" -> PropertyType.Short
        "byte[]" -> PropertyType.ByteArray
        "java.lang.String", "String" -> PropertyType.String
        "java.util.Date", "Date" -> PropertyType.Date
        else -> throw RuntimeException("Unsupported type ${javaTypeName}")
    }

}