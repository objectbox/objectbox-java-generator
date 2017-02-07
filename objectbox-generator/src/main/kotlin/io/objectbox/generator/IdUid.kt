package io.objectbox.generator

/** This class also helps with Moshi: Moshi needs a field (not just a getter) and has custom serializers for types. */
class IdUid(var id: Int = 0, var uid: Long = 0) : Cloneable {

    constructor(value: String) : this() {
        fromString(value)
    }

    override fun toString() = "$id:$uid"

    fun fromString(value: String) {
        val splitted = value.split(':')
        if (splitted.size != 2) throw IllegalArgumentException("Illegal ID: $value (expected format \"id:uid\")")
        id = splitted[0].toInt()
        uid = splitted[1].toLong()
    }

    override fun equals(other: Any?): Boolean {
        return other is IdUid && id == other.id && uid == other.uid
    }

    override fun hashCode(): Int {
        return id.xor(uid.hashCode())
    }

    fun set(id: Int, uid: Long): IdUid {
        this.id = id
        this.uid = uid
        return this
    }

    /** Convenience for last IDs, which get incremented */
    fun incId(uid: Long): IdUid {
        id++
        this.uid = uid
        return this
    }

    fun set(idUid: IdUid): IdUid {
        id = idUid.id
        uid = idUid.uid
        return this
    }

    public override fun clone(): IdUid {
        return IdUid(id, uid)
    }

    fun isEmpty() = id == 0 && uid == 0L

}