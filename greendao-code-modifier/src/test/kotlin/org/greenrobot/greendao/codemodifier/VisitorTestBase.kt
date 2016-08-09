package org.greenrobot.greendao.codemodifier

open class VisitorTestBase {

    val BarType = VariableType("com.example.Bar", false, "Bar")
    val BarItemType = VariableType("com.example.Bar.Item", false, "Bar.Item")
    val BarListType = VariableType("java.util.List", false, "List<Bar>", listOf(BarType))

    fun visit(code: String, classesInPackage: List<String> = emptyList()) =
            tryParseEntityClass(code, classesInPackage)

}
