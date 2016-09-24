package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.core.dom.Annotation
import org.greenrobot.greendao.annotation.*
import java.io.File
import kotlin.reflect.KClass

/**
 * Visits compilation unit, find if it is an Entity and reads all the required information about it
 */
class EntityClassASTVisitor(val source: String, val classesInPackage: List<String> = emptyList(),
                            val keepFieldsStartLineNumber: Int, val keepFieldsEndLineNumber: Int) : LazyVisitor() {
    var isEntity = false
    var schemaName: String = "default"
    val fields = mutableListOf<EntityField>()
    val transientFields = mutableListOf<TransientField>()
    val legacyTransientFields = mutableListOf<TransientField>()
    val constructors = mutableListOf<Method>()
    val methods = mutableListOf<Method>()
    val imports = mutableListOf<ImportDeclaration>()
    val staticInnerClasses = mutableListOf<String>()
    var packageName: String? = null
    var entityTableName: String? = null
    var typeDeclaration: TypeDeclaration? = null
    val oneRelations = mutableListOf<OneRelation>()
    val manyRelations = mutableListOf<ManyRelation>()
    var tableIndexes = emptyList<TableIndex>()
    var active = false
    var keepSource = false
    var createTable = true
    var generateConstructors = true
    var generateGettersSetters = true
    var protobufClassName: String? = null
    var usedNotNullAnnotation: String? = null
    var lastField: FieldDeclaration? = null

    private val methodAnnotations = mutableListOf<Annotation>()
    private val fieldAnnotations = mutableListOf<Annotation>()

    override fun visit(node: CompilationUnit): Boolean = true

    override fun visit(node: ImportDeclaration): Boolean {
        imports += node
        return true
    }

    override fun visit(node: PackageDeclaration): Boolean {
        packageName = node.name.fullyQualifiedName
        return true
    }

    private fun Annotation.hasType(klass: KClass<*>): Boolean {
        return if (typeName.isSimpleName) {
            typeName.fullyQualifiedName == klass.simpleName && imports.has(klass)
        } else {
            typeName.fullyQualifiedName == klass.qualifiedName
        }
    }

    private fun Type.toVariableType(): VariableType {
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
            typeName(typeDeclaration?.name?.identifier, imports, packageName, classesInPackage)
        } catch (e: IllegalArgumentException) {
            throw RuntimeException("Error processing \"${typeDeclaration?.name?.identifier}\": ${e.message}", e)
        }

    fun visitAnnotation(node: Annotation): Boolean {
        val parent = node.parent
        when (parent) {
            is TypeDeclaration -> {
                when {
                    node.hasType(Entity::class) -> {
                        isEntity = true
                        val entityAnnotation = AnnotationProxy<Entity>(node)
                        schemaName = entityAnnotation.schema
                        active = entityAnnotation.active
                        entityTableName = entityAnnotation.nameInDb.nullIfBlank()
                        createTable = entityAnnotation.createInDb
                        generateConstructors = entityAnnotation.generateConstructors
                        generateGettersSetters = entityAnnotation.generateGettersSetters
                        if (node is NormalAnnotation) {
                            protobufClassName = (node["protobuf"] as? TypeLiteral)?.type?.typeName?.nullIfBlank()
                            if (protobufClassName != null && entityTableName == null) {
                                // TODO remove this requirement (the following is just a workaround to fill
                                // protobufEntity.dbName):
                                // explicitly require table name so the user is aware where both DAOs store their data
                                throw RuntimeException("Set nameInDb in the ${parent.name} @Entity annotation. " +
                                        "An explicit table name is required when specifying a protobuf class.")
                            }
                        }
                        try {
                            tableIndexes = entityAnnotation.indexes.map {
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

    override fun visit(node: MarkerAnnotation): Boolean = visitAnnotation(node)

    override fun visit(node: SingleMemberAnnotation): Boolean = visitAnnotation(node)

    override fun visit(node: NormalAnnotation): Boolean = visitAnnotation(node)

    override fun visit(node: FieldDeclaration): Boolean = isEntity

    override fun endVisit(node: FieldDeclaration) {
        val variableNames = node.fragments()
                .filter { it is VariableDeclarationFragment }
                .map { it as VariableDeclarationFragment }.map { it.name }
        val variableType = node.type.toVariableType()

        val lineNumber = node.lineNumber
        val isInLegacyKeepSection = lineNumber != null
                && lineNumber > keepFieldsStartLineNumber && lineNumber < keepFieldsEndLineNumber

        // check how the field(s) should be treated
        val annotations = fieldAnnotations
        if (annotations.any { it.typeName.fullyQualifiedName == "Transient" }
                || Modifier.isTransient(node.modifiers)
                || Modifier.isStatic(node.modifiers)) {
            // field is considered transient (@Transient, transient or static)
            val generatorHint = annotations.generatorHint
            if (generatorHint != null) {
                // check code is not changed
                if (generatorHint is GeneratorHint.Generated) {
                    node.checkUntouched(generatorHint)
                }
            }
            transientFields += variableNames.map {
                TransientField(Variable(variableType, it.toString()), node, generatorHint)
            }
        } else if (isInLegacyKeepSection) {
            // field in legacy KEEP FIELDS section not yet explicitly marked as transient
            legacyTransientFields += variableNames.map {
                TransientField(Variable(variableType, it.toString()), node, null)
            }
            System.err.println("Field $variableNames in ${node.codePlace} will be annotated with @Transient, " +
                    "you can remove the KEEP FIELDS comments.")
        } else {
            // property field
            when {
                annotations.has<ToOne>() -> {
                    oneRelations += variableNames.map { oneRelation(annotations, it, variableType) }
                }
                annotations.has<ToMany>() -> {
                    manyRelations += variableNames.map { manyRelation(annotations, it, variableType) }
                }
                else -> fields += variableNames.map { entityField(annotations, it, node, variableType) }
            }
        }

        // check what type of not-null annotation is used
        if (usedNotNullAnnotation == null) {
            usedNotNullAnnotation = annotations.find {
                it.typeName.fullyQualifiedName == "NotNull" || it.typeName.fullyQualifiedName == "NonNull"
            }?.typeName?.fullyQualifiedName?.let { "@" + it }
        }

        // clear all collected annotations for this field
        annotations.clear()
        lastField = node
    }

    private val ASTNode.codePlace: String?
        get() = "${typeDeclaration?.name?.identifier}:$lineNumber"

    private val ASTNode.originalCode: String
        get() = source.substring(startPosition..(startPosition + length - 1))

    private fun ASTNode.checkUntouched(hint: GeneratorHint.Generated) {
        if (hint.hash != -1 && hint.hash != CodeCompare.codeHash(this.originalCode)) {
            val place = when (this) {
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
                            variableType: VariableType): OneRelation {
        val proxy = fa.proxy<ToOne>()!!
        return OneRelation(
                Variable(variableType, fieldName.toString()),
                foreignKeyField = proxy.joinProperty.nullIfBlank(),
                columnName = fa.proxy<Property>()?.nameInDb?.nullIfBlank(),
                isNotNull = fa.hasNotNull,
                unique = fa.has<Unique>()
        )
    }

    private fun manyRelation(fa: MutableList<Annotation>, fieldName: SimpleName,
                             variableType: VariableType): ManyRelation {
        val proxy = fa.proxy<ToMany>()!!
        val joinEntityAnnotation = fa.find {
            it.hasType(JoinEntity::class)
        } as? NormalAnnotation
        val orderByAnnotation = fa.proxy<OrderBy>()
        return ManyRelation(
                Variable(variableType, fieldName.toString()),
                mappedBy = proxy.referencedJoinProperty.nullIfBlank(),
                joinOnProperties = proxy.joinProperties.map { JoinOnProperty(it.name, it.referencedName) },
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
        val columnAnnotation = fa.proxy<Property>()
        val indexAnnotation = fa.proxy<Index>()
        val idAnnotation = fa.proxy<Id>()

        if (indexAnnotation?.value?.isNotBlank() ?: false) {
            throw RuntimeException(
                    """greenDAO: setting value on @Index is not supported if @Index is used on the properties
                See '$fieldName' in ${typeDeclaration?.name?.identifier}"""
            )
        }

        val customType = findConvert(fieldName, fa)
        return EntityField(
                variable = Variable(variableType, fieldName.toString()),
                id = idAnnotation?.let { TableId(it.autoincrement) },
                index = indexAnnotation?.let { PropertyIndex(indexAnnotation.name.nullIfBlank(), indexAnnotation.unique) },
                isNotNull = node.type.isPrimitiveType || fa.hasNotNull,
                dbName = columnAnnotation?.nameInDb?.let { it.nullIfBlank() },
                customType = customType,
                unique = fa.has<Unique>()
        )
    }

    private fun findConvert(fieldName: SimpleName, fa: MutableList<Annotation>): CustomType? {
        val convert: Annotation? = fa.find { it.hasType(Convert::class) }
        if (convert !is NormalAnnotation) {
            return null
        }

        val converterClassName = (convert["converter"] as? TypeLiteral)?.type?.typeName
        val columnType = (convert["columnType"] as? TypeLiteral)?.type
        if (converterClassName == null || columnType == null) {
            throw RuntimeException(
                    "greenDAO: Missing @Convert arguments at '$fieldName' in ${typeDeclaration?.name?.identifier}")
        }
        return CustomType(converterClassName, columnType.toVariableType())
    }

    override fun visit(node: MethodDeclaration): Boolean = isEntity

    override fun endVisit(node: MethodDeclaration) {
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

    override fun visit(node: EnumDeclaration): Boolean {
        // collect all inner enums to assert inner custom types as static (enum implies static)
        staticInnerClasses.add(node.name.identifier)
        return false
    }

    override fun visit(node: TypeDeclaration): Boolean {
        if (node.parent is TypeDeclaration) {
            // collect all static inner classes to assert inner converters or custom types as static
            if (Modifier.isStatic(node.modifiers)) {
                staticInnerClasses.add(node.name.identifier)
            }
            // skip inner classes
            return false
        } else {
            this.typeDeclaration = node
            return true
        }
    }

    /**
     * If a type converter is used and the property type or converter type is defined inline, checks that they are
     * defined as static.
     */
    private fun checkInnerCustomTypes() {
        val entityClassName = typeDeclaration?.name?.identifier ?: return
        fields.forEach {
            if (it.customType != null) {
                // if the property type is defined inline, it should be static
                val variableClassName = it.variable.type.name
                checkIfInnerTypeThenStatic(variableClassName, entityClassName)

                // if the converter is defined inline, it should be static
                val converterClassName = it.customType.converterClassName
                checkIfInnerTypeThenStatic(converterClassName, entityClassName)
            }
        }
    }

    private fun checkIfInnerTypeThenStatic(typeClassName: String, outerClassName: String) {
        val split = typeClassName.split(".")
        if (split.size < 2) {
            return // no qualified name
        }
        // get <OuterClass> from a.b.c.<OuterClass>.<InnerClass>
        val qualifiedNames = split.takeLast(2)
        if (outerClassName.equals(qualifiedNames[0])) {
            // check if inner class is static, otherwise warn
            if (!staticInnerClasses.contains(qualifiedNames[1])) {
                throw IllegalArgumentException("Inner class $typeClassName in $outerClassName has to be static. " +
                        "Only static classes are supported if converters or custom types (@Convert) are defined as inner classes.")
            }
        }
    }

    /**
     * Collects parsed information into EntityClass, sets javaFile and source
     * @return null if parsed class it is not an entity
     * */
    fun toEntityClass(javaFile: File, source: String): EntityClass? {
        return if (isEntity) {
            // we only know about all inner classes after visiting all nodes, so do inner type and converter checks here
            checkInnerCustomTypes()

            val node = typeDeclaration!!
            EntityClass(
                    node.name.identifier,
                    schemaName,
                    active,
                    fields,
                    transientFields,
                    legacyTransientFields,
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
                    generateConstructors,
                    generateGettersSetters,
                    protobufClassName,
                    usedNotNullAnnotation,
                    lastField
            )
        } else {
            null
        }
    }
}