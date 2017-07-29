package io.objectbox.generator.idsync

import io.objectbox.generator.IdUid

data class IdSyncModel(
        /** "Comments" in the JSON file */
        val _note1: String = "KEEP THIS FILE! Check it into a version control system (VCS) like git.",
        val _note2: String = "ObjectBox manages crucial IDs for your object model. See docs for details.",
        val _note3: String = "If you have VCS merge conflicts, you must resolve them according to ObjectBox docs.",

        val version: Long,
        val modelVersion: Int = 2,
        val lastEntityId: IdUid,
        val lastIndexId: IdUid,
        val lastRelationId: IdUid,
        // TODO use this once we support sequences
        val lastSequenceId: IdUid,

        val entities: List<Entity>,

        /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
        val retiredEntityUids: List<Long>?,

        /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
        val retiredPropertyUids: List<Long>?,

        /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
        val retiredIndexUids: List<Long>?,

        /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
        val retiredRelationUids: List<Long>?
)

interface HasIdUid {
    val id: IdUid

    var uid: Long
        get() = id.uid
        set(value) {
            id.uid = value
        }

    var modelId: Int
        get() = id.id
        set(value) {
            id.id = value
        }
}

data class Entity(
        val name: String,
        override val id: IdUid = IdUid(),
        val lastPropertyId: IdUid,
        val properties: List<Property>
) : HasIdUid


data class Property(
        override val id: IdUid = IdUid(),
        val name: String,
        val indexId: IdUid?
) : HasIdUid

