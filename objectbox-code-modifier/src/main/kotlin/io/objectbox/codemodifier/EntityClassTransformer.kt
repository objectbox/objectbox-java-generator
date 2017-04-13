package io.objectbox.codemodifier

import io.objectbox.annotation.Uid
import org.greenrobot.eclipse.jdt.core.dom.*
import org.greenrobot.eclipse.jdt.core.dom.rewrite.ASTRewrite
import org.greenrobot.eclipse.jface.text.Document
import java.nio.charset.Charset
import java.util.Hashtable

/**
 * Helper for [ObjectBoxGenerator] to perform transformations on Entity class
 *
 * Features and responsibilities:
 * - manages imports
 * - detects formatting of the code and add new code according to that formatting
 * - does not replace existing code if it equals to generated without formatting
 * - keeps location of the replaced code (e.g. does not rearrange methods/properties after user)
 * - add new generated code
 * - keeps the code which is marked with to be kept
 * - replaces code which is marked as generated
 * - removes previously generated code which was not refreshed
 *
 * TODO make formatting detection lazy
 * TODO don't write AST to string if nothing is changed
 */
class EntityClassTransformer(val parsedEntity: ParsedEntity, val jdtOptions: Hashtable<String, String>,
                             formattingOptions: FormattingOptions?, val charset: Charset = Charsets.UTF_8) {
    private val cu = parsedEntity.node.root
    private val formatting = formattingOptions?.toFormatting()
            ?: Formatting.detect(parsedEntity.source, formattingOptions)
    private val formatter = Formatter(formatting)
    // Get a rewriter for this tree, that allows for transformation of the tree
    private val astRewrite = ASTRewrite.create(cu.ast)
    private val importsRewrite = astRewrite.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY)
    private val bodyRewrite = astRewrite.getListRewrite(parsedEntity.node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY)
    private val keepNodes = mutableSetOf<ASTNode>()
    private val addedImports = mutableSetOf<String>()

    init {
        val tabulation = formatting.tabulation
        jdtOptions.put("org.greenrobot.eclipse.jdt.core.formatter.tabulation.char", if (tabulation.tabChar == ' ') "space" else "tab")
        jdtOptions.put("org.greenrobot.eclipse.jdt.core.formatter.tabulation.size", tabulation.size.toString())
    }

    fun ensureImport(name: String) {
        val packageName = name.substringBeforeLast('.', "")
        // do not create import for inner classes
        val maybeInnerClassName = packageName.substringAfterLast(".", "")
        if (packageName != parsedEntity.packageName && maybeInnerClassName != parsedEntity.name
                && !parsedEntity.imports.has(name) && !addedImports.contains(name)) {
            val id = cu.ast.newImportDeclaration()
            id.name = cu.ast.newName(name.split('.').toTypedArray())
            importsRewrite.insertLast(id, null)
            addedImports += name
        }
    }

    fun remove(node: ASTNode) = bodyRewrite.remove(node, null)

    private fun insertMethod(code: String, replaceOld: ASTNode?, insertAfter: ASTNode?) {
        if (replaceOld != null && CodeCompare.isSameCode(replaceOld, code)) {
            keepNodes += replaceOld
        } else {
            val formatted = formatter.format(code)
            val newMethod = astRewrite.createStringPlaceholder(formatted, TypeDeclaration.METHOD_DECLARATION)
            replaceNode(newMethod, replaceOld, insertAfter)
        }
    }

    private fun insertField(code: String, replaceOld: ASTNode? = null) {
        if (replaceOld != null && CodeCompare.isSameCode(replaceOld, code)) {
            keepNodes += replaceOld
        } else {
            val formatted = formatter.format(code)
            val newField = astRewrite.createStringPlaceholder(formatted, TypeDeclaration.FIELD_DECLARATION)
            replaceNode(newField, replaceOld, parsedEntity.lastFieldDeclaration)
        }
    }

    /**
     * If it exists, replaces a Uid marker annotation node with a single member annotation that has the given UID as
     * value, like '@Uid' is replaced with '@Uid(42L)'.
     */
    fun insertUidAnnotationValue(node: FieldDeclaration, uid: Long) {
        // find the @Uid marker annotation node
        val uidAnnotation = node.modifiers().find { it is MarkerAnnotation
                && it.typeName.fullyQualifiedName == Uid::class.simpleName } as MarkerAnnotation?
        if (uidAnnotation == null) {
            return // field has no @Uid marker annotation
        }

        // create a new single member annotation such as '@Uid(42L)'
        val newUidAnnotation = cu.ast.newSingleMemberAnnotation()
        newUidAnnotation.typeName = cu.ast.newName(Uid::class.simpleName)
        newUidAnnotation.value = cu.ast.newNumberLiteral("${uid}L")

        // replace the annotation
        val listRewrite = astRewrite.getListRewrite(node, FieldDeclaration.MODIFIERS2_PROPERTY)
        listRewrite.replace(uidAnnotation, newUidAnnotation, null)
    }

    /**
     * if [oldNode] is not null, then replace it with [newNode]
     * otherwise if [orInsertAfter] is not null, then insert [newNode] after it
     * otherwise insert [newNode] after all available nodes
     * */
    private fun replaceNode(newNode: ASTNode, oldNode: ASTNode?, orInsertAfter: ASTNode?) {
        when {
            oldNode != null -> {
                bodyRewrite.insertAfter(newNode, oldNode, null)
                remove(oldNode)
            }
            orInsertAfter != null -> bodyRewrite.insertAfter(newNode, orInsertAfter, null)
            else -> bodyRewrite.insertLast(newNode, null)
        }
    }

    private val ASTNode.sourceLine: String
        get() = "${parsedEntity.sourceFile.path}:${lineNumber}"

    private fun Generatable<*>.checkKeepPresent() {
        val node = this.node
        val place = when (node) {
            is MethodDeclaration -> if (node.isConstructor) "constructor" else "method"
            is FieldDeclaration -> "field"
            else -> "declaration"
        }
        when (hint) {
            GeneratorHint.Keep -> println("Keep $place in ${node.sourceLine}")
            null -> throw RuntimeException(
                    """Can't replace $place in ${node.sourceLine} with generated version.
                    If you would like to keep it, it should be explicitly marked with @Keep annotation.
                    Otherwise please mark it with @Generated annotation""".trimIndent()
            )
            else -> Unit // ignore
        }
    }

    /**
     * Insert a new constructor with the given code block or replace the code block of an existing constructor.
     */
    fun defConstructor(paramTypes: List<String>, code: () -> String) {
        // constructor with the same signature already exists?
        val matchingConstructor = parsedEntity.constructors.find { it.hasSignature(parsedEntity.name, paramTypes) }
        if (matchingConstructor != null) {
            if (matchingConstructor.generated) {
                // annotated as generated, we can safely update it
                insertMethod(replaceHashStub(code()), matchingConstructor.node,
                        parsedEntity.lastConstructorDeclaration ?: parsedEntity.lastFieldDeclaration)
            } else {
                // no generated annotation, check if the user does not want us to replace it (has a keep annotation)
                matchingConstructor.checkKeepPresent()
            }
        } else {
            // any generated constructor we could replace exists?
            val generatedConstructor = parsedEntity.constructors.find { it.generated }
            if (generatedConstructor != null) {
                // can we replace it?
                val nodeToReplace: MethodDeclaration?
                if (generatedConstructor.parameters.isEmpty() || paramTypes.isEmpty()) {
                    // do not replace () constructor with (x,y,z) constructor, keep it
                    // do not replace (x,y,z) constructor with () constructor, keep it
                    keepNodes += generatedConstructor.node
                    nodeToReplace = null
                } else {
                    nodeToReplace = generatedConstructor.node
                }
                // insert or replace constructor
                insertMethod(replaceHashStub(code()), nodeToReplace,
                        parsedEntity.lastConstructorDeclaration ?: parsedEntity.lastFieldDeclaration)
            } else {
                // no generated constructor exists, just insert the desired one
                // any existing ones will be deleted once writing to string
                insertMethod(replaceHashStub(code()), null,
                        parsedEntity.lastConstructorDeclaration ?: parsedEntity.lastFieldDeclaration)
            }
        }
    }

    /**
     * Add or replace a default constructor. Ensures it is not annotated with @Generated.
     */
    fun ensureDefaultConstructor() {
        val defaultConstructor = parsedEntity.constructors.find { it.parameters.isEmpty() }
        if (defaultConstructor == null || defaultConstructor.generated) {
            // add a default constructor or replace the @Generated version
            val defaultConstructorCode =
                    """public ${parsedEntity.name}() {
                    }"""
            insertMethod(defaultConstructorCode, defaultConstructor?.node,
                    parsedEntity.lastConstructorDeclaration ?: parsedEntity.lastFieldDeclaration)
        } else {
            // there is one! just keep it
            keepNodes += defaultConstructor.node
        }
    }

    /**
     * Defines new method with the result of block function
     * In case method with such signature already exist:
     *  - if it has @Generated annotation, then replace it with the new one
     *  - otherwise keep it
     */
    fun defMethod(name: String, vararg paramTypes: String, code: () -> String) {
        val method = parsedEntity.methods.find { it.hasSignature(name, paramTypes.toList()) }
        // replace only generated code
        if (method == null || method.generated) {
            paramTypes.filter { it.contains('.') }.forEach { ensureImport(it) }
            insertMethod(replaceHashStub(code()), method?.node,
                    parsedEntity.lastMethodDeclaration
                            ?: parsedEntity.lastConstructorDeclaration
                            ?: parsedEntity.lastFieldDeclaration)
        } else {
            method.checkKeepPresent()
        }
    }

    /**
     * Defines new method, only if there is not other method with same signature
     * @see defMethod
     */
    fun defMethodIfMissing(name: String, vararg paramTypes: String, code: () -> String) {
        val method = parsedEntity.methods.find { it.hasSignature(name, paramTypes.toList()) }
        if (method == null) {
            paramTypes.filter { it.contains('.') }.forEach { ensureImport(it) }
            insertMethod(code(), null, parsedEntity.lastMethodDeclaration
                    ?: parsedEntity.lastConstructorDeclaration
                    ?: parsedEntity.lastFieldDeclaration)
        }
    }

    /**
     * In case field with provided name already exists:
     *  - if it has @Generated annotation, then replace it with the new one
     *  - otherwise keep it
     * */
    fun defineTransientGeneratedField(name: String, type: VariableType, comment: String? = null,
                                      qualifier: String? = null, assignment: String? = null) {
        val field = parsedEntity.transientFields.find { it.variable.name == name }
        // replace only generated code
        if (field == null || field.generated) {
            if (!type.isPrimitive && type.name != type.simpleName) {
                ensureImport(type.name)
            }
            var code = if (comment != null) "/** $comment */\n" else ""
            code += "@Internal\n"
            code += "@Generated(hash = $HASH_STUB)\n"
            var genericParams = type.typeArguments?.map { it.simpleName }?.joinToString() ?: ""
            if (genericParams.isNotBlank()) genericParams = "<$genericParams>"
            if (qualifier != null) {
                code += qualifier + " "
            }
            code += "transient ${type.simpleName}$genericParams $name"
            code += if (assignment != null) " = $assignment;" else ";"
            code = replaceHashStub(code)
            insertField(code, field?.node)
        } else {
            field.checkKeepPresent()
        }
    }

    fun defineProperty(name: String, type: VariableType, comment: String? = null) {
        if (!type.isPrimitive && type.name != type.simpleName) {
            ensureImport(type.name)
        }
        var code = if (comment != null) "/** $comment */\n" else ""
        var genericParams = type.typeArguments?.map { it.simpleName }?.joinToString() ?: ""
        if (genericParams.isNotBlank()) genericParams = "<$genericParams>"
        code += "${type.simpleName}$genericParams $name;"
        insertField(code)
    }

    private fun replaceHashStub(source: String): String {
        val hash = CodeCompare.codeHash(source)
        return source.replace(HASH_STUB, hash.toString())
    }

    fun writeToFile() {
        val newSource = writeToString()
        if (newSource != null) {
            println("Change " + parsedEntity.sourceFile.path)
            parsedEntity.sourceFile.writeText(newSource, charset)
        } else {
            println("Skip " + parsedEntity.sourceFile.path)
        }
    }

    /** @return null if nothing is changed */
    fun writeToString(): String? {
        fun Iterable<Generatable<*>>.removeUnneeded() {
            asSequence().filter { it.generated && it.node !in keepNodes }.forEach { remove(it.node) }
        }

        // remove all old generated methods/constructor/fields
        parsedEntity.apply {
            constructors.removeUnneeded()
            methods.removeUnneeded()
            transientFields.removeUnneeded()
        }

        val document = Document(parsedEntity.source)
        val edits = astRewrite.rewriteAST(document, jdtOptions)

        // computation of the new source code
        edits.apply(document)
        val newSource = document.get()
        return if (newSource != parsedEntity.source) {
            newSource
        } else {
            null
        }
    }
}

val HASH_STUB = "GENERATED_HASH_STUB"