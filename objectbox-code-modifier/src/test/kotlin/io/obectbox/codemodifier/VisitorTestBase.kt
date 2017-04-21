package io.obectbox.codemodifier

import io.objectbox.codemodifier.VariableType

open class VisitorTestBase {

    val BarType = VariableType("com.example.Bar", false, "Bar")
    val BarItemType = VariableType("com.example.Bar.Item", false, "Bar.Item")
    val BarListType = VariableType("java.util.List", false, "List<Bar>", listOf(BarType))

    fun visit(code: String, classesInPackage: List<String> = emptyList()) =
            tryParseEntity(code, classesInPackage)

}
