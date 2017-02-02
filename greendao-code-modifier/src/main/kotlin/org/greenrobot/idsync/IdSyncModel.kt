package org.greenrobot.idsync

data class IdSyncModel(
        /** "Comments" in the JSON file */
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

        /** Previously used refIds, which are now deleted. Archived to ensure no collisions. */
        val retiredEntityUids: List<Long>?,

        /** Previously used refIds, which are now deleted. Archived to ensure no collisions. */
        val retiredPropertyUids: List<Long>?
)

/** This class also helps with Moshi: Moshi needs a field (not just a getter) and has custom serializers for types. */
class IdUid(var id: Int = 0, var uid: Long = 0) {

    constructor(value: String) : this() {
        fromString(value)
    }

    override fun toString() = "$id:$uid"

    fun fromString(value: String) {
        val splitted = value.split(':')
        if (splitted.size != 2) throw IllegalAccessException("Illegal ID: $value")
        id = splitted[0].toInt()
        uid = splitted[1].toLong()
    }

    override fun equals(other: Any?): Boolean {
        return other is IdUid && id == other.id && uid == other.uid
    }

    override fun hashCode(): Int {
        return id.xor(uid.hashCode())
    }
}

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
        @Deprecated("To read in old refIds from JSON", ReplaceWith("uid"))
        val refId: Long? = null,
        val lastPropertyId: Int,

        val properties: List<Property>
) : HasIdUid


data class Property(
        override val id: IdUid = IdUid(),
        val name: String,
        @Deprecated("To read in old refIds from JSON", ReplaceWith("uid"))
        val refId: Long? = null,
        val indexId: Int?
) : HasIdUid

