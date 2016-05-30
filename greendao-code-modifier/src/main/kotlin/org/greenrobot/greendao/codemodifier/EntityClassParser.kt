package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.CompilationUnit
import java.io.File

class EntityClassParser(val jdtOptions: MutableMap<Any, Any>, val encoding: String) {
    fun parse(javaFile : File, classesInPackage: List<String>) : EntityClass? {
        val source = javaFile.readText(charset(encoding))

        val parser = ASTParser.newParser(AST.JLS8)
        parser.setCompilerOptions(jdtOptions)
        parser.setKind(ASTParser.K_COMPILATION_UNIT)

        // uncomment to have bindings resolved
        //        parser.setEnvironment(emptyArray(), sourceRoots.toTypedArray(), null, true)
        //        parser.setUnitName("/" + javaFile.path)
        //        parser.setBindingsRecovery(true)
        //        parser.setResolveBindings(true)

        parser.setSource(source.toCharArray())
        val astRoot = parser.createAST(null) as CompilationUnit

        val problems = astRoot.problems;
        if (problems != null && problems.size > 0) {
            System.err.println("Found ${problems.size} problem(s) parsing \"${javaFile}\":");
            problems.forEach { System.err.println(it) }
            throw RuntimeException("Found problem(s) parsing \"${javaFile}\". See above")
        }

        val visitor = EntityClassASTVisitor(source, classesInPackage)
        astRoot.accept(visitor)

        return visitor.toEntityClass(javaFile, source)
    }
}