package io.objectbox.gradle

import org.greenrobot.essentials.hash.Murmur3F
import java.util.*

object SchemaIdRef {
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

