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

package io.objectbox.generator.idsync

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import io.objectbox.generator.IdUid
import io.objectbox.generator.model.Schema
import io.objectbox.generator.model.ToManyStandalone
import io.objectbox.logging.log
import io.objectbox.model.EntityFlags
import okio.Buffer
import okio.Okio
import org.greenrobot.essentials.collections.LongHashSet
import org.jetbrains.annotations.TestOnly
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class IdSync(val jsonFile: File = File("objectmodel.json")) {
    private val noteSeeDocs = "Please read the docs how to resolve this."

    // public for tests to delete
    val backupFile = File(jsonFile.absolutePath + ".bak")

    private val modelJsonAdapter: JsonAdapter<IdSyncModel>

    private val uidHelper = UidHelper()

    private val entitiesReadByUid = HashMap<Long, Entity>()
    private val entitiesReadByName = HashMap<String, Entity>()
    private val parsedUids = LongHashSet()

    private var modelRead: IdSyncModel? = null

    var lastEntityId: IdUid = IdUid()
        private set

    var lastIndexId: IdUid = IdUid()
        private set

    var lastRelationId: IdUid = IdUid()
        private set

    var lastSequenceId: IdUid = IdUid()
        private set

    // visibility for test
    val newUidPool = mutableSetOf<Long>()

    private val retiredEntityUids = ArrayList<Long>()
    private val retiredPropertyUids = ArrayList<Long>()
    private val retiredIndexUids = ArrayList<Long>()
    private val retiredRelationUids = ArrayList<Long>()

    // Use IdentityHashMap here to avoid collisions (e.g. same name)
    private val entitiesBySchemaEntity = IdentityHashMap<io.objectbox.generator.model.Entity, Entity>()

    // Use IdentityHashMap here to avoid collisions (e.g. same name)
    private val propertiesBySchemaProperty = IdentityHashMap<io.objectbox.generator.model.Property, Property>()

    companion object {
        const val MIN_VERSION = 2
        const val MAX_VERSION = IdSyncModel.MODEL_VERSION
    }

    class ModelIdAdapter {
        // Writing [0:0] for empty "last ID" values is OK, null would confuse Kotlin with its non-null types
        @ToJson
        fun toJson(modelId: IdUid) = modelId.toString()

        @FromJson
        fun fromJson(id: String) = IdUid(id)
    }

    init {
        val moshi = Moshi.Builder().add(ModelIdAdapter()).build()
        modelJsonAdapter = IdSyncModelJsonAdapter(moshi)
        initModel()
    }

    private fun initModel() {
        try {
            val idSyncModel = justRead()
            if (idSyncModel != null) {
                if (idSyncModel.modelVersion < MIN_VERSION) {
                    throw IdSyncException("The model version is too old: minimum version is $MIN_VERSION, " +
                            "but found ${idSyncModel.modelVersion}. " +
                            "The model files was generated by an old version that is not supported anymore.")
                }
                if (idSyncModel.modelVersion > MAX_VERSION) {
                    // introduced with version 4, so can be null
                    val parserMinimum = idSyncModel.modelVersionParserMinimum ?: 0
                    if (parserMinimum == 0L || parserMinimum > IdSyncModel.MODEL_VERSION) {
                        throw IdSyncException(
                                "The model is too new: maximum supported version is $MAX_VERSION, " +
                                        "but found ${idSyncModel.modelVersion}. " +
                                        "The model files was generated by a newer version and has incompatible changes.")
                    }
                }
                validateIds(idSyncModel)
                modelRead = idSyncModel
                lastEntityId = idSyncModel.lastEntityId
                // version 2 did not have this, provide non-null
                lastRelationId = idSyncModel.lastRelationId ?: IdUid()
                lastIndexId = idSyncModel.lastIndexId
                lastSequenceId = idSyncModel.lastSequenceId
                retiredEntityUids += idSyncModel.retiredEntityUids ?: emptyList()
                retiredPropertyUids += idSyncModel.retiredPropertyUids ?: emptyList()
                retiredIndexUids += idSyncModel.retiredIndexUids ?: emptyList()
                retiredRelationUids += idSyncModel.retiredRelationUids ?: emptyList()
                newUidPool += idSyncModel.newUidPool ?: emptyList()
                uidHelper.addExistingIds(retiredEntityUids)
                uidHelper.addExistingIds(retiredPropertyUids)
                uidHelper.addExistingIds(retiredIndexUids)
                uidHelper.addExistingIds(retiredRelationUids)
                idSyncModel.entities.forEach { entity ->
                    uidHelper.addExistingId(entity.uid)
                    entity.properties.forEach { uidHelper.addExistingId(it.uid) }
                    entitiesReadByUid[entity.uid] = entity
                    if (entitiesReadByName.put(entity.name.toLowerCase(), entity) != null) {
                        throw IdSyncException("Malformed model file \"${jsonFile.name}\": " +
                                "duplicate entity name ${entity.name} - please edit the file to resolve the issue")
                    }
                }
            }
        } catch (e: Throwable) {
            // Repeat e.message so it shows up in gradle right away
            val message = "Could not load object model ID file \"${jsonFile.absolutePath}\": ${e.message}"
            throw IdSyncException(message, e)
        }
    }

    private fun validateIds(model: IdSyncModel) {
        val entityIds = mutableSetOf<Int>()
        model.entities.forEach { entity ->
            if (!entityIds.add(entity.id.id)) {
                throw IdSyncException("Duplicate ID ${entity.id.id} for entity ${entity.name}. $noteSeeDocs")
            }
            if (entity.modelId == model.lastEntityId.id) {
                if (entity.uid != model.lastEntityId.uid) {
                    throw IdSyncException("Entity ${entity.name} ID ${entity.id} does not match UID of lastEntityId " +
                            "${model.lastEntityId}. $noteSeeDocs")
                }
            } else if (entity.modelId > model.lastEntityId.id) {
                throw IdSyncException("Entity ${entity.name} has an ID ${entity.id} above lastEntityId " +
                        "${model.lastEntityId}. $noteSeeDocs")
            }

            val propertyIds = mutableSetOf<Int>()
            entity.properties.forEach { property ->
                if (!propertyIds.add(property.id.id)) {
                    throw IdSyncException("Duplicate ID ${property.id.id} for property " +
                            "${entity.name}.${property.name}. $noteSeeDocs")
                }
                if (property.modelId == entity.lastPropertyId.id) {
                    if (property.uid != entity.lastPropertyId.uid) {
                        throw IdSyncException("Property ${entity.name}.${property.name} ID ${property.id}" +
                                " does not match UID of lastPropertyId ${entity.lastPropertyId}. $noteSeeDocs")
                    }
                } else if (property.modelId > entity.lastPropertyId.id) {
                    throw IdSyncException("Property ${entity.name}.${property.name} has an ID ${property.id} above " +
                            "lastPropertyId ${entity.lastPropertyId}. $noteSeeDocs")
                }
            }
        }
    }

    fun sync(schema: Schema) {
        if (!schema.isFinished) {
            throw IllegalStateException("Must call schema.finish() first")
        }

        if (entitiesBySchemaEntity.isNotEmpty() || propertiesBySchemaProperty.isNotEmpty()) {
            throw IllegalStateException("May be called only once")
        }

        val schemaEntities = schema.entities
        try {
            // ensure IdUid for all entities in schema, needed by relations sync
            val existingEntities = schemaEntities.mapNotNull { syncEntityIdUids(it) }.associateBy { it.uid }

            val entities = schemaEntities.map {
                syncEntity(it, existingEntities[it.modelUid])
            }.sortedBy { it.id.id }

            updateRetiredUids(entities)
            writeModel(entities)
        } catch (e: Throwable) {
            if (e is IdSyncPrintUidException) {
                try {
                    // At this point model was already validated (at least partially)
                    val model = justRead()!!
                    model.newUidPool = listOf(e.randomNewUid)
                    writeModel(model)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                throw e
            }
            // Repeat e.message so it shows up in gradle right away
            val message = "Could not sync parsed model with ID model file \"${jsonFile.absolutePath}\": ${e.message}"
            throw IdSyncException(message, e)
        }

        // update schema with new IDs
        schema.lastEntityId = lastEntityId
        schema.lastIndexId = lastIndexId
        schema.lastRelationId = lastRelationId
    }

    private fun syncEntityIdUids(schemaEntity: io.objectbox.generator.model.Entity): Entity? {
        val entityName = schemaEntity.dbName ?: schemaEntity.className
        val entityUid = schemaEntity.modelUid
        val printUid = entityUid == -1L
        if (entityUid != null && !printUid && !parsedUids.add(entityUid)) {
            throw IdSyncException("Non-unique UID $entityUid in parsed entity " +
                    "${schemaEntity.javaPackage}.${schemaEntity.className}")
        }

        val existingEntity: Entity? = findEntity(entityName, entityUid)

        if (printUid) {
            if (existingEntity != null) {
                throw IdSyncPrintUidException("entity \"$entityName\"", existingEntity.uid, uidHelper.create())
            } else {
                throw IdSyncException("Cannot use @Uid without a value for a new entity: $entityName")
            }
        }

        val sourceId = if (existingEntity?.id == null) {
            lastEntityId.incId(newUid(entityUid)) // create new id
        } else {
            existingEntity.id // use existing id + uid
        }

        // update schema entity
        schemaEntity.modelId = sourceId.id
        schemaEntity.modelUid = sourceId.uid

        return existingEntity
    }

    private fun syncEntity(schemaEntity: io.objectbox.generator.model.Entity, existingEntity: Entity?): Entity {
        // Validate flags changes.
        if (existingEntity != null) {
            val oldFlags = existingEntity.flags ?: 0
            val newFlags = schemaEntity.entityFlagsForModelFile ?: 0
            if (oldFlags != newFlags) {
                // New or old flags contain SYNC_ENABLED?
                val oldSyncEnabled = oldFlags.and(EntityFlags.SYNC_ENABLED) != 0
                val newSyncEnabled = newFlags.and(EntityFlags.SYNC_ENABLED) != 0
                if (oldSyncEnabled != newSyncEnabled) {
                    throw IdSyncException("Can't change Sync annotation for existing entity '${schemaEntity.getName()}'.")
                }

                // New or old flags contain SHARED_GLOBAL_IDS?
                val oldSyncSharedGlobalIds = oldFlags.and(EntityFlags.SHARED_GLOBAL_IDS) != 0
                val newSyncSharedGlobalIds = newFlags.and(EntityFlags.SHARED_GLOBAL_IDS) != 0
                if (oldSyncSharedGlobalIds != newSyncSharedGlobalIds) {
                    throw IdSyncException("Can't change Sync.sharedGlobalIds setting for existing entity '${schemaEntity.getName()}'.")
                }
            }
        }

        val lastPropertyId = if (existingEntity?.lastPropertyId == null) {
            IdUid() // create empty id + uid
        } else {
            existingEntity.lastPropertyId.clone() // use existing id + uid
        }
        val properties = syncProperties(schemaEntity, existingEntity, lastPropertyId)
        val relations = syncRelations(schemaEntity, existingEntity)

        val entity = Entity(
                name = schemaEntity.getName(),
                id = IdUid(schemaEntity.modelId, schemaEntity.modelUid),
                flags = schemaEntity.entityFlagsForModelFile,
                properties = properties,
                relations = relations,
                lastPropertyId = lastPropertyId
        )
        // update schema entity
        schemaEntity.lastPropertyId = entity.lastPropertyId

        entitiesBySchemaEntity[schemaEntity] = entity
        return entity
    }

    /**
     * Returns [io.objectbox.generator.model.Entity.getDbName] or if null
     * [io.objectbox.generator.model.Entity.getClassName].
     */
    private fun io.objectbox.generator.model.Entity.getName(): String {
        return dbName ?: className
    }

    private fun syncProperties(schemaEntity: io.objectbox.generator.model.Entity, existingEntity: Entity?,
                               lastPropertyId: IdUid): ArrayList<Property> {
        val properties = ArrayList<Property>()
        for (parsedProperty in schemaEntity.properties) {
            val property = syncProperty(existingEntity, schemaEntity, parsedProperty, lastPropertyId)
            if (property.modelId > lastPropertyId.id) {
                lastPropertyId.set(property.id)
            }
            properties.add(property)
        }
        properties.sortBy { it.id.id }
        return properties
    }

    private fun syncProperty(existingEntity: Entity?, schemaEntity: io.objectbox.generator.model.Entity,
                             schemaProperty: io.objectbox.generator.model.Property, lastPropertyId: IdUid): Property {
        val name = schemaProperty.dbName ?: schemaProperty.propertyName
        val propertyUid: Long? = schemaProperty.modelId?.uid
        val printUid = propertyUid == -1L
        var existingProperty: Property? = null
        if (existingEntity != null) {
            if (propertyUid != null && !printUid && !parsedUids.add(propertyUid)) {
                throw IdSyncException("Non-unique UID $propertyUid in parsed entity " +
                        "${schemaEntity.javaPackage}.${schemaEntity.className} " +
                        "for property ${schemaProperty.propertyName}")
            }
            existingProperty = findProperty(existingEntity, name, propertyUid)
        }
        if (printUid) {
            val propertyName = "\"${schemaEntity.className}.${schemaProperty.propertyName}\""
            if (existingProperty != null) {
                throw IdSyncPrintUidException("property $propertyName", existingProperty.uid, uidHelper.create())
            } else {
                throw IdSyncException("Cannot use @Uid without a value for a new property: $propertyName")
            }
        }

        var sourceIndexId: IdUid? = null
        // check entity for index as Property.index is only auto-set for to-ones
        val index = schemaEntity.indexes.find { it.properties.size == 1 && it.properties[0] == schemaProperty }
        if (index != null) {
            sourceIndexId = existingProperty?.indexId ?: lastIndexId.incId(uidHelper.create())
        }

        val sourceId = if (existingProperty?.id == null) {
            lastPropertyId.incId(newUid(propertyUid)) // create a new id
        } else {
            existingProperty.id // use existing id + uid
        }
        val property = Property(
                name = name,
                id = sourceId.clone(),
                indexId = sourceIndexId?.clone(),
                type = schemaProperty.dbTypeId.toInt(),
                flags = if (schemaProperty.propertyFlags != 0) schemaProperty.propertyFlags else null,
                relationTarget = schemaProperty.targetEntity?.dbName
        )

        // update schema property
        schemaProperty.modelId = property.id
        schemaProperty.modelIndexId = property.indexId

        val collision = propertiesBySchemaProperty.put(schemaProperty, property)
        if (collision != null) {
            throw IllegalStateException("Property collision: $schemaProperty vs. $collision")
        }
        return property
    }

    /** Check a given UID against newUidPool or create a new UID. UID will be removed from pool if found. */
    private fun newUid(uidCandidate: Long?): Long {
        if (uidCandidate != null) {
            if (!newUidPool.remove(uidCandidate)) {
                throw IdSyncException("Unexpected UID $uidCandidate was not in newUidPool")
            }
        }
        return uidCandidate ?: uidHelper.create()
    }

    private fun syncRelations(schemaEntity: io.objectbox.generator.model.Entity, existingEntity: Entity?)
            : ArrayList<Relation> {
        val relations = ArrayList<Relation>()
        for (schemaRelation in schemaEntity.toManyRelations.filterIsInstance<ToManyStandalone>()) {
            val relation = syncRelation(existingEntity, schemaEntity, schemaRelation)
            if (relation.modelId > lastRelationId.id) {
                lastRelationId.set(relation.id)
            }
            relations.add(relation)
        }
        relations.sortBy { it.id.id }
        return relations
    }

    private fun syncRelation(existingEntity: Entity?, schemaEntity: io.objectbox.generator.model.Entity,
                             schemaRelation: ToManyStandalone): Relation {
        val name = schemaRelation.dbName
        val relationUid = schemaRelation.modelId?.uid
        val printUid = relationUid == -1L
        var existingRelation: Relation? = null
        if (existingEntity != null) {
            if (relationUid != null && !printUid && !parsedUids.add(relationUid)) {
                throw IdSyncException("Non-unique UID $relationUid in parsed entity " +
                        "${schemaEntity.javaPackage}.${schemaEntity.className} " +
                        "for relation ${schemaRelation.name}")
            }
            existingRelation = findRelation(existingEntity, name, relationUid)
        }

        if (printUid) {
            val relationName = "\"${schemaEntity.className}.${schemaRelation.name}\""
            if (existingRelation != null) {
                throw IdSyncPrintUidException("relation $relationName", existingRelation.uid, uidHelper.create())
            } else {
                throw IdSyncException("Cannot use @Uid without a value for a new relation: $relationName")
            }
        }

        val sourceId = if (existingRelation?.id == null) {
            lastRelationId.incId(newUid(relationUid)) // create a new id
        } else {
            existingRelation.id // use existing id + uid
        }
        val relation = Relation(
                name = name,
                id = sourceId.clone(),
                // issue: schemaRelation.targetEntity might not have modelId or modelUid set by now
                targetId = IdUid(schemaRelation.targetEntity.modelId, schemaRelation.targetEntity.modelUid)
        )

        // update schema property
        schemaRelation.modelId = relation.id
        return relation
    }

    /**
     * Just reads the model without changing any state of this object. Nice for testing also.
     */
    fun justRead(file: File = jsonFile): IdSyncModel? {
        if (!jsonFile.exists() || jsonFile.length() == 0L) { // Temp files have a 0 bytes length
            return null
        }
        return try {
            Okio.source(file).use { modelJsonAdapter.fromJson(Okio.buffer(it)) }
        } catch (e: FileNotFoundException) {
            null
        }
    }

    fun findEntity(name: String, uid: Long?): Entity? {
        if (uid != null && uid != 0L && uid != -1L) {
            return entitiesReadByUid[uid] ?:
                    if (newUidPool.contains(uid)) return null
                    else throw IdSyncException("No entity with UID $uid found")
        } else {
            return entitiesReadByName[name.toLowerCase()]
        }
    }

    fun findProperty(entity: Entity, name: String, uid: Long?): Property? {
        if (uid != null && uid != 0L && uid != -1L) {
            val filtered = entity.properties.filter { it.uid == uid }
            if (filtered.isEmpty()) {
                if (newUidPool.contains(uid)) {
                    return null
                }
                throw IdSyncException("In entity ${entity.name}, no property with UID $uid found")
            }
            check(filtered.size == 1) { "property name: $name, UID: $uid" }
            return filtered.first()
        } else {
            val nameLowerCase = name.toLowerCase()
            val filtered = entity.properties.filter { it.name.toLowerCase() == nameLowerCase }
            check(filtered.size <= 1) { "size: ${filtered.size} property name: $name, UID: $uid" }
            return if (filtered.isNotEmpty()) filtered.first() else null
        }
    }

    fun findRelation(entity: Entity, name: String, uid: Long?): Relation? {
        if (entity.relations == null) return null
        if (uid != null && uid != 0L && uid != -1L) {
            val filtered = entity.relations.filter { it.uid == uid }
            if (filtered.isEmpty()) {
                if (newUidPool.contains(uid)) {
                    return null
                }
                throw IdSyncException("In entity ${entity.name}, no relation with UID $uid found")
            }
            check(filtered.size == 1) { "relation name: $name, UID: $uid" }
            return filtered.first()
        } else {
            val nameLowerCase = name.toLowerCase()
            val filtered = entity.relations.filter { it.name.toLowerCase() == nameLowerCase }
            check(filtered.size <= 1) { "size: ${filtered.size} relation name: $name, UID: $uid" }
            return if (filtered.isNotEmpty()) filtered.first() else null
        }
    }

    private fun updateRetiredUids(entities: List<Entity>) {
        val oldEntityUids = entitiesReadByUid.keys.toMutableList()
        oldEntityUids.removeAll(entities.map { it.uid })
        retiredEntityUids.addAll(oldEntityUids)

        val oldPropertyUids = collectPropertyUids(entitiesReadByUid.values)
        val newPropertyUids = collectPropertyUids(entities)

        oldPropertyUids.first.removeAll(newPropertyUids.first)
        retiredPropertyUids.addAll(oldPropertyUids.first)

        oldPropertyUids.second.removeAll(newPropertyUids.second)
        retiredIndexUids.addAll(oldPropertyUids.second)

        oldPropertyUids.third.removeAll(newPropertyUids.third)
        retiredRelationUids.addAll(oldPropertyUids.third)
    }

    /** Collects a UID triple: property UIDs, index UIDs, and TODO relation UIDs.*/
    private fun collectPropertyUids(entities: Collection<Entity>)
            : Triple<MutableList<Long>, MutableList<Long>, MutableList<Long>> {
        val propertyUids = ArrayList<Long>()
        val indexUids = ArrayList<Long>()
        val relationUids = ArrayList<Long>()
        entities.forEach { entity ->
            entity.properties.forEach {
                propertyUids += it.uid
                if (it.indexId != null) {
                    indexUids += it.indexId.uid
                }
            }
            @Suppress("UNNECESSARY_SAFE_CALL") // read from JSON
            entity.relations?.forEach { relationUids += it.uid }
        }
        return Triple(propertyUids, indexUids, relationUids)
    }

    private fun writeModel(entities: List<Entity>) {
        val model = IdSyncModel(
                version = 1, // User-version
                modelVersion = IdSyncModel.MODEL_VERSION,
                modelVersionParserMinimum = IdSyncModel.MODEL_VERSION_PARSER_MINIMUM,
                lastEntityId = lastEntityId,
                lastIndexId = lastIndexId,
                lastRelationId = lastRelationId,
                lastSequenceId = lastSequenceId,
                newUidPool = null,
                entities = entities,
                retiredEntityUids = retiredEntityUids,
                retiredPropertyUids = retiredPropertyUids,
                retiredIndexUids = retiredIndexUids,
                retiredRelationUids = retiredRelationUids)
        writeModel(model)
        // Paranoia check, that synced model is OK (do this after writing because that's what the user sees)
        validateIds(model)
    }

    private fun writeModel(model: IdSyncModel) {
        validateBeforeWrite(model)
        val buffer = Buffer()
        val jsonWriter = JsonWriter.of(buffer)
        jsonWriter.indent = "  "
        model.modelVersion = IdSyncModel.MODEL_VERSION
        model.modelVersionParserMinimum = IdSyncModel.MODEL_VERSION_PARSER_MINIMUM
        modelJsonAdapter.toJson(jsonWriter, model)
        if (jsonFile.exists()) {
            val existingContent = jsonFile.readBytes()
            val content = buffer.snapshot().toByteArray()
            if (Arrays.equals(existingContent, content)) {
                log("ID model file unchanged: " + jsonFile.name)
                return
            } else {
                log("ID model file changed: " + jsonFile.name + ", creating backup (.bak)")
                jsonFile.copyTo(backupFile, true)
            }
        } else {
            log("ID model file created: " + jsonFile.name)
        }

        Okio.sink(jsonFile).use {
            buffer.readAll(it)
        }
    }

    /** We've seen duplicate names in written to the file before, so double check here.  */
    private fun validateBeforeWrite(model: IdSyncModel) {
        val entityNames = mutableSetOf<String>()
        for (entity in model.entities) {
            if (!entityNames.add(entity.name.toLowerCase())) {
                throw IdSyncException("Could not write model file \"${jsonFile.name}\" - verification failed: " +
                        "duplicate entity name \"${entity.name}\" (please report if you think this a bug)")
            }
            val propertyNames = mutableSetOf<String>()
            for (property in entity.properties) {
                if (!propertyNames.add(property.name.toLowerCase())) {
                    throw IdSyncException("Could not write model file \"${jsonFile.name}\" - verification failed: " +
                            "duplicate property name \"${property.name}\" in entity \"${entity.name}\" " +
                            "(please report if you think this a bug)")
                }
            }
        }
    }

    /** For unit testing only. */
    @TestOnly
    fun get(property: io.objectbox.generator.model.Property): Property {
        return propertiesBySchemaProperty[property] ?:
                throw IllegalStateException("No ID model property available for schema property ${property.propertyName}")
    }

}