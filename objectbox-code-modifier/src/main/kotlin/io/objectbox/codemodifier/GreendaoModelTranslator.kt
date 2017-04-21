package io.objectbox.codemodifier

import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.TextUtil
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType
import io.objectbox.generator.model.Schema

object GreendaoModelTranslator {
    // TODO types seems not consistent? (thus listing both here)
    var WRAPPER_TYPES = listOf("Boolean", "Byte", "Character", "Short", "Integer", "Long", "Float", "Double",
            "java.lang.Boolean", "java.lang.Byte", "java.lang.Character", "java.lang.Short", "java.lang.Integer",
            "java.lang.Long", "java.lang.Float", "java.lang.Double")

    /**
     * Modifies provided schema object according to entities list
     * @return mapping EntityClass to Entity
     * */
    fun translate(entities: Iterable<ParsedEntity>, schema: Schema, daoPackage: String?, idSync: IdSync)
            : Map<ParsedEntity, Entity> {
        val mapping = convertEntities(entities, schema, daoPackage, idSync)

        convertToOneRelations(mapping, entities, schema)
        convertToManyRelations(mapping, entities, schema)

        return mapping
    }

    private fun convertEntities(parsedEntities: Iterable<ParsedEntity>, schema: Schema, daoPackage: String?,
                                idSync: IdSync)
            : Map<ParsedEntity, Entity> {
        return parsedEntities.map { parsedEntity ->
            val entity = schema.addEntity(parsedEntity.name)
            addBasicProperties(daoPackage, parsedEntity, entity)
            if (parsedEntity.dbName != null) entity.dbName = parsedEntity.dbName
            if (parsedEntity.active) entity.active = true
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

    private fun convertToOneRelations(mapping: Map<ParsedEntity, Entity>, entities: Iterable<ParsedEntity>, schema: Schema) {
        entities.filterNot {
            it.oneRelations.isEmpty()
        }.forEach {
            parsedEntity ->
            val entity = mapping[parsedEntity]!!
            parsedEntity.oneRelations.forEach {
                relation: OneRelation ->
                val targetEntity: Entity = schema.entities.find {
                    it.className == relation.variable.type.simpleName
                } ?: throw RuntimeException("Class ${relation.variable.type.name} marked " +
                        "with @Relation in class ${parsedEntity.name} is not an entity")
                when {
                    relation.foreignKeyField != null -> {
                        // find fkProperty in current entity
                        val fkProperty: Property = entity.properties.find {
                            it.propertyName == relation.foreignKeyField
                        } ?: throw RuntimeException("Can't find ${relation.foreignKeyField} in ${parsedEntity.name} " +
                                "for @Relation")
                        if (relation.columnName != null || relation.unique) {
                            throw RuntimeException(
                                    "If @Relation with ID property used, @Column and @Unique are ignored. " +
                                            "See ${parsedEntity.name}.${relation.variable.name}")
                        }
                        entity.addToOne(targetEntity, fkProperty, relation.variable.name)
                    }
                    else -> {
                        entity.addToOneWithoutProperty(
                                relation.variable.name,
                                targetEntity,
                                relation.columnName ?: TextUtil.dbName(relation.variable.name),
                                relation.isNotNull,
                                relation.unique
                        )
                    }
                }
            }
        }
    }

    private fun convertToManyRelations(mapping: Map<ParsedEntity, Entity>, entities: Iterable<ParsedEntity>, schema: Schema) {
        entities.filterNot {
            it.manyRelations.isEmpty()
        }.forEach {
            parsedEntity ->
            val entity = mapping[parsedEntity]!!
            try {
                parsedEntity.manyRelations.forEach {
                    relation ->
                    if (relation.variable.type.name != "java.util.List") {
                        throw RuntimeException("Can't create to-many relation for ${parsedEntity.name} " +
                                "on ${relation.variable.type.name} ${relation.variable.name}: " +
                                "use java.util.List<T>")
                    }
                    val argument = relation.variable.type.typeArguments?.singleOrNull()
                            ?: throw RuntimeException("Can't create to-many relation on ${relation.variable.name}. " +
                            "ToMany type should have specified exactly one type argument")

                    val targetEntity = schema.entities.find {
                        it.className == argument.simpleName
                    } ?: throw RuntimeException("${argument.name} is not an entity, but it is referenced " +
                            "for @Relation relation (field: ${relation.variable.name})")

                    if (relation.mappedBy == null) {
                        throw RuntimeException("Can't create to-many relation on ${relation.variable.name}: use " +
                                "@Relation(idProperty=\"...\") with idProperty being a to-one @Relation in the " +
                                "target entity (to-many relations are \"backlinks\" of to-one relations)")
                    }

                    // Currently not support in ObjectBox:
                    val options = if (relation.joinEntitySpec != null) 1 else 0 +
                            if (relation.mappedBy != null) 1 else 0 +
                                    if (relation.joinOnProperties.isNotEmpty()) 1 else 0
                    if (options != 1) {
                        throw RuntimeException("Can't create to-many relation on ${relation.variable.name}. " +
                                "Either referencedJoinProperty, joinProperties or @JoinEntity must be used to describe the relation")
                    }
                    val toMany = when {
                    // ObjectBox currently only supports "mappedBy"
                        relation.mappedBy != null -> {
                            val backlinkProperty = targetEntity.findProperty(relation.mappedBy)
                            entity.addToMany(targetEntity, backlinkProperty, relation.variable.name)
                        }
                    // Currently not supported by ObjectBox
                        relation.joinOnProperties.isNotEmpty() -> {
                            val joinOn = relation.joinOnProperties
                            entity.addToMany(
                                    joinOn.map { entity.findProperty(it.source) }.toTypedArray(),
                                    targetEntity,
                                    joinOn.map { targetEntity.findProperty(it.target) }.toTypedArray()
                            ).apply {
                                name = relation.variable.name
                            }
                        }
                    // Currently not supported by ObjectBox
                        else -> {
                            if (relation.joinEntitySpec == null) {
                                throw RuntimeException("Unknown @ToMany relation type")
                            }
                            val spec = relation.joinEntitySpec
                            val joinEntity = entities.find {
                                it.name == spec.entityName
                                        || it.qualifiedClassName == spec.entityName
                            }?.let { mapping[it] }
                                    ?: throw RuntimeException("Can't find join entity with name ${spec.entityName}")
                            entity.addToMany(
                                    targetEntity,
                                    joinEntity,
                                    joinEntity.findProperty(spec.sourceIdProperty),
                                    joinEntity.findProperty(spec.targetIdProperty)
                            ).apply {
                                name = relation.variable.name
                            }
                        }
                    }
                    if (relation.order != null) {
                        if (relation.order.size > 0) {
                            relation.order.forEach {
                                when (it.order) {
                                    Order.ASC -> toMany.orderAsc(targetEntity.findProperty(it.name))
                                    Order.DESC -> toMany.orderDesc(targetEntity.findProperty(it.name))
                                }
                            }
                        } else {
                            val pkProperty = targetEntity.properties.find { it.isPrimaryKey }
                                    ?: throw RuntimeException("@OrderBy used to order by primary key of " +
                                    "entity (${targetEntity.className}) without primary key")
                            toMany.orderAsc(pkProperty)
                        }
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Can't process ${parsedEntity.name}: ${e.message}", e)
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
        if(property.fieldAccessible) {
            propertyBuilder.fieldAccessible()
        }
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

    private fun Entity.findProperty(name: String): Property {
        return properties.find {
            it.propertyName == name
        } ?: throw RuntimeException("Can't find $name field in $className")
    }
}