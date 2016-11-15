package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import java.io.File

/**
 * Context for parsing and transformation
 */
class JdtCodeContext(val formattingOptions: FormattingOptions? = null, encoding: String) {
    private val jdtOptions: MutableMap<Any, Any>
    private val classParser: EntityClassParser

    init {
        jdtOptions = JavaCore.getOptions()
        jdtOptions.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_7)
        jdtOptions.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_7)
        // it could be the encoding is never used by JDT itself for our use case, but just to be sure (and for future)
        jdtOptions.put(CompilerOptions.OPTION_Encoding, encoding)

        classParser = EntityClassParser(jdtOptions, encoding)
    }

    fun parse(javaFile: File, classesInPackage: List<String>) = classParser.parse(javaFile, classesInPackage)

    fun transformer(parsedEntity: ParsedEntity) = EntityClassTransformer(parsedEntity, jdtOptions, formattingOptions)

}
