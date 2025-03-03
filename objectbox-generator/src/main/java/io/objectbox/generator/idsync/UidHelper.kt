/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.objectbox.generator.idsync

import org.greenrobot.essentials.hash.Murmur3F
import java.security.SecureRandom

/**
 * A random ID to be assigned to model artifacts such as entities and properties.
 * This is preferred in referencing, because they are unique and thus detect illegal copy&paste.
 */
// Note: make it independent from the (real) ID, it will be necessary to change ID after git conflicts.
class UidHelper(
    private val existingUids: MutableSet<Long> = HashSet()
) {
    // Use SecureRandom to better avoid conflicts when IDs are assigned in diverging git branches
    private val random = SecureRandom()

    init {
        existingUids.forEach { verify(it) }
    }

    fun addExistingId(id: Long) {
        verify(id)
        if (!existingUids.add(id)) {
            throw IdSyncException("Duplicate UID $id")
        }
    }

    fun addExistingIds(ids: Collection<Long>) {
        ids.forEach { addExistingId(it) }
    }

    /**
     * Creates random long where lowest byte is Murmur3F hash of upper bytes.
     */
    fun create(): Long {
        var newId: Long
        do {
            val randomPart = (1 + random.nextLong()) and 0x7FFFFFFFFFFFFF00
            val murmur = Murmur3F()
            murmur.updateLongLE(randomPart)
            newId = randomPart or (murmur.value and 0xFF)
        } while (!existingUids.add(newId))
        return newId
    }

    /**
     * This previously verified that the lowest byte is the Murmur3F hash of all upper bytes.
     *
     * As other language bindings never enforced this, currently only verifies that the value is positive.
     */
    fun verify(value: Long) {
        if (value <= 0) throw IdSyncException("Illegal UID: $value")
//        val randomPart = value and 0x7FFFFFFFFFFFFF00
//        if (randomPart == 0L) throw IdSyncException("Illegal UID: $value")
//        val murmur = Murmur3F()
//        murmur.updateLongLE(randomPart)
//        if (value < 0 || (value and 0xFF != murmur.value and 0xFF)) {
//            throw IdSyncException("Illegal UID: $value")
//        }
    }

}

