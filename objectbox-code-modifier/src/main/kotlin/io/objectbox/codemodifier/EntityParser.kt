package io.objectbox.codemodifier

import org.greenrobot.eclipse.jdt.core.compiler.IProblem
import org.greenrobot.eclipse.jdt.core.dom.AST
import org.greenrobot.eclipse.jdt.core.dom.ASTParser
import org.greenrobot.eclipse.jdt.core.dom.CompilationUnit
import java.io.File
import java.util.Hashtable

class EntityParser(val jdtOptions: Hashtable<String, String>, val encoding: String) {

    companion object {
        val AST_PARSER_LEVEL: Int = AST.JLS8

        // ignore errors about broken references to types/names defined outside of the entity class
        // the number is (problem id & IProblem.IgnoreCategoriesMask) as shown in log output
        val ignoredProblemIds: IntArray = intArrayOf(
                IProblem.UndefinedType, // 2
                IProblem.UndefinedName, // 50, external class refs, like TextUtils
                IProblem.UndefinedField, // 70
                IProblem.UnresolvedVariable, // 83
                IProblem.UndefinedMethod, // 100 entities with super class
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

    fun parseEntityFiles(sourceFiles: Iterable<File>): Map<String, List<ParsedEntity>> {
        val start = System.currentTimeMillis()
        val classesByDir = sourceFiles.map { it.parentFile }.distinct().map {
            it to it.getJavaClassNames()
        }.toMap()

        val parsedEntities = sourceFiles.asSequence()
                .map {
                    val entity = parse(it, classesByDir[it.parentFile]!!)
                    if (entity != null && entity.properties.size == 0) {
                        System.err.println("Skipping entity ${entity.name} as it has no properties.")
                        null
                    } else {
                        entity
                    }
                }
                .filterNotNull()
                .toList()

        val time = System.currentTimeMillis() - start
        println("Parsed ${parsedEntities.size} entities in $time ms among ${sourceFiles.count()} source files: " +
                "${parsedEntities.asSequence().map { it.name }.joinToString()}")
        val entitiesBySchema = parsedEntities.groupBy { it.schema }
        entitiesBySchema.values.forEach { parse2ndPass(it) }
        return entitiesBySchema
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
            throw ParseException("Found ${problems.size} problem(s) parsing \"${javaFile}\". First problem: " +
                    first + " (${first.id} at line ${first.sourceLineNumber}).\n" +
                    "Run gradle with --info for more details.")
        }

        val visitor = EntityClassASTVisitor(source, classesInPackage)
        astRoot.accept(visitor)

        return visitor.createParsedEntity(javaFile, source)
    }

    private fun parse2ndPass(parsedEntities: List<ParsedEntity>) {
        parsedEntities.forEach { parsedEntity ->
            parsedEntity.toOneRelations.forEach { toOne ->
                parse2ndPassToOne(parsedEntity, toOne)
            }
        }
    }

    /**
     * Look at to-one relation ID properties to:
     * 1) generate virtual properties
     * 2) add the index so IdSync can assign a index ID
     */
    private fun parse2ndPassToOne(parsedEntity: ParsedEntity, toOne: ToOneRelation) {
        val defaultName = toOne.variable.name + "Id"
        if (toOne.targetIdName == null) {
            toOne.targetIdName = defaultName
        }

        var parsedProperty: ParsedProperty? = parsedEntity.properties.find { it.variable.name == toOne.targetIdName }
        if (parsedProperty == null) {
            // Property does not exist, adding a virtual property

            // TODO ensure generator's ToOne uses the same targetName (ToOne.nameToOne)
            val targetName = if (toOne.variableIsToOne) toOne.variable.name else toOne.variable.name + "ToOne"
            parsedProperty = ParsedProperty(
                    variable = Variable(VariableType("long", true, "long"), toOne.targetIdName!!),
                    fieldAccessible = true,
                    uid = toOne.uid,
                    dbName = toOne.targetIdDbName,
                    virtualTargetName = targetName
            )
            parsedEntity.properties.add(parsedProperty)
            // parsedEntity.propertiesToGenerate.add(parsedProperty)
        }
        if (parsedProperty.index == null) {
            parsedProperty.index = PropertyIndex(null, false)
        }
    }

}