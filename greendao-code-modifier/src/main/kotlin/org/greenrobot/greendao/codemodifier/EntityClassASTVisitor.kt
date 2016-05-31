package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.core.dom.Annotation
import org.greenrobot.greendao.annotation.*
import java.io.File
import kotlin.reflect.KClass

/**
 * Visits compilation unit, find if it is an Entity and reads all the required information about it
 */
class EntityClassASTVisitor(val source: String, val classesInPackage: List<String> = emptyList()) : LazyVisitor() {
    var isEntity = false
    var schemaName: String = "default"
    val fields = mutableListOf<EntityField>()
    val transientFields = mutableListOf<TransientField>()
    val constructors = mutableListOf<Method>()
    val methods = mutableListOf<Method>()
    val imports = mutableListOf<ImportDeclaration>()
    var packageName : String? = null
    var entityTableName : String? = null
    var typeDeclaration : TypeDeclaration? = null
    val oneRelations = mutableListOf<OneRelation>()
    val manyRelations = mutableListOf<ManyRelation>()
    var tableIndexes = emptyList<TableIndex>()
    var active = false
    var keepSource = false
    var createTable = true
    var usedNotNullAnnotation: String? = null;
    var lastField: FieldDeclaration? = null

    private val methodAnnotations = mutableListOf<Annotation>()
    private val fieldAnnotations = mutableListOf<Annotation>()

    override fun visit(node: CompilationUnit): Boolean  = true

    override fun visit(node: ImportDeclaration): Boolean {
        imports += node
        return true
    }

    override fun visit(node: PackageDeclaration): Boolean {
        packageName = node.name.fullyQualifiedName
        return true
    }

    private fun Annotation.hasType(klass : KClass<*>) : Boolean {
        return if (typeName.isSimpleName) {
            typeName.fullyQualifiedName == klass.simpleName && imports.has(klass)
        } else {
            typeName.fullyQualifiedName == klass.qualifiedName
        }
    }

    private fun Type.toVariableType() : VariableType {
        val arguments = if (this is ParameterizedType) {
            this.typeArguments().asSequence().map {
                if (it is Type) it.toVariableType() else null
            }.filterNotNull().toList()
        } else {
            null
        }
        return VariableType(typeName, isPrimitiveType, toString(), arguments)
    }

    private val Type.typeName: String
        get() = try {
            typeName(imports, packageName, classesInPackage)
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("Error processing \"${typeDeclaration?.name?.identifier}\": ${e.message}", e)
        }

    fun visitAnnotation(node : Annotation) : Boolean {
        val parent = node.parent
        when (parent) {
            is TypeDeclaration -> {
                when {
                    node.hasType(Entity::class) -> {
                        isEntity = true
                        val entityAnnotation = AnnotationProxy<Entity>(node)
                        schemaName = entityAnnotation.schema
                        active = entityAnnotation.active
                    }
                    node.hasType(Table::class) -> {
                        val tableAnnotation = AnnotationProxy<Table>(node)
                        entityTableName = tableAnnotation.name.nullIfBlank()
                        createTable = tableAnnotation.create
                        try {
                            tableIndexes = tableAnnotation.indexes.map {
                                TableIndex(it.name.nullIfBlank(), parseIndexSpec(it.value), it.unique)
                            }
                        } catch (e: IllegalArgumentException) {
                            throw RuntimeException("Can't parse @Index.value for ${parent.name} " +
                                "because of: ${e.message}", e)
                        }
                    }
                    node.hasType(Keep::class) -> {
                        keepSource = true
                    }
                }
            }
            is MethodDeclaration -> methodAnnotations += node
            is FieldDeclaration -> fieldAnnotations += node
        }
        return true
    }

    override fun visit(node : MarkerAnnotation) : Boolean = visitAnnotation(node)

    override fun visit(node: SingleMemberAnnotation): Boolean = visitAnnotation(node)

    override fun visit(node: NormalAnnotation): Boolean = visitAnnotation(node)

    override fun visit(node : FieldDeclaration) : Boolean = isEntity

    override fun endVisit(node: FieldDeclaration) {
        val fa = fieldAnnotations
        val varNames = node.fragments()
            .filter { it is VariableDeclarationFragment }
            .map { it as VariableDeclarationFragment }.map { it.name }
        val variableType = node.type.toVariableType()
        if (fa.none { it.typeName.fullyQualifiedName == "Transient" }
            && !Modifier.isTransient(node.modifiers)) {
            when {
                fa.has<ToOne>() -> {
                    oneRelations += varNames.map { oneRelation(fa, it, variableType) }
                }
                fa.has<ToMany>() -> {
                    manyRelations += varNames.map { manyRelation(fa, it, variableType) }
                }
                else -> fields += varNames.map { entityField(fa, it, node, variableType) }
            }
        } else {
            val generatorHint = fa.generatorHint
            if (generatorHint != null) {
                // check code is not changed
                if (generatorHint is GeneratorHint.Generated) {
                    node.checkUntouched(generatorHint)
                }
            }
            transientFields += varNames.map {
                TransientField(Variable(variableType, it.toString()), node, generatorHint)
            }
        }
        if (usedNotNullAnnotation == null) {
            usedNotNullAnnotation = fa.find {
                it.typeName.fullyQualifiedName == "NotNull" || it.typeName.fullyQualifiedName == "NonNull"
            }?.typeName?.fullyQualifiedName?.let { "@" + it }
        }
        fa.clear()

        lastField = node
    }

    private val ASTNode.codePlace : String?
        get() = "${typeDeclaration?.name?.identifier}:$lineNumber"

    private val ASTNode.originalCode: String
        get() = source.substring(startPosition..(startPosition + length - 1))

    private fun ASTNode.checkUntouched(hint: GeneratorHint.Generated) {
        if (hint.hash != -1 && hint.hash != CodeCompare.codeHash(this.originalCode)) {
            val place = when(this) {
                is MethodDeclaration -> if (this.isConstructor) "Constructor" else "Method '$name'"
                is FieldDeclaration -> "Field '${this.originalCode.trim()}'"
                else -> "Node"
            }
            throw RuntimeException("""
                        $place (see ${codePlace}) has been changed after generation.
                        Please either mark it with @Keep annotation instead of @Generated to keep it untouched,
                        or use @Generated (without hash) to allow to replace it.
                        """.trimIndent())

        }
    }

    private val List<Annotation>.generatorHint: GeneratorHint?
        get() = if (has<Keep>()) {
            GeneratorHint.Keep
        } else {
            proxy<Generated>()?.let { GeneratorHint.Generated(it.hash) }
        }

    private val List<Annotation>.hasNotNull: Boolean
        get() = any {
            it.typeName.fullyQualifiedName == "NotNull" || it.typeName.fullyQualifiedName == "NonNull"
        }

    private inline fun <reified A> List<Annotation>.has(): Boolean {
        return any { it.hasType(A::class) }
    }

    /** Tries to find annotation of specified list, and if any, then create proxy for it */
    private inline fun <reified T : kotlin.Annotation> List<Annotation>.proxy(): T? {
        return find { it.hasType(T::class) }?.let { AnnotationProxy<T>(it) }
    }

    private fun oneRelation(fa: MutableList<Annotation>, fieldName: SimpleName,
                            variableType: VariableType) : OneRelation {
        val proxy = fa.proxy<ToOne>()!!
        return OneRelation(
            Variable(variableType, fieldName.toString()),
            foreignKeyField = proxy.foreignKey.nullIfBlank(),
            columnName = fa.proxy<Column>()?.name?.nullIfBlank(),
            isNotNull = fa.hasNotNull,
            unique = fa.has<Unique>()
        )
    }

    private fun manyRelation(fa: MutableList<Annotation>, fieldName: SimpleName,
                             variableType: VariableType) : ManyRelation {
        val proxy = fa.proxy<ToMany>()!!
        val joinEntityAnnotation = fa.find {
            it.hasType(JoinEntity::class)
        } as? NormalAnnotation
        val orderByAnnotation = fa.proxy<OrderBy>()
        return ManyRelation(
            Variable(variableType, fieldName.toString()),
            mappedBy = proxy.mappedBy.nullIfBlank(),
            joinOnProperties = proxy.joinOn.map { JoinOnProperty(it.source, it.target) },
            joinEntitySpec = joinEntityAnnotation?.let {
                val joinProxy = AnnotationProxy<JoinEntity>(it)
                JoinEntitySpec(
                    entityName = (it["entity"] as TypeLiteral).type.typeName,
                    sourceIdProperty = joinProxy.sourceProperty,
                    targetIdProperty = joinProxy.targetProperty
                )
            },
            order = orderByAnnotation?.let {
                val spec = it.value
                if (spec.isBlank()) {
                    emptyList()
                } else {
                    try {
                        parseIndexSpec(spec)
                    } catch (e: IllegalArgumentException) {
                        throw RuntimeException("Can't parse @OrderBy.value for " +
                            "${typeDeclaration?.name}.${fieldName} because of: ${e.message}.", e)
                    }
                }
            }
        )
    }

    private fun entityField(fa: MutableList<Annotation>, fieldName: SimpleName,
                            node: FieldDeclaration, variableType: VariableType): EntityField {
        val columnAnnotation = fa.proxy<Column>()
        val indexAnnotation = fa.proxy<Index>()
        val idAnnotation = fa.proxy<Id>()

        if (indexAnnotation?.value?.isNotBlank() ?: false) {
            throw RuntimeException(
                """greenDAO: setting value on @Index is not supported if @Index is used on the properties
                See '$fieldName' in ${typeDeclaration?.name?.identifier}"""
            )
        }

        return EntityField(
            variable = Variable(variableType, fieldName.toString()),
            id = idAnnotation?.let { TableId(it.autoincrement) },
            index = indexAnnotation?.let { PropertyIndex(indexAnnotation.name.nullIfBlank(), indexAnnotation.unique) },
            isNotNull = node.type.isPrimitiveType || fa.hasNotNull,
            columnName = columnAnnotation?.name?.let { it.nullIfBlank() },
            customType = fa.find { it.hasType(Convert::class) }
                ?.let { it as? NormalAnnotation }
                ?.let { readConverterAnnotation(it) },
            unique = fa.has<Unique>()
        )
    }

    private fun readConverterAnnotation(it: NormalAnnotation): CustomType? {
        val converterClassName = (it["converter"] as? TypeLiteral)?.type?.typeName
        val columnType = (it["columnType"] as? TypeLiteral)?.type
        return if (converterClassName != null && columnType != null) {
            CustomType(converterClassName, columnType.toVariableType())
        } else {
            null
        }
    }

    override fun visit(node: MethodDeclaration): Boolean = isEntity

    override fun endVisit(node : MethodDeclaration) {
        val generatorHint = methodAnnotations.generatorHint
        if (generatorHint is GeneratorHint.Generated) {
            node.checkUntouched(generatorHint)
        }
        val method = Method(
            node.name.fullyQualifiedName,
            node.parameters()
                .filter { it is SingleVariableDeclaration }
                .map { it as SingleVariableDeclaration }
                .map { it -> Variable(it.type.toVariableType(), it.name.identifier) },
            node,
            generatorHint
        )
        if (node.isConstructor) {
            constructors += method
        } else {
            methods += method
        }
        methodAnnotations.clear()
    }

    override fun visit(node: TypeDeclaration) : Boolean {
        if (node.parent is TypeDeclaration) {
            // skip inner classes
            return false
        } else {
            this.typeDeclaration = node
            return true
        }
    }

    /**
     * Collects parsed information into EntityClass, sets javaFile and source
     * @return null if parsed class it is not an entity
     * */
    fun toEntityClass(javaFile: File, source: String): EntityClass? {
        return if (isEntity) {
            val node = typeDeclaration!!
            EntityClass(
                node.name.identifier,
                schemaName,
                active,
                fields,
                transientFields,
                constructors,
                methods,
                node,
                imports,
                packageName ?: "",
                entityTableName,
                oneRelations,
                manyRelations,
                tableIndexes,
                javaFile, source,
                keepSource,
                createTable,
                usedNotNullAnnotation,
                lastField
            )
        } else {
            null
        }
    }
}