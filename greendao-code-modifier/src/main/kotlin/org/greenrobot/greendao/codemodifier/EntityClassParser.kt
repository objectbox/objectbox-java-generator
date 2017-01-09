package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser
import org.eclipse.jdt.core.dom.Comment
import org.eclipse.jdt.core.dom.CompilationUnit
import java.io.File

class EntityClassParser(val jdtOptions: MutableMap<Any, Any>, val encoding: String) {

    companion object {
        val AST_PARSER_LEVEL: Int = AST.JLS8

        // ignore errors about broken references to types/names defined outside of the entity class
        // the number is (problem id & IProblem.IgnoreCategoriesMask) as shown in log output
        val ignoredProblemIds: IntArray = intArrayOf(
                IProblem.UndefinedType, // 2
                IProblem.UndefinedName, // 50, external class refs, like TextUtils
                IProblem.UndefinedField, // 70
                IProblem.UnresolvedVariable, // 83
                IProblem.MissingTypeInMethod, // 120
                IProblem.MissingTypeInConstructor, // 129
                IProblem.MissingTypeInLambda, // 271
                IProblem.ImportNotFound, // 390
                IProblem.AbstractMethodMustBeImplemented, // 400 Comparable<T>.compareTo(T) can not be checked
                IProblem.PublicClassMustMatchFileName, // 325 our tests violate this
                IProblem.UnhandledWarningToken, // 631 SuppressWarnings tokens supported by IntelliJ, but not Eclipse
                IProblem.MethodMustOverrideOrImplement // 634 Inner defined PropertyConverter overrides
        )

        fun shouldReportProblem(problemId: Int): Boolean {
            return !ignoredProblemIds.contains(problemId)
        }
    }

    fun parse(javaFile: File, classesInPackage: List<String>): ParsedEntity? {
        val source = javaFile.readText(charset(encoding))

        val parser = ASTParser.newParser(AST_PARSER_LEVEL)
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
            val keep = shouldReportProblem(problemId)
            if (!keep) {
                System.out.println("[Verbose] Ignoring parser problem in ${javaFile}:${it.sourceLineNumber}: $it.")
            }
            keep
        }
        if (problems != null && problems.isNotEmpty()) {
            System.err.println("Found ${problems.size} problem(s) parsing \"${javaFile}\":")
            problems.forEachIndexed { i, problem ->
                System.err.println("#${i + 1} @${problem.sourceLineNumber}: $problem" +
                        " (ID: ${problem.id}; error: ${problem.isError})")
            }
            val first = problems[0]
            throw RuntimeException("Found ${problems.size} problem(s) parsing \"${javaFile}\". First problem:\n" +
                    first + " (${first.id} at line ${first.sourceLineNumber}).\n" +
                    "Run gradle with --info for more details.")
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

        return visitor.createParsedEntity(javaFile, source)
    }


}