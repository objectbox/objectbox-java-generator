package org.greenrobot.idsync

import org.greenrobot.essentials.hash.Murmur3F
import java.security.SecureRandom
import java.util.*

/**
 * A random ID to be assigned to model artifacts such as entities and properties.
 * This is preferred in referencing, because they are unique and thus detect illegal copy&paste.
 */
// Note: make it independent from the (real) ID, it will be necessary to change ID after git conflicts.
class ModelUid(
        val existingUids: MutableSet<Long> = HashSet()
) {
    // Use SecureRandom to better avoid conflicts when IDs are assigned in diverging git branches
    val random = SecureRandom();

    init {
        existingUids.forEach { verify(it) }
    }

    fun addExistingId(id: Long ) {
        verify(id)
        if(!existingUids.add(id)) {
            throw IdSyncException("Duplicate ref ID $id")
        }
    }

    fun addExistingIds(ids: Collection<Long> ) {
        ids.forEach { addExistingId(it) }
    }

    fun create(): Long {
        var newId:Long;
        do {
            var randomPart = (1 + random.nextLong()) and 0x7FFFFFFFFFFFFF00
            val murmur = Murmur3F()
            murmur.updateLongLE(randomPart)
            newId = randomPart or (murmur.value and 0xFF)
        } while (!existingUids.add(newId))
        return newId
    }

    fun verify(value: Long) {
        if (value < 0) throw IdSyncException("Illegal ref ID: " + value)
        val randomPart = value and 0x7FFFFFFFFFFFFF00
        if (randomPart == 0L) throw IdSyncException("Illegal ref ID: " + value)
        val murmur = Murmur3F()
        murmur.updateLongLE(randomPart)
        if (value < 0 || (value and 0xFF != murmur.value and 0xFF)) {
            throw IdSyncException("Illegal ref ID: " + value)
        }
    }

}

