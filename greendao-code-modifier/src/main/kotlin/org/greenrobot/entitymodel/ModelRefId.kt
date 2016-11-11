package org.greenrobot.entitymodel

import org.greenrobot.essentials.hash.Murmur3F
import java.util.*

/**
 * A random ID to be assigned to model artifacts such as entities and properties.
 * This is preferred in referencing, because they are unique and thus detect illegal copy&paste.
 */
// Note: make it independent from the (real) ID, it will be necessary to change ID after git conflicts.
object ModelRefId {
    val random = Random();

    fun create(): Long {
        var rid = random.nextLong() and 0x7FFFFFFFFFFFFF00
        val murmur = Murmur3F()
        murmur.updateLongLE(rid)
        return rid or (murmur.value and 0xFF)
    }

    fun verify(value: Long) {
        if (value < 0) throw RuntimeException("Illegal ref: " + value)
        val murmur = Murmur3F()
        murmur.updateLongLE(value and 0x7FFFFFFFFFFFFF00)
        if (value < 0 || (value and 0xFF != murmur.value and 0xFF)) {
            throw RuntimeException("Illegal ref: " + value)
        }
    }


}

