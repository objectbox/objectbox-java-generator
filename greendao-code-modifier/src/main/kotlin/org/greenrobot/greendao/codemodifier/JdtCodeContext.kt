package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import java.io.File

/**
 * Context for parsing and transformation
 */
class JdtCodeContext(val formattingOptions: FormattingOptions? = null) {
    private val jdtOptions: MutableMap<Any, Any>
    private val classParser: EntityClassParser

    init {
        jdtOptions = JavaCore.getOptions()
        jdtOptions.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_7)
        jdtOptions.put(CompilerOptions.OPTION_Encoding, "UTF-8")

        classParser = EntityClassParser(jdtOptions)
    }

    fun parse(javaFile: File, classesInPackage: List<String>) = classParser.parse(javaFile, classesInPackage)

    fun transformer(entityClass: EntityClass) = EntityClassTransformer(entityClass, jdtOptions, formattingOptions)

    fun transform(entityClass: EntityClass, block: EntityClassTransformer.() -> Unit) =
        transformer(entityClass).apply(block).writeToFile()
}
