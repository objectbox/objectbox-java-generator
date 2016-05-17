package org.greenrobot.greendao.gradle

import org.junit.Assert.*
import org.junit.Test

class UtilKtTest {
    val buffer = CharArray(DEFAULT_BUFFER_SIZE)
    val token = "org.greenrobot.greendao.annotation".toCharArray()

    @Test
    fun containsIgnoreSpaces() {
        val source = "import org.greenrobot.greendao.annotation.*;".toByteArray().inputStream()
        assertTrue(source.containsIgnoreSpaces(token, buffer, Charsets.UTF_8))
    }

    @Test
    fun containsIgnoreSpaces2() {
        val source = "@org\t.greenrobot .greendao\n\n\r.annotation.Entity".toByteArray().inputStream()
        assertTrue(source.containsIgnoreSpaces(token, buffer, Charsets.UTF_8))
    }

    @Test
    fun containsIgnoreSpacesFalse() {
        val source = "@org\t.greenrobot .greendao\n\n\r fail".toByteArray().inputStream()
        assertFalse(source.containsIgnoreSpaces(token, buffer, Charsets.UTF_8))
    }
}