package org.greenrobot.greendao.codemodifier

import org.junit.Assert
import org.junit.Test

class FunsKtTest {
    @Test
    fun stringContainsIgnoreWhiteSpace() {
        val source = "import org.greenrobot.greendao.annotation.*;"
        val token = "org.greenrobot.greendao.annotations".toCharArray()
        Assert.assertTrue(source.containsIgnoreWhitespace(token))
    }

    @Test
    fun stringContainsIgnoreWhiteSpace2() {
        val source = "@org\t.greenrobot .greendao\n\n\r.annotation.Entity"
        val token = "org.greenrobot.greendao.annotation".toCharArray()
        Assert.assertTrue(source.containsIgnoreWhitespace(token))
    }
}