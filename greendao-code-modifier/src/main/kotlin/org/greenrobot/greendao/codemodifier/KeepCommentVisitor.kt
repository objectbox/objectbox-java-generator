package org.greenrobot.greendao.codemodifier

import org.greenrobot.jdt.jdt.core.dom.CompilationUnit
import org.greenrobot.jdt.jdt.core.dom.LineComment

/**
 * Visitor to run for each comment in a compilation units comments list. It records the legacy keep fields positions.
 * http://stackoverflow.com/questions/3019729/how-to-access-comments-from-the-java-compiler-tree-api-generated-ast/9884987#9884987
 */
class KeepCommentVisitor(val entityUnit: CompilationUnit, val sourceSplit: List<String>) : LazyVisitor() {

    var keepFieldsStartLineNumber: Int = -1
    var keepFieldsEndLineNumber: Int = -1

    override fun visit(node: LineComment): Boolean {
        val lineNumber = entityUnit.getLineNumber(node.startPosition)
        val lineComment = sourceSplit[lineNumber - 1].trim()

        if (lineComment.startsWith("// KEEP FIELDS END")) {
            keepFieldsEndLineNumber = lineNumber
        } else if (lineComment.startsWith("// KEEP FIELDS")) {
            keepFieldsStartLineNumber = lineNumber
        }

        return false
    }

    /**
     * If start or end value is missing or end is before start, resets values.
     */
    fun validateLineNumbers() {
        if (keepFieldsStartLineNumber == -1 || keepFieldsEndLineNumber == -1
                || keepFieldsEndLineNumber < keepFieldsStartLineNumber) {
            keepFieldsStartLineNumber = -1
            keepFieldsEndLineNumber = -1
        }
    }

}
