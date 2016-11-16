package org.greenrobot.entitymodel

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import okio.Okio
import okio.Source
import org.greenrobot.greendao.codemodifier.ParsedEntity
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class ModelSync(
        val jsonFile: File,
        val parsedEntities: List<ParsedEntity>) {

    private val modelJsonAdapter: JsonAdapter<Model>

    private val modelRefId: ModelRefId


    private val entitiesReadByRefId = HashMap<Long, Entity>()
    private val entitiesReadByName = HashMap<String, Entity>()

    private var modelRead: Model? = null

    var lastEntityId: Int = 0
    var lastIndexId: Int = 0
    var lastSequenceId: Int = 0

    init {
        val moshi = Moshi.Builder().build()
        modelJsonAdapter = moshi.adapter<Model>(Model::class.java)
        modelRefId = ModelRefId()
        initModel()
    }

    fun sync() {
        val entitiesModel = ArrayList<org.greenrobot.entitymodel.Entity>()

        for (parsedEntity in parsedEntities) {
            val entityName = parsedEntity.name
            var entityRefId = parsedEntity.refId
            var existingEntity = findEntity(entityName, entityRefId)
            if (entityRefId == null) {
                entityRefId = existingEntity?.refId ?: modelRefId.create()
            }
            var propertyId = 1
            val properties = ArrayList<Property>()
            for (parsedProperty in parsedEntity.properties) {
                val name = parsedProperty.dbName ?: parsedProperty.variable.name
                if (existingEntity != null) {
                    val existingProperty = findProperty(existingEntity, name, null)
                }
                val refId = modelRefId.create()
                val property = Property(name = name, id = propertyId, refId = refId)
                propertyId++
                properties.add(property)
            }

            val modelEntity = org.greenrobot.entitymodel.Entity(
                    name = entityName,
                    id = ++lastEntityId, refId = entityRefId,
                    properties = properties,
                    lastPropertyId = propertyId - 1)
            entitiesModel.add(modelEntity)
        }

        val model = Model(
                version = 1,
                metaVersion = 1,
                lastEntityId = lastEntityId,
                lastIndexId = 0,
                lastSequenceId = 0,
                entities = entitiesModel)

        writeModel(model)
    }


    private fun initModel() {
        var source: Source? = null;
        try {
            source = Okio.source(jsonFile)
            modelRead = modelJsonAdapter.fromJson(Okio.buffer(source))
        } catch (e: FileNotFoundException) {
        } finally {
            source?.close()
        }
        modelRead?.entities?.forEach {
            if (!modelRefId.addExistingId(it.refId)) {
                throw RuntimeException("Duplicate ref ID ${it.refId} in " + jsonFile.absolutePath)
            }
            entitiesReadByRefId.put(it.refId, it)
            if (entitiesReadByName.put(it.name, it) != null) {
                throw RuntimeException("Duplicate entity name ${it.name} in " + jsonFile.absolutePath)
            }
        }
    }

    private fun findEntity(name: String, refId: Long?): Entity? {
        if (refId != null) {
            return entitiesReadByRefId[refId] ?:
                    throw RuntimeException("No entity with refID $refId found in " + jsonFile.absolutePath)
        } else {
            return entitiesReadByName[name]
        }
    }

    private fun findProperty(entity: Entity, name: String, refId: Long?): Property? {
        if (refId != null) {
            val filtered = entity.properties.filter { it.refId == refId }
            if (filtered.isEmpty()) {
                throw RuntimeException("In entity ${entity.name}, no property with refID $refId found in " +
                        jsonFile.absolutePath)
            }
            check(filtered.size == 1)
            return filtered.first()
        } else {
            val filtered = entity.properties.filter { it.name == name }
            check(filtered.size <= 1)
            return if (filtered.isNotEmpty()) filtered.first() else null
        }
    }


    private fun writeModel(model: Model) {
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