package org.greenrobot.greendao.codemodifier

import org.greenrobot.greendao.generator.*
import org.greenrobot.idsync.IdSync

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
        val mapping = mapEntityClassesToEntities(entities, schema, daoPackage, idSync)

        resolveToOneRelations(mapping, entities, schema)
        resolveToManyRelations(mapping, entities, schema)

        return mapping
    }

    private fun mapEntityClassesToEntities(entities: Iterable<ParsedEntity>, schema: Schema,
                                           daoPackage: String?, idSync: IdSync): Map<ParsedEntity, Entity> {
        return entities.map {
            val entity = schema.addEntity(it.name)
            addBasicProperties(daoPackage, it, entity)
            if (it.dbName != null) entity.dbName = it.dbName
            if (it.active) entity.active = true
            entity.isSkipCreationInDb = !it.createInDb
            entity.javaPackage = it.packageName
            entity.modelRefId = idSync.get(it).refId
            entity.modelId = idSync.get(it).id
            convertProperties(it, entity, idSync)

            // trigger creation of an additional protobuf dao
            if (it.protobufClassName != null) {
                val protobufEntity = schema.addProtobufEntity(it.protobufClassName.substringAfterLast("."))
                addBasicProperties(daoPackage, it, protobufEntity)
                protobufEntity.dbName = entity.dbName // table name is required (checked in annotation visitor)
                protobufEntity.active = false
                protobufEntity.isSkipCreationInDb = true // table creation/deletion is handled by the original DAO
                protobufEntity.javaPackage = it.protobufClassName.substringBeforeLast(".")
                convertProperties(it, protobufEntity, idSync)
            }

            it to entity
        }.toMap()
    }

    private fun addBasicProperties(daoPackage: String?, it: ParsedEntity, entity: Entity) {
        entity.isConstructors = it.generateConstructors
        entity.javaPackageDao = daoPackage ?: it.packageName
        entity.javaPackageTest = daoPackage ?: it.packageName
        entity.isSkipGeneration = true
    }

    private fun convertProperties(parsedEntity: ParsedEntity, entity: Entity, idSync: IdSync) {
        val properties = parsedEntity.getPropertiesInConstructorOrder() ?: parsedEntity.properties
        properties.forEach {
            try {
                convertProperty(entity, it, idSync.get(it))
            } catch (e: Exception) {
                throw RuntimeException("Can't add property '${it.variable}' to entity ${parsedEntity.name} " +
                        "due to: ${e.message}", e)
            }
        }
    }

    private fun resolveToOneRelations(mapping: Map<ParsedEntity, Entity>, entities: Iterable<ParsedEntity>, schema: Schema) {
        entities.filterNot {
            it.oneRelations.isEmpty()
        }.forEach {
            entity ->
            val source = mapping[entity]!!
            entity.oneRelations.forEach {
                relation ->
                val target = schema.entities.find {
                    it.className == relation.variable.type.simpleName
                } ?: throw RuntimeException("Class ${relation.variable.type.name} marked " +
                        "with @ToOne in class ${entity.name} is not an entity")
                when {
                    relation.foreignKeyField != null -> {
                        // find fkProperty in current entity
                        val fkProperty = source.properties.find {
                            it.propertyName == relation.foreignKeyField
                        } ?: throw RuntimeException("Can't find ${relation.foreignKeyField} in ${entity.name} " +
                                "for @ToOne relation")
                        if (relation.columnName != null || relation.unique) {
                            throw RuntimeException(
                                    "If @ToOne with foreign property used, @Column and @Unique are ignored. " +
                                            "See ${entity.name}.${relation.variable.name}")
                        }
                        source.addToOne(target, fkProperty, relation.variable.name)
                    }
                    else -> {
                        source.addToOneWithoutProperty(
                                relation.variable.name,
                                target,
                                relation.columnName ?: DaoUtil.dbName(relation.variable.name),
                                relation.isNotNull,
                                relation.unique
                        )
                    }
                }
            }
        }
    }

    private fun resolveToManyRelations(mapping: Map<ParsedEntity, Entity>, entities: Iterable<ParsedEntity>, schema: Schema) {
        entities.filterNot {
            it.manyRelations.isEmpty()
        }.forEach {
            entity ->
            val source = mapping[entity]!!
            try {
                entity.manyRelations.forEach {
                    relation ->
                    if (relation.variable.type.name != "java.util.List") {
                        throw RuntimeException("Can't create 1-M relation for ${entity.name} " +
                                "on ${relation.variable.type.name} ${relation.variable.name}. " +
                                "ToMany only supports java.util.List<T>")
                    }
                    val argument = relation.variable.type.typeArguments?.singleOrNull()
                            ?: throw RuntimeException("Can't create 1-M relation on ${relation.variable.name}. " +
                            "ToMany type should have specified exactly one type argument")

                    val target = schema.entities.find {
                        it.className == argument.simpleName
                    } ?: throw RuntimeException("${argument.name} is not an entity, but it is referenced " +
                            "for @ToMany relation in class (field: ${relation.variable.name})")

                    val options = if (relation.joinEntitySpec != null) 1 else 0 +
                            if (relation.mappedBy != null) 1 else 0 +
                                    if (relation.joinOnProperties.isNotEmpty()) 1 else 0
                    if (options != 1) {
                        throw RuntimeException("Can't create 1-M relation on ${relation.variable.name}. " +
                                "Either referencedJoinProperty, joinProperties or @JoinEntity must be used to describe the relation")
                    }
                    val toMany = when {
                        relation.mappedBy != null -> {
                            val relativeProperty = target.findProperty(relation.mappedBy)
                            source.addToMany(target, relativeProperty, relation.variable.name)
                        }
                        relation.joinOnProperties.isNotEmpty() -> {
                            val joinOn = relation.joinOnProperties
                            source.addToMany(
                                    joinOn.map { source.findProperty(it.source) }.toTypedArray(),
                                    target,
                                    joinOn.map { target.findProperty(it.target) }.toTypedArray()
                            ).apply {
                                name = relation.variable.name
                            }
                        }
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
                            source.addToMany(
                                    target,
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
                                    Order.ASC -> toMany.orderAsc(target.findProperty(it.name))
                                    Order.DESC -> toMany.orderDesc(target.findProperty(it.name))
                                }
                            }
                        } else {
                            val pkProperty = target.properties.find { it.isPrimaryKey }
                                    ?: throw RuntimeException("@OrderBy used to order by primary key of " +
                                    "entity (${target.className}) without primary key")
                            toMany.orderAsc(pkProperty)
                        }
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Can't process ${entity.name}: ${e.message}", e)
            }
        }
    }

    private fun convertProperty(entity: Entity, property: ParsedProperty, modelIds: org.greenrobot.idsync.Property) {
        val propertyType = convertPropertyType((property.customType?.columnJavaType ?: property.variable.type).name)
        val propertyBuilder = entity.addProperty(propertyType, property.variable.name)
        propertyBuilder.modelId(modelIds.id)
        propertyBuilder.modelRefId(modelIds.refId)
        if(modelIds.indexId != null && modelIds.indexId != 0) {
            propertyBuilder.modelIndexId(modelIds.indexId);
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
            propertyBuilder.indexAsc(property.index.name, property.index.unique)
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