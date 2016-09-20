package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.CompilationUnit
import java.io.File
import java.util.Hashtable

class EntityClassParser(val jdtOptions: Hashtable<String, String>, val encoding: String) {
    fun parse(javaFile : File, classesInPackage: List<String>) : EntityClass? {
        val source = javaFile.readText(charset(encoding))

        val parser = ASTParser.newParser(AST.JLS8)
        parser.setCompilerOptions(jdtOptions)
        parser.setKind(ASTParser.K_COMPILATION_UNIT)

        // resolve bindings
        parser.setEnvironment(emptyArray(), emptyArray(), null, true)
        parser.setUnitName("/" + javaFile.path)
        parser.setBindingsRecovery(true)
        parser.setResolveBindings(true)

        parser.setSource(source.toCharArray())
        val astRoot = parser.createAST(null) as CompilationUnit

        // filtering type and import errors as bindings are only resolved inside of the entity class
        // in a future version we might include the whole classpath so all bindings can be resolved
        val problems = astRoot.problems?.filter {
            val problemId = it.id
            problemId != IProblem.PublicClassMustMatchFileName // our tests violate this
                    && problemId != IProblem.UndefinedField
                    && problemId != IProblem.UndefinedName // class refs, like TextUtils
                    && problemId != IProblem.UndefinedType
                    && problemId != IProblem.ImportNotFound
                    && problemId != IProblem.UnresolvedVariable
        }
        if (problems != null && problems.size > 0) {
            System.err.println("Found ${problems.size} problem(s) parsing \"${javaFile}\":")
            problems.forEach { System.err.println(it) }
            throw RuntimeException("Found problem(s) parsing \"${javaFile}\". See above")
        }

        val visitor = EntityClassASTVisitor(source, classesInPackage)
        astRoot.accept(visitor)

        return visitor.toEntityClass(javaFile, source)
    }
}