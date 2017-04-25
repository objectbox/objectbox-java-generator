package io.objectbox.generator.idsync

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import okio.Okio
import okio.Source
import org.greenrobot.essentials.collections.LongHashSet
import io.objectbox.codemodifier.ParsedEntity
import io.objectbox.codemodifier.ParsedProperty
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import io.objectbox.generator.IdUid

class IdSync(val jsonFile: File = File("objectmodel.json")) {
    private val noteSeeDocs = "Please read the docs how to resolve this."

    // public for tests to delete
    val backupFile = File(jsonFile.absolutePath + ".bak")

    private val modelJsonAdapter: JsonAdapter<IdSyncModel>

    private val uidHelper = UidHelper()

    private val entitiesReadByRefId = HashMap<Long, Entity>()
    private val entitiesReadByName = HashMap<String, Entity>()
    private val parsedRefIds = LongHashSet()

    private var modelRead: IdSyncModel? = null

    var lastEntityId: IdUid = IdUid()
        private set

    var lastIndexId: IdUid = IdUid()
        private set

    var lastSequenceId: IdUid = IdUid()
        private set

    private val retiredEntityUids = ArrayList<Long>()
    private val retiredPropertyUids = ArrayList<Long>()
    private val retiredIndexUids = ArrayList<Long>()

    // Use IdentityHashMap here to avoid collisions (e.g. same name)
    private val entitiesByParsedEntity = IdentityHashMap<ParsedEntity, Entity>()

    // Use IdentityHashMap here to avoid collisions (e.g. same name)
    private val propertiesByParsedProperty = IdentityHashMap<ParsedProperty, Property>()

    class ModelIdAdapter {
        // Writing [0:0] for empty "last ID" values is OK, null would confuse Kotlin with its non-null types
        @ToJson fun toJson(modelId: IdUid) = modelId.toString()

        @FromJson fun fromJson(id: String) = IdUid(id)
    }

    init {
        val moshi = Moshi.Builder().add(ModelIdAdapter()).build()
        modelJsonAdapter = moshi.adapter<IdSyncModel>(IdSyncModel::class.java)
        initModel()
    }

    private fun initModel() {
        try {
            val idSyncModel = justRead()
            if (idSyncModel != null) {
                if (idSyncModel.modelVersion != 2) {
                    throw IdSyncException("This version requires model version 2, but found " +
                            idSyncModel.modelVersion +
                            ". If you just updated from 0.9.6 that is expected. $noteSeeDocs")
                }
                validateIds(idSyncModel)
                modelRead = idSyncModel
                lastEntityId = idSyncModel.lastEntityId
                lastIndexId = idSyncModel.lastIndexId
                lastSequenceId = idSyncModel.lastSequenceId
                retiredEntityUids += idSyncModel.retiredEntityUids ?: emptyList()
                retiredPropertyUids += idSyncModel.retiredPropertyUids ?: emptyList()
                retiredIndexUids += idSyncModel.retiredIndexUids ?: emptyList()
                uidHelper.addExistingIds(retiredEntityUids)
                uidHelper.addExistingIds(retiredPropertyUids)
                uidHelper.addExistingIds(retiredIndexUids)
                idSyncModel.entities.forEach {
                    uidHelper.addExistingId(it.uid)
                    it.properties.forEach { uidHelper.addExistingId(it.uid) }
                    entitiesReadByRefId.put(it.uid, it)
                    if (entitiesReadByName.put(it.name.toLowerCase(), it) != null) {
                        throw IdSyncException("Duplicate entity name ${it.name}")
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

    fun sync(parsedEntities: List<ParsedEntity>) {
        if (entitiesByParsedEntity.isNotEmpty() || propertiesByParsedProperty.isNotEmpty()) {
            throw IllegalStateException("May be called only once")
        }
        try {
            val entities = parsedEntities.map { syncEntity(it) }.sortedBy { it.id.id }
            updateRetiredRefIds(entities)
            val model = IdSyncModel(
                    version = 1,
                    modelVersion = 2,
                    lastEntityId = lastEntityId,
                    lastIndexId = lastIndexId,
                    lastSequenceId = lastSequenceId,
                    entities = entities,
                    retiredEntityUids = retiredEntityUids,
                    retiredPropertyUids = retiredPropertyUids,
                    retiredIndexUids = retiredIndexUids)
            writeModel(model)
            // Paranoia check, that synced model is OK (do this after writing because that's what the user sees)
            validateIds(model)
        } catch (e: Throwable) {
            // Repeat e.message so it shows up in gradle right away
            val message = "Could not sync parsed model with ID model file \"${jsonFile.absolutePath}\": ${e.message}"
            throw IdSyncException(message, e)
        }
    }

    private fun syncEntity(parsedEntity: ParsedEntity): Entity {
        val entityName = parsedEntity.dbName ?: parsedEntity.name
        val entityRefId = parsedEntity.uid
        if (entityRefId != null && !parsedRefIds.add(entityRefId)) {
            throw IdSyncException("Non-unique refId $entityRefId in parsed entity ${parsedEntity.name} in file " +
                    parsedEntity.sourceFile.absolutePath)
        }
        val existingEntity: Entity? = findEntity(entityName, entityRefId)
        val lastPropertyId = existingEntity?.lastPropertyId?.clone() ?: IdUid()
        val properties = ArrayList<Property>()
        for (parsedProperty in parsedEntity.properties) {
            val property = syncProperty(existingEntity, parsedEntity, parsedProperty, lastPropertyId)
            // update last id:uid if id is bigger (new property) or id is equal and uid has changed
            if (property.modelId > lastPropertyId.id
                    || (property.modelId == lastPropertyId.id && property.uid != lastPropertyId.uid)) {
                lastPropertyId.set(property.id)
            }
            properties.add(property)
        }
        properties.sortBy { it.id.id }

        val sourceId = existingEntity?.id ?: lastEntityId.incId(uidHelper.create())
        val entity = Entity(
                name = entityName,
                id = sourceId.clone(),
                properties = properties,
                lastPropertyId = lastPropertyId
        )
        entitiesByParsedEntity[parsedEntity] = entity
        return entity
    }

    private fun syncProperty(existingEntity: Entity?, parsedEntity: ParsedEntity, parsedProperty: ParsedProperty,
                             lastPropertyId: IdUid): Property {
        val name = parsedProperty.dbName ?: parsedProperty.variable.name
        var existingProperty: Property? = null
        if (existingEntity != null) {
            val propertyRefId = parsedProperty.uid
            if (propertyRefId != null && propertyRefId != -1L && !parsedRefIds.add(propertyRefId)) {
                throw IdSyncException("Non-unique refId $propertyRefId in parsed entity ${parsedEntity.name} " +
                        "and property ${parsedProperty.variable.name} in file " +
                        parsedEntity.sourceFile.absolutePath)
            }
            existingProperty = findProperty(existingEntity, name, propertyRefId)
        }

        var sourceIndexId: IdUid? = null
        if (parsedProperty.index != null) {
            sourceIndexId = existingProperty?.indexId ?: lastIndexId.incId(uidHelper.create())
        }

        val sourceId: IdUid
        if (existingProperty?.id == null) {
            sourceId = lastPropertyId.incId(uidHelper.create()) // create a new id + uid
        } else if (parsedProperty.uid == -1L) {
            // retire current uid
            retiredPropertyUids.add(existingProperty.id.uid)
            // create new uid + keep the id
            sourceId = existingProperty.id.set(existingProperty.id.id, uidHelper.create())
        } else {
            sourceId = existingProperty.id // use existing id + uid
        }
        val property = Property(
                name = name,
                id = sourceId.clone(),
                indexId = sourceIndexId?.clone()
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
        var source: Source? = null
        try {
            source = Okio.source(file)
            val syncModel = modelJsonAdapter.fromJson(Okio.buffer(source))
            return syncModel
        } catch (e: FileNotFoundException) {
            return null
        } finally {
            source?.close()
        }
    }

    fun findEntity(name: String, uid: Long?): Entity? {
        if (uid != null) {
            return entitiesReadByRefId[uid] ?:
                    throw IdSyncException("No entity with UID $uid found")
        } else {
            return entitiesReadByName[name.toLowerCase()]
        }
    }

    fun findProperty(entity: Entity, name: String, uid: Long?): Property? {
        if (uid != null && uid != -1L) {
            val filtered = entity.properties.filter { it.uid == uid }
            if (filtered.isEmpty()) {
                throw IdSyncException("In entity ${entity.name}, no property with UID $uid found")
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
        retiredEntityUids.addAll(oldEntityRefIds)

        val oldPropertyUids = collectPropertyUids(entitiesReadByRefId.values)
        val newPropertyUids = collectPropertyUids(entities)

        oldPropertyUids.first.removeAll(newPropertyUids.first)
        retiredPropertyUids.addAll(oldPropertyUids.first)

        oldPropertyUids.second.removeAll(newPropertyUids.second)
        retiredIndexUids.addAll(oldPropertyUids.second)
    }

    /** Collects a UID triple: property UIDs, index UIDs, and TODO relation UIDs.*/
    private fun collectPropertyUids(entities: Collection<Entity>)
            : Triple<MutableList<Long>, MutableList<Long>, MutableList<Long>> {
        val propertyUids = ArrayList<Long>()
        val indexUids = ArrayList<Long>()
        val relationUids = ArrayList<Long>()
        entities.forEach {
            it.properties.forEach {
                propertyUids += it.uid
                if (it.indexId != null) {
                    indexUids += it.indexId.uid
                }
            }
        }
        return Triple(propertyUids, indexUids, relationUids)
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