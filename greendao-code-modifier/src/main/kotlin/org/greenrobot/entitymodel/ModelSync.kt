package org.greenrobot.entitymodel

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import okio.Okio
import okio.Source
import org.greenrobot.greendao.codemodifier.EntityClass
import org.greenrobot.greendao.generator.Schema
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class ModelSync(
        val jsonFile: File,
        val entitiesParser: List<EntityClass>,
        val schemaGenerator: Schema,
        val entitiesGenerator: Map<EntityClass, org.greenrobot.greendao.generator.Entity>) {

    private val modelJsonAdapter: JsonAdapter<Model>

    private val modelRefId: ModelRefId

    private val entitiesReadByRefId = HashMap<Long, Entity>()
    private val entitiesReadByName = HashMap<String, Entity>()

    private var modelRead: Model? = null

    init {
        val moshi = Moshi.Builder().build()
        modelJsonAdapter = moshi.adapter<Model>(Model::class.java)
        modelRefId = ModelRefId()
        initModel()
    }

    fun sync() {
        var entityId = 1
        val entitiesModel = ArrayList<org.greenrobot.entitymodel.Entity>()

        for (entityParsed in entitiesParser) {
            val entityGenerator: org.greenrobot.greendao.generator.Entity = entitiesGenerator[entityParsed]!!
            var propertyId = 1
            val properties = ArrayList<Property>()
            for (field in entityGenerator.properties) {
                val name = field.dbName ?: field.propertyName
                val refId = modelRefId.create()
                val property = Property(name = name, id = propertyId,
                        refId = refId, targetEntityId = 0, indexId = 0, flags = 0, type = 0)
                propertyId++
                properties.add(property)
            }
            val name = entityParsed.name
            var refId = entityParsed.refId
            var existingEntity = findEntity(name, refId)
            if (refId == null) {
                refId = existingEntity?.refId ?: modelRefId.create()
            }

            val modelEntity = org.greenrobot.entitymodel.Entity(name = name, id = entityId, refId = refId,
                    properties = properties, lastPropertyId = propertyId - 1)
            entitiesModel.add(modelEntity)
            entityId++;
        }

        val model = Model(
                version = 1,
                metaVersion = 1,
                lastEntityId = entityId - 1,
                lastIndexId = 0,
                lastSequenceId = 0,
                entities = entitiesModel)

        writeModel(model)
    }

    private fun  findEntity(name: String, refId: Long?): Entity? {
        if(refId != null) {
            return entitiesReadByRefId[refId]
        } else {
            return entitiesReadByName[name]
        }
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
            if(entitiesReadByName.put(it.name, it) != null) {
                throw RuntimeException("Duplicate entity name ${it.name} in " + jsonFile.absolutePath)
            }
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