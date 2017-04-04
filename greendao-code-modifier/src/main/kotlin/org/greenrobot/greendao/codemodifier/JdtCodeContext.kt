package org.greenrobot.greendao.codemodifier

import org.greenrobot.eclipse.jdt.core.JavaCore
import org.greenrobot.eclipse.jdt.internal.compiler.impl.CompilerOptions
import java.io.File
import java.util.Hashtable

/**
 * Context for parsing and transformation
 */
class JdtCodeContext(val formattingOptions: FormattingOptions? = null, encoding: String) {

    companion object {
        val JAVA_LEVEL: String = CompilerOptions.VERSION_1_7
    }

    private val jdtOptions: Hashtable<String, String>
    private val classParser: EntityClassParser

    init {
        jdtOptions = JavaCore.getOptions()
        jdtOptions.put(CompilerOptions.OPTION_Source, JAVA_LEVEL)
        jdtOptions.put(CompilerOptions.OPTION_Compliance, JAVA_LEVEL)
        // it could be the encoding is never used by JDT itself for our use case, but just to be sure (and for future)
        jdtOptions.put(CompilerOptions.OPTION_Encoding, encoding)

        classParser = EntityClassParser(jdtOptions, encoding)
    }

    fun parse(javaFile: File, classesInPackage: List<String>) = classParser.parse(javaFile, classesInPackage)

    fun transformer(parsedEntity: ParsedEntity) = EntityClassTransformer(parsedEntity, jdtOptions, formattingOptions)

}
