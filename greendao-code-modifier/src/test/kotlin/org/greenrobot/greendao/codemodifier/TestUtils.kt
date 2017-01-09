package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.CompilationUnit
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import org.junit.Assert
import org.mockito.Mockito
import java.io.File

/**
 * WARNING: this should behave similar to code in JdtCodeContext and EntityClassParser.
 */
fun parseCompilationUnit(javaCode: String): CompilationUnit {
    val jdtOptions = JavaCore.getOptions()
    jdtOptions.put(CompilerOptions.OPTION_Source, JdtCodeContext.JAVA_LEVEL)
    jdtOptions.put(CompilerOptions.OPTION_Compliance, JdtCodeContext.JAVA_LEVEL)
    jdtOptions.put(CompilerOptions.OPTION_Encoding, "UTF-8")

    val parser = ASTParser.newParser(EntityClassParser.AST_PARSER_LEVEL)
    parser.setCompilerOptions(jdtOptions)
    parser.setKind(ASTParser.K_COMPILATION_UNIT)

    // resolve bindings
    parser.setEnvironment(emptyArray(), emptyArray(), null, true)
    parser.setUnitName("/")
    parser.setBindingsRecovery(true)
    parser.setResolveBindings(true)

    parser.setSource(javaCode.toCharArray())
    val astRoot = parser.createAST(null) as CompilationUnit

    val problems = astRoot.problems
    if (problems != null && problems.isNotEmpty()) {
        System.err.println("Found ${problems.size} problem(s) parsing:")
        var fail = false
        problems.forEachIndexed { i, problem ->
            val message = "#${i + 1} @${problem.sourceLineNumber}: $problem (ID: ${problem.id}; error: ${problem.isError})"
            if (EntityClassParser.shouldReportProblem(problem.id)) {
                System.err.println(message)
                fail = true
            } else {
                System.out.println("[IGNORED] $message")
            }
        }
        if (fail) {
            Assert.fail("There were parsing problems, see log messages.")
        }
    }

    return astRoot
}

fun tryParseEntity(javaCode: String, classesInPackage: List<String> = emptyList()): ParsedEntity? {
    val visitor = EntityClassASTVisitor(javaCode, classesInPackage, -1, -1)
    val unit = parseCompilationUnit(javaCode)
    unit.accept(visitor)
    return visitor.createParsedEntity(Mockito.mock(File::class.java), javaCode)
}

fun parseEntity(javaCode: String): ParsedEntity = tryParseEntity(javaCode)!!

val StringType = VariableType("String", isPrimitive = false, originalName = "String")
val IntType = VariableType("int", isPrimitive = true, originalName = "int")