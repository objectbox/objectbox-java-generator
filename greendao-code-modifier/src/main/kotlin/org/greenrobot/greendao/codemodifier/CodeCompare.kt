package org.greenrobot.greendao.codemodifier

import org.greenrobot.essentials.hash.Murmur3F
import org.greenrobot.jdt.jdt.core.dom.ASTNode

object CodeCompare {
    private val regexTooManySpaces = Regex("[ \\n\\t\\r]+")
    private val regexUselessSpaces = Regex("\\s?(\\W)\\s?")
    private val regexJavaCommentMl = Regex("/\\*([^\\*]|\\*(?!/))*?\\*/")
    private val regexJavaCommentSl = Regex("//(.*)$")
    private val regexGeneratedAnnotation = Regex("@(org.greenrobot.greendao.annotation.)?Generated[(][^)]+[)]")
    private val murmur = Murmur3F()

    /** IMPORTANT: do not change this method without real need, because all the hashes can be invalidated */
    fun unformatCode(code: String) =
        // replace single-line with multi-line to do no mess up the code after \n is replaced
        code.replace(regexJavaCommentSl, "/*$1*/")
            .replace(regexJavaCommentMl, { "/*" + it.value.replace("*", "").trim() + "*/" })
            .replace(regexTooManySpaces, " ")
            .replace(regexUselessSpaces, "$1")
            .trim()

    fun codeHash(code: String): Int {
        murmur.reset()
        val unformattedCode = unformatCode(code.replace(regexGeneratedAnnotation, ""))
        murmur.update(unformattedCode.toByteArray())
        val intHash = murmur.value.toInt()
        // get rid of minus
        return intHash shl 1 ushr 1
    }

    /** checks code without formatting and comments are equal */
    fun isSameCode(node: ASTNode, code: String) : Boolean {
        val nodeCode = CodeCompare.unformatCode(node.toString())
        val unformattedCode = CodeCompare.unformatCode(code)
        return nodeCode == unformattedCode
    }
}

