package io.objecbox.generator.idsync

import io.objectbox.generator.IdUid

data class IdSyncModel(
        /** "Comments" in the JSON file */
        val _note1: String = "KEEP THIS FILE! Check it into a version control system (VCS) like git.",
        val _note2: String = "ObjectBox manages crucial IDs for your object model. See docs for details.",
        val _note3: String = "If you have VCS merge conflicts, you must resolve them according to ObjectBox docs.",

        val version: Long,
        val metaVersion: Int,
        val lastEntityId: IdUid,
        val lastIndexId: IdUid,
        // TODO use this once we support sequences
        val lastSequenceId: IdUid,

        val entities: List<Entity>,

        /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
        val retiredEntityUids: List<Long>?,

        /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
        val retiredPropertyUids: List<Long>?,

        /** Previously used UIDs, which are now deleted. Archived to ensure no collisions. */
        val retiredIndexUids: List<Long>?
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
        val properties: List<Property>,

        @Deprecated("To read in old refIds from JSON", ReplaceWith("uid"))
        val refId: Long? = null
) : HasIdUid


data class Property(
        override val id: IdUid = IdUid(),
        val name: String,
        val indexId: IdUid?,

        @Deprecated("To read in old refIds from JSON", ReplaceWith("uid"))
        val refId: Long? = null
) : HasIdUid

