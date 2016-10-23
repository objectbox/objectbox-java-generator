package io.objectbox.gradle

import org.junit.Test

class SchemaSyncTest {
    @Test
    fun testSchemaIdRef() {
        for (i in 0..100) {
            val x = SchemaIdRef.create()
            SchemaIdRef.verify(x)
            try {
                SchemaIdRef.verify(x xor 1)
            } catch (e: RuntimeException) {
                // Expected
            }
            try {
                SchemaIdRef.verify(x xor (1 shl 60))
            } catch (e: RuntimeException) {
                // Expected
            }
        }
    }
}