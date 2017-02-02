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
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class IdSync(val jsonFile: File = File("objectmodel.json")) {
    val backupFile: File

    private val modelJsonAdapter: JsonAdapter<IdSyncModel>

    private val modelUid: ModelUid

    private val entitiesReadByRefId = HashMap<Long, Entity>()
    private val entitiesReadByName = HashMap<String, Entity>()
    private val parsedRefIds = LongHashSet()

    private var modelRead: IdSyncModel? = null

    var lastEntityId: Int = 0
        private set

    var lastIndexId: Int = 0
        private set

    var lastSequenceId: Int = 0
        private set

    private val retiredEntityRefIds = ArrayList<Long>()
    private val retiredPropertyRefIds = ArrayList<Long>()

    // Use IdentityHashMap here to avoid collisions (e.g. same name)
    private val entitiesByParsedEntity = IdentityHashMap<ParsedEntity, Entity>()

    // Use IdentityHashMap here to avoid collisions (e.g. same name)
    private val propertiesByParsedProperty = IdentityHashMap<ParsedProperty, Property>()

    class ModelIdAdapter {
        @ToJson fun toJson(modelId: IdUid) = modelId.toString()

        @FromJson fun fromJson(id: String) = IdUid(id)
    }

    init {
        backupFile = File(jsonFile.absolutePath + ".bak")
        val moshi = Moshi.Builder().add(ModelIdAdapter()).build()
        modelJsonAdapter = moshi.adapter<IdSyncModel>(IdSyncModel::class.java)
        modelUid = ModelUid()
        initModel()
    }

    private fun initModel() {
        try {
            modelRead = justRead()
            if (modelRead != null) {
                lastEntityId = modelRead!!.lastEntityId
                lastIndexId = modelRead!!.lastIndexId
                lastSequenceId = modelRead!!.lastSequenceId
                retiredEntityRefIds += modelRead!!.retiredEntityUids ?: emptyList()
                retiredPropertyRefIds += modelRead!!.retiredPropertyUids ?: emptyList()
                modelUid.addExistingIds(retiredEntityRefIds)
                modelUid.addExistingIds(retiredPropertyRefIds)
                modelRead!!.entities.forEach {
                    modelUid.addExistingId(it.uid)
                    it.properties.forEach { modelUid.addExistingId(it.uid) }
                    validateLastIds(it)
                    entitiesReadByRefId.put(it.uid, it)
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
        if (entity.modelId > lastEntityId) {
            throw IdSyncException("Entity ${entity.name} has an ID ${entity.id} above lastEntityId ${lastEntityId}" +
                    " in " + jsonFile.absolutePath)
        }
        entity.properties.forEach {
            if (it.modelId > entity.lastPropertyId) {
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
            val entities = parsedEntities.map { syncEntity(it) }.sortedBy { it.id.id }
            updateRetiredRefIds(entities)
            val model = IdSyncModel(
                    version = 1,
                    metaVersion = 1,
                    lastEntityId = lastEntityId,
                    lastIndexId = lastIndexId,
                    lastSequenceId = lastSequenceId,
                    entities = entities,
                    retiredEntityUids = retiredEntityRefIds,
                    retiredPropertyUids = retiredPropertyRefIds)
            writeModel(model)
        } catch (e: Throwable) {
            throw IdSyncException("Could not sync parsed model with ID model. Please check " + jsonFile.absolutePath, e)
        }
    }

    private fun syncEntity(parsedEntity: ParsedEntity): Entity {
        val entityName = parsedEntity.dbName ?: parsedEntity.name
        var entityRefId = parsedEntity.uid
        if (entityRefId != null && !parsedRefIds.add(entityRefId)) {
            throw IdSyncException("Non-unique refId $entityRefId in parsed entity ${parsedEntity.name} in file " +
                    parsedEntity.sourceFile.absolutePath)
        }
        var existingEntity = findEntity(entityName, entityRefId)
        var lastPropertyId = existingEntity?.lastPropertyId ?: 0
        val properties = ArrayList<Property>()
        for (parsedProperty in parsedEntity.properties) {
            val property = syncProperty(existingEntity, parsedEntity, parsedProperty, lastPropertyId)
            lastPropertyId = Math.max(lastPropertyId, property.modelId)
            properties.add(property)
        }
        properties.sortBy { it.id.id }

        val modelId = existingEntity?.modelId ?: ++lastEntityId
        val uid = existingEntity?.uid ?: modelUid.create()
        val entity = Entity(
                name = entityName,
                id = IdUid(modelId, uid),
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
            val propertyRefId = parsedProperty.uid
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

        val uid = existingProperty?.uid ?: modelUid.create()
        val modelId = existingProperty?.modelId ?: lastPropertyId + 1
        val property = Property(
                name = name,
                id = IdUid(modelId, uid),
                indexId = indexId
        )
        val collision = propertiesByParsedProperty.put(parsedProperty, property)
        if (collision != null) {
            throw IllegalStateException("Property collision: " + parsedProperty + " vs. " + collision)
        }
        return property
    }

    /**
     * Just reads the model without changing any state of this object. Nice for testing also.
     */
    fun justRead(file: File = jsonFile): IdSyncModel? {
        var source: Source? = null;
        try {
            source = Okio.source(file)
            val syncModel = modelJsonAdapter.fromJson(Okio.buffer(source))
            syncModel.entities.forEach {
                if (it.uid == 0L && it.refId != null) {
                    it.uid = it.refId
                }
            }
            return syncModel
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
            val filtered = entity.properties.filter { it.uid == refId }
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
        oldEntityRefIds.removeAll(entities.map { it.uid })
        retiredEntityRefIds.addAll(oldEntityRefIds)

        val oldPropertyRefIds = collectPropertyRefIds(entitiesReadByRefId.values)
        val newPropertyRefIds = collectPropertyRefIds(entities)
        oldPropertyRefIds.removeAll(newPropertyRefIds)
        retiredPropertyRefIds.addAll(oldPropertyRefIds)
    }

    private fun collectPropertyRefIds(entities: Collection<Entity>): MutableList<Long> {
        val propertyRefIds = ArrayList<Long>()
        entities.forEach {
            it.properties.forEach { propertyRefIds += it.uid }
        }
        return propertyRefIds
    }

    private fun writeModel(model: IdSyncModel) {
        val buffer = Buffer()
        val jsonWriter = JsonWriter.of(buffer)
        jsonWriter.setIndent("  ")
        modelJsonAdapter.toJson(jsonWriter, model)
        if (jsonFile.exists()) {
            val existingContent = jsonFile.readBytes()
            val content = buffer.snapshot().toByteArray()
            if (Arrays.equals(existingContent, content)) {
                println("ID model file unchanged: " + jsonFile.name)
                return
            } else {
                println("ID model file changed: " + jsonFile.name + ", creating backup (.bak)")
            }
            jsonFile.copyTo(backupFile, true)
        } else {
            println("ID model file created: " + jsonFile.name)
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