package org.greenrobot.entitymodel

import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import org.greenrobot.greendao.codemodifier.EntityClass
import org.greenrobot.greendao.generator.Entity
import org.greenrobot.greendao.generator.Schema
import java.util.*

class ModelSync(val entities: List<EntityClass>, val schema: Schema, val mapping: Map<EntityClass, Entity>) {
    fun sync() {
        var entityId = 1
        val modelEntities = ArrayList<org.greenrobot.entitymodel.Entity>()

        for (entityParsed in entities) {
            val entityGenerator: Entity = mapping[entityParsed]!!
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
            modelEntities.add(modelEntity)
            entityId++;
        }

        val model = Model(
                version = 1,
                metaVersion = 1,
                lastEntityId = entityId - 1,
                lastIndexId = 0,
                lastSequenceId = 0,
                entities = modelEntities)
        val moshi = Moshi.Builder().build()
        val modelJsonAdapter = moshi.adapter<Model>(Model::class.java)

        val buffer = Buffer()
        val jsonWriter = JsonWriter.of(buffer)
        jsonWriter.setIndent("    ")
        modelJsonAdapter.toJson(jsonWriter, model)
        val json = buffer.readUtf8();
        System.out.println(json)
    }

}