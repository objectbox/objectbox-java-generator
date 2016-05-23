package org.greenrobot.greendao.codemodifier

import de.greenrobot.common.hash.Murmur3A
import org.eclipse.jdt.core.dom.ASTNode

object CodeCompare {
    private val regexTooManySpaces = Regex("[ \\n\\t]+")
    private val regexUselessSpaces = Regex("\\s?(\\W)\\s?")
    private val regexJavaCommentMl = Regex("/\\*([^\\*]|\\*(?!/))*?\\*/")
    private val regexJavaCommentSl = Regex("//.*$")
    private val regexGeneratedAnnotation = Regex("@(org.greenrobot.greendao.annotation.)?Generated[(][^)]+[)]")
    private val murmur = Murmur3A()

    fun unformatCode(code: String) =
        code.replace(regexJavaCommentMl, "")
            .replace(regexJavaCommentSl, "")
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
}

/** checks code without formatting and comments are equal */
fun ASTNode.isSameCode(code: String) : Boolean {
    val nodeCode = CodeCompare.unformatCode(toString())
    val unformattedCode = CodeCompare.unformatCode(code)
    return nodeCode == unformattedCode
}