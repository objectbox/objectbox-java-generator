package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.CompilationUnit
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import org.junit.Assert
import org.mockito.Mockito
import java.io.File

fun parseCompilationUnit(javaCode: String): CompilationUnit {
    val parser = ASTParser.newParser(AST.JLS8)
    val jdtOptions = JavaCore.getOptions()
    jdtOptions.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_7)
    jdtOptions.put(CompilerOptions.OPTION_Encoding, "UTF-8")
    parser.setCompilerOptions(jdtOptions)
    parser.setKind(ASTParser.K_COMPILATION_UNIT)
    parser.setSource(javaCode.toCharArray())
    val astRoot = parser.createAST(null) as CompilationUnit

    val problems = astRoot.problems;
    if (problems != null && problems.size > 0) {
        println("Got {} problems compiling the source file: ${problems.size}");
        problems.forEach { println(it) }
        Assert.fail("There was problems during source compilation")
    }

    return astRoot
}

fun tryParseEntityClass(javaCode: String, classesInPackage: List<String> = emptyList()): EntityClass? {
    val visitor = EntityClassASTVisitor(classesInPackage)
    val unit = parseCompilationUnit(javaCode)
    unit.accept(visitor)
    return visitor.toEntityClass(Mockito.mock(File::class.java), javaCode)
}

fun parseEntityClass(javaCode: String): EntityClass = tryParseEntityClass(javaCode)!!

val StringType = VariableType("String", isPrimitive = false, originalName = "String")
val IntType = VariableType("int", isPrimitive = true, originalName = "int")