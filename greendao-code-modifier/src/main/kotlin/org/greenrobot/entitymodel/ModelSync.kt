package org.greenrobot.entitymodel

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import okio.Okio
import org.greenrobot.greendao.codemodifier.EntityClass
import org.greenrobot.greendao.generator.Entity
import org.greenrobot.greendao.generator.Schema
import java.io.File
import java.util.*

class ModelSync(
        val jsonFile: File,
        val entitiesParser: List<EntityClass>,
        val schemaGenerator: Schema,
        val entitiesGenerator: Map<EntityClass, Entity>) {

    private var modelJsonAdapter: JsonAdapter<Model>

    init {
        val moshi = Moshi.Builder().build()
        modelJsonAdapter = moshi.adapter<Model>(Model::class.java)
    }

    fun sync() {
        val source = Okio.source(jsonFile)
        val modelRead: Model;
        try {
            modelRead = modelJsonAdapter.fromJson(Okio.buffer(source))
        } finally {
            source.close()
        }

        var entityId = 1
        val entitiesModel = ArrayList<org.greenrobot.entitymodel.Entity>()

        for (entityParsed in entitiesParser) {
            val entityGenerator: Entity = entitiesGenerator[entityParsed]!!
            var propertyId = 1
            val properties = ArrayList<Property>()
            for (field in entityGenerator.properties) {
                val name = field.dbName ?: field.propertyName
                val property = Property(name = name, id = propertyId, targetEntityId = 0, indexId = 0, flags = 0, type = 0)
                propertyId++
                properties.add(property)
            }
            val modelEntity = org.greenrobot.entitymodel.Entity(name = entityParsed.name, id = entityId,
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