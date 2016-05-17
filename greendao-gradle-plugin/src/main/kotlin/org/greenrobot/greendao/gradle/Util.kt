package org.greenrobot.greendao.gradle

import org.codehaus.groovy.runtime.MethodClosure
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

/** helper to create Groovy's Closure from Kotlin's block */
fun <P, R> Closure(block: (P) -> R) = MethodClosure(block, "invoke")

/** Search for token ignoring whitespaces */
fun File.containsIgnoreSpaces(token: CharArray, buffer: CharArray, charset: Charset): Boolean =
    inputStream().containsIgnoreSpaces(token, buffer, charset)

fun InputStream.containsIgnoreSpaces(token: CharArray, buffer: CharArray, charset: Charset): Boolean {
    bufferedReader(charset).use { reader ->
        val tokenSize = token.size
        var index = 0
        do {
            val read = reader.read(buffer)
            for (i in 0..read - 1) {
                val ch = buffer[i]
                if (index == 0) {
                    if (ch == token[0]) {
                        index = 1
                    }
                } else if (ch == token[index]) {
                    index++
                    if (index == tokenSize) {
                        return true
                    }
                } else if (!(ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')) {
                    index = 0
                }
            }
        } while (read >= 0)
    }
    return false
}

