package org.greenrobot.idsync

data class IdSyncModel(
        // "Comments" in the JSON file
        val _note1: String = "KEEP THIS FILE! Check it into a version control system (VCS) like git.",
        val _note2: String = "ObjectBox manages crucial IDs for your object model. See docs for details.",
        val _note3: String = "If you have VCS merge conflicts, you must resolve them according to ObjectBox docs.",

        val version: Long,
        val metaVersion: Int,
        val lastEntityId: Int,
        val lastIndexId: Int,
        // TODO use this once we support sequences
        val lastSequenceId: Int,

        val entities: List<Entity>,

        val deletedEntities: List<Entity>,
        val deletedProperties: List<Property>
)

data class Entity(
        val name: String,
        val id: Int,
        val refId: Long,
        val lastPropertyId: Int,

        val properties: List<Property>
)

data class Property(
        val name: String,
        val id: Int,
        val refId: Long,
        val indexId: Int?
)

