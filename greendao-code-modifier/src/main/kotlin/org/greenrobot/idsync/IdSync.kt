package org.greenrobot.idsync

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import okio.Okio
import okio.Source
import org.greenrobot.essentials.collections.LongHashSet
import org.greenrobot.greendao.codemodifier.ParsedEntity
import org.greenrobot.greendao.codemodifier.ParsedProperty
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class IdSync(val jsonFile: File) {

    private val modelJsonAdapter: JsonAdapter<IdSyncModel>

    private val modelRefId: ModelRefId

    private val entitiesReadByRefId = HashMap<Long, Entity>()
    private val entitiesReadByName = HashMap<String, Entity>()
    private val parsedRefIds = LongHashSet()

    private var modelRead: IdSyncModel? = null

    var lastEntityId: Int = 0
    var lastIndexId: Int = 0
    var lastSequenceId: Int = 0

    init {
        val moshi = Moshi.Builder().build()
        modelJsonAdapter = moshi.adapter<IdSyncModel>(IdSyncModel::class.java)
        modelRefId = ModelRefId()
        initModel()
    }

    private fun initModel() {
        modelRead = justRead()
        if (modelRead != null) {
            lastEntityId = modelRead!!.lastEntityId
            lastIndexId = modelRead!!.lastIndexId
            lastSequenceId = modelRead!!.lastSequenceId
            modelRead!!.entities.forEach {
                if (!modelRefId.addExistingId(it.refId)) {
                    throw IdSyncException("Duplicate ref ID ${it.refId} in " + jsonFile.absolutePath)
                }
                validateLastIds(it)
                entitiesReadByRefId.put(it.refId, it)
                if (entitiesReadByName.put(it.name.toLowerCase(), it) != null) {
                    throw IdSyncException("Duplicate entity name ${it.name} in " + jsonFile.absolutePath)
                }
            }
        }
    }

    private fun validateLastIds(entity: Entity) {
        if (entity.id > lastEntityId) {
            throw IdSyncException("Entity ${entity.name} has an ID ${entity.id} above lastEntityId ${lastEntityId}" +
                    " in " + jsonFile.absolutePath)
        }
        entity.properties.forEach {
            if (it.id > entity.lastPropertyId) {
                throw IdSyncException("Property ${entity.name}.${it.name} has an ID ${it.id} above " +
                        "lastPropertyId ${entity.lastPropertyId} in " + jsonFile.absolutePath)
            }
        }
    }

    fun sync(parsedEntities: List<ParsedEntity>) {
        val entities = parsedEntities.map { syncEntity(it) }
        val model = IdSyncModel(
                version = 1,
                metaVersion = 1,
                lastEntityId = lastEntityId,
                lastIndexId = lastIndexId,
                lastSequenceId = lastSequenceId,
                entities = entities,
                deletedEntities = emptyList(),
                deletedProperties = emptyList())
        writeModel(model)
    }

    private fun syncEntity(parsedEntity: ParsedEntity): Entity {
        val entityName = parsedEntity.dbName ?: parsedEntity.name
        var entityRefId = parsedEntity.refId
        if (entityRefId != null && !parsedRefIds.add(entityRefId)) {
            throw IdSyncException("Non-unique refId $entityRefId in parsed entity ${parsedEntity.name} in file " +
                    parsedEntity.sourceFile.absolutePath)
        }
        var existingEntity = findEntity(entityName, entityRefId)
        var lastPropertyId = existingEntity?.lastPropertyId ?: 0
        val properties = ArrayList<Property>()
        for (parsedProperty in parsedEntity.properties) {
            val property = syncProperty(existingEntity, parsedEntity, parsedProperty, lastPropertyId)
            lastPropertyId = Math.max(lastPropertyId, property.id)
            properties.add(property)
        }

        return Entity(
                name = entityName,
                id = existingEntity?.id ?: ++lastEntityId,
                refId = existingEntity?.refId ?: modelRefId.create(),
                properties = properties,
                lastPropertyId = lastPropertyId
        )
    }

    private fun syncProperty(existingEntity: Entity?, parsedEntity: ParsedEntity, parsedProperty: ParsedProperty,
                             lastPropertyId: Int): Property {
        val name = parsedProperty.dbName ?: parsedProperty.variable.name
        var existingProperty: Property? = null
        if (existingEntity != null) {
            val propertyRefId = parsedProperty.refId
            if (propertyRefId != null && !parsedRefIds.add(propertyRefId)) {
                throw IdSyncException("Non-unique refId $propertyRefId in parsed entity ${parsedEntity.name} " +
                        "and property ${parsedProperty.variable.name} in file " +
                        parsedEntity.sourceFile.absolutePath)
            }
            existingProperty = findProperty(existingEntity, name, propertyRefId)
        }

        var indexId: Int? = null
        if (parsedProperty.index != null) {
            indexId = existingProperty?.indexId
            if (indexId == null) {
                indexId = ++lastIndexId
            }
        }

        return Property(
                name = name,
                refId = existingProperty?.refId ?: modelRefId.create(),
                id = existingProperty?.id ?: lastPropertyId + 1,
                indexId = indexId
        )
    }

    /**
     * Justs reads the model without changing any state of this object. Nice for testing.
     */
    fun justRead(file: File = jsonFile): IdSyncModel? {
        var source: Source? = null;
        try {
            source = Okio.source(file)
            return modelJsonAdapter.fromJson(Okio.buffer(source))
        } catch (e: FileNotFoundException) {
            return null
        } finally {
            source?.close()
        }
    }


    private fun findEntity(name: String, refId: Long?): Entity? {
        if (refId != null) {
            return entitiesReadByRefId[refId] ?:
                    throw IdSyncException("No entity with refID $refId found in " + jsonFile.absolutePath)
        } else {
            return entitiesReadByName[name.toLowerCase()]
        }
    }

    private fun findProperty(entity: Entity, name: String, refId: Long?): Property? {
        if (refId != null) {
            val filtered = entity.properties.filter { it.refId == refId }
            if (filtered.isEmpty()) {
                throw IdSyncException("In entity ${entity.name}, no property with refID $refId found in " +
                        jsonFile.absolutePath)
            }
            check(filtered.size == 1)
            return filtered.first()
        } else {
            val nameLowerCase = name.toLowerCase()
            val filtered = entity.properties.filter { it.name.toLowerCase() == nameLowerCase }
            check(filtered.size <= 1)
            return if (filtered.isNotEmpty()) filtered.first() else null
        }
    }


    private fun writeModel(model: IdSyncModel) {
        val buffer = Buffer()
        val jsonWriter = JsonWriter.of(buffer)
        jsonWriter.setIndent("  ")
        modelJsonAdapter.toJson(jsonWriter, model)
        val sink = Okio.sink(jsonFile)
        try {
            buffer.readAll(sink)
        } finally {
            sink.close()
        }
    }

}