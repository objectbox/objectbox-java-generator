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

class IdSync(val jsonFile: File = File("objectmodel.json")) {
    val backupFile: File

    private val modelJsonAdapter: JsonAdapter<IdSyncModel>

    private val modelRefId: ModelRefId

    private val entitiesReadByRefId = HashMap<Long, Entity>()
    private val entitiesReadByName = HashMap<String, Entity>()
    private val parsedRefIds = LongHashSet()

    private var modelRead: IdSyncModel? = null

    private var lastEntityId: Int = 0
    private var lastIndexId: Int = 0
    private var lastSequenceId: Int = 0

    private val retiredEntityRefIds = ArrayList<Long>()
    private val retiredPropertyRefIds = ArrayList<Long>()

    private val entitiesByParsedEntity = HashMap<ParsedEntity, Entity>()
    private val propertiesByParsedProperty = HashMap<ParsedProperty, Property>()

    init {
        backupFile = File(jsonFile.absolutePath + ".bak")
        val moshi = Moshi.Builder().build()
        modelJsonAdapter = moshi.adapter<IdSyncModel>(IdSyncModel::class.java)
        modelRefId = ModelRefId()
        initModel()
    }

    private fun initModel() {
        try {
            modelRead = justRead()
            if (modelRead != null) {
                lastEntityId = modelRead!!.lastEntityId
                lastIndexId = modelRead!!.lastIndexId
                lastSequenceId = modelRead!!.lastSequenceId
                retiredEntityRefIds += modelRead!!.retiredEntityRefIds ?: emptyList()
                retiredPropertyRefIds += modelRead!!.retiredPropertyRefIds ?: emptyList()
                modelRefId.addExistingIds(retiredEntityRefIds)
                modelRefId.addExistingIds(retiredPropertyRefIds)
                modelRead!!.entities.forEach {
                    modelRefId.addExistingId(it.refId)
                    it.properties.forEach { modelRefId.addExistingId(it.refId) }
                    validateLastIds(it)
                    entitiesReadByRefId.put(it.refId, it)
                    if (entitiesReadByName.put(it.name.toLowerCase(), it) != null) {
                        throw IdSyncException("Duplicate entity name ${it.name} in " + jsonFile.absolutePath)
                    }
                }
            }
        } catch (e: Throwable) {
            throw IdSyncException("Loading object model ID file failed. Please check " + jsonFile.absolutePath, e)
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
        if (entitiesByParsedEntity.isNotEmpty() || propertiesByParsedProperty.isNotEmpty()) {
            throw IllegalStateException("May be called only once")
        }
        try {
            val entities = parsedEntities.map { syncEntity(it) }
            updateRetiredRefIds(entities)
            val model = IdSyncModel(
                    version = 1,
                    metaVersion = 1,
                    lastEntityId = lastEntityId,
                    lastIndexId = lastIndexId,
                    lastSequenceId = lastSequenceId,
                    entities = entities,
                    retiredEntityRefIds = retiredEntityRefIds,
                    retiredPropertyRefIds = retiredPropertyRefIds)
            writeModel(model)
        } catch (e: Throwable) {
            throw IdSyncException("Could not sync parsed model with ID model. Please check " + jsonFile.absolutePath, e)
        }
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

        val entity = Entity(
                name = entityName,
                id = existingEntity?.id ?: ++lastEntityId,
                refId = existingEntity?.refId ?: modelRefId.create(),
                properties = properties,
                lastPropertyId = lastPropertyId
        )
        entitiesByParsedEntity[parsedEntity] = entity
        return entity
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

        val property = Property(
                name = name,
                refId = existingProperty?.refId ?: modelRefId.create(),
                id = existingProperty?.id ?: lastPropertyId + 1,
                indexId = indexId
        )
        propertiesByParsedProperty[parsedProperty] = property
        return property
    }

    /**
     * Just reads the model without changing any state of this object. Nice for testing also.
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

    private fun updateRetiredRefIds(entities: List<Entity>) {
        val oldEntityRefIds = entitiesReadByRefId.keys.toMutableList()
        oldEntityRefIds.removeAll(entities.map { it.refId })
        retiredEntityRefIds.addAll(oldEntityRefIds)

        val oldPropertyRefIds = collectPropertyRefIds(entitiesReadByRefId.values)
        val newPropertyRefIds = collectPropertyRefIds(entities)
        oldPropertyRefIds.removeAll(newPropertyRefIds)
        retiredPropertyRefIds.addAll(oldPropertyRefIds)
    }

    private fun collectPropertyRefIds(entities: Collection<Entity>): MutableList<Long> {
        val propertyRefIds = ArrayList<Long>()
        entities.forEach {
            it.properties.forEach { propertyRefIds += it.refId }
        }
        return propertyRefIds
    }

    private fun writeModel(model: IdSyncModel) {
        val buffer = Buffer()
        val jsonWriter = JsonWriter.of(buffer)
        jsonWriter.setIndent("  ")
        modelJsonAdapter.toJson(jsonWriter, model)
        if (jsonFile.exists()) {
            jsonFile.copyTo(backupFile, true)
        }

        val sink = Okio.sink(jsonFile)
        try {
            buffer.readAll(sink)
        } finally {
            sink.close()
        }
    }

    fun get(parsedEntity: ParsedEntity): Entity {
        return entitiesByParsedEntity[parsedEntity] ?:
                throw IllegalStateException("No ID model entity available for parsed entity " + parsedEntity.name)
    }

    fun get(parsedProperty: ParsedProperty): Property {
        return propertiesByParsedProperty[parsedProperty] ?:
                throw IllegalStateException("No ID model property available for parsed property " +
                        parsedProperty.variable.name)
    }

}