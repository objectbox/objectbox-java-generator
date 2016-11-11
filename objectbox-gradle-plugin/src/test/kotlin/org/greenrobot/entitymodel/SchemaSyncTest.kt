package org.greenrobot.entitymodel

import org.junit.Test

class SchemaSyncTest {
    @Test
    fun testModelRefId() {
        for (i in 0..100) {
            val x = ModelRefId.create()
            ModelRefId.verify(x)
            try {
                ModelRefId.verify(x xor 1)
            } catch (e: RuntimeException) {
                // Expected
            }
            try {
                ModelRefId.verify(x xor (1 shl 60))
            } catch (e: RuntimeException) {
                // Expected
            }
        }
    }
}