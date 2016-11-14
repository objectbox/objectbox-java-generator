package org.greenrobot.entitymodel

data class Model(
        val version: Long,
        val metaVersion: Int,
        val lastEntityId: Int,
        val lastIndexId: Int,
        val lastSequenceId: Int,

        val entities: List<Entity>
)

data class Entity(
        val name: String,
        val id: Int,
        val lastPropertyId: Int,

        val properties: List<Property>
)

data class Property(
        val name: String,
        val id: Int,
        val targetEntityId: Int,
        val indexId: Int,
        val flags: Int,
        val type: Int
)

