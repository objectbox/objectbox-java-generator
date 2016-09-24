package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.Comment
import org.eclipse.jdt.core.dom.CompilationUnit
import java.io.File

class EntityClassParser(val jdtOptions: MutableMap<Any, Any>, val encoding: String) {
    fun parse(javaFile: File, classesInPackage: List<String>): EntityClass? {
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
            val keep = problemId != IProblem.PublicClassMustMatchFileName // our tests violate this
                    && problemId != IProblem.UndefinedField
                    && problemId != IProblem.UndefinedName // class refs, like TextUtils
                    && problemId != IProblem.UndefinedType
                    && problemId != IProblem.ImportNotFound
                    && problemId != IProblem.UnresolvedVariable
            if (!keep) {
                System.out.println(
                        "[Verbose] Ignoring ID $problemId in ${javaFile}:${it.sourceLineNumber} (${it.message})")
            }
            keep
        }
        if (problems != null && problems.size > 0) {
            System.err.println("Found ${problems.size} problem(s) parsing \"${javaFile}\":")
            problems.forEachIndexed { i, problem ->
                System.err.println("#$i @${problem.sourceLineNumber}: ${problem.message}" +
                        " (ID: ${problem.id}; error: ${problem.isError})")
            }
            val first = problems[0]
            throw RuntimeException("Found ${problems.size} problem(s) parsing \"${javaFile}\". First problem:\n" +
                    first + " (${first.id} at line ${first.sourceLineNumber}\nRun gradle with --info for more details")
        }

        // try to find legacy KEEP FIELDS section
        val commentVisitor = KeepCommentVisitor(astRoot, source.split("\n"))
        val commentList = astRoot.commentList
        commentList.forEach {
            if (it is Comment) {
                it.accept(commentVisitor)
            }
        }
        commentVisitor.validateLineNumbers()

        val visitor = EntityClassASTVisitor(source, classesInPackage,
                commentVisitor.keepFieldsStartLineNumber, commentVisitor.keepFieldsEndLineNumber)
        astRoot.accept(visitor)

        return visitor.toEntityClass(javaFile, source)
    }
}