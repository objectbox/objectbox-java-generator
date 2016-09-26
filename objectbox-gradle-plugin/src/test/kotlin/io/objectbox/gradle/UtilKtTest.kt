package io.objectbox.gradle

import org.junit.Assert.*
import org.junit.Test

class UtilKtTest {
    val buffer = CharArray(DEFAULT_BUFFER_SIZE)
    val token = "io.objectbox.annotation".toCharArray()

    @Test
    fun containsIgnoreSpaces() {
        val source = "import io.objectbox.annotation.*;".toByteArray().inputStream()
        assertTrue(Util.containsIgnoreSpaces(source, token, buffer, Charsets.UTF_8))
    }

    @Test
    fun containsIgnoreSpaces2() {
        val source = "@io\t.objectbox\n\n\r.annotation .Entity".toByteArray().inputStream()
        assertTrue(Util.containsIgnoreSpaces(source, token, buffer, Charsets.UTF_8))
    }

    @Test
    fun containsIgnoreSpacesFalse() {
        val source = "@io\t.objectbox .\n\n\r fail".toByteArray().inputStream()
        assertFalse(Util.containsIgnoreSpaces(source, token, buffer, Charsets.UTF_8))
    }
}