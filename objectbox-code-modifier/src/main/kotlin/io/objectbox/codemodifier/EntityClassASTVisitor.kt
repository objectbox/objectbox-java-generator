package io.objectbox.codemodifier

import io.objectbox.annotation.*
import org.greenrobot.eclipse.jdt.core.dom.*
import org.greenrobot.eclipse.jdt.core.dom.Annotation
import java.io.File
import kotlin.reflect.KClass

/**
 * Visits compilation unit, find if it is an Entity and reads all the required information about it
 */
class EntityClassASTVisitor(val source: String, val classesInPackage: List<String> = emptyList()) : LazyVisitor() {
    var isEntity = false
    var schemaName: String = "default"
    val properties = mutableListOf<ParsedProperty>()
    val transientFields = mutableListOf<TransientField>()
    val constructors = mutableListOf<Method>()
    val methods = mutableListOf<Method>()
    val imports = mutableListOf<ImportDeclaration>()
    val staticInnerClasses = mutableListOf<String>()
    var packageName: String? = null
    var entityDbName: String? = null
    var typeDeclaration: TypeDeclaration? = null
    val oneRelations = mutableListOf<ToOneRelation>()
    val manyRelations = mutableListOf<ToManyRelation>()
    var active = false
    var keepSource = false
    var createTable = true
    var generateConstructors = true
    var generateGettersSetters = true
    var protobufClassName: String? = null
    var usedNotNullAnnotation: String? = null
    var lastField: FieldDeclaration? = null
    var entityUid: Long? = null

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
                        // schemaName = "entityAnnotation.schema
                        // active = entityAnnotation.active
                        entityDbName = entityAnnotation.nameInDb.nullIfBlank()
                        // createTable = entityAnnotation.createInDb
                        generateConstructors = entityAnnotation.generateConstructors
                        generateGettersSetters = true // TODO trouble with that - getters and setter are gone in tests - entityAnnotation.generateGettersSetters
                        if (node is NormalAnnotation) {
                            // protobufClassName = (node["protobuf"] as? TypeLiteral)?.type?.typeName?.nullIfBlank()
                            if (protobufClassName != null && entityDbName == null) {
                                // TODO remove this requirement (the following is just a workaround to fill
                                // protobufEntity.dbName):
                                // explicitly require table name so the user is aware where both DAOs store their data
                                throw RuntimeException("Set nameInDb in the ${parent.name} @Entity annotation. " +
                                        "An explicit table name is required when specifying a protobuf class.")
                            }
                        }
                    }
                    node.hasType(Uid::class) -> {
                        entityUid = AnnotationProxy<Uid>(node).value
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
        // There can be multiple variables defined like "int a, b, c" and thus we need to operate on lists here.
        val variableNames = node.fragments()
                .filter { it is VariableDeclarationFragment }
                .map { it as VariableDeclarationFragment }.map { it.name }
        val variableType = node.type.toVariableType()

        // check how the field(s) should be treated
        val annotations = fieldAnnotations
        if (annotations.any { it.typeName.fullyQualifiedName == "Transient" }
                || Modifier.isTransient(node.modifiers) || Modifier.isStatic(node.modifiers)) {
            // field is considered transient (@Transient, transient or static)
            val generatorHint = getGeneratorHint(annotations)
            if (generatorHint != null) {
                // check code is not changed
                if (generatorHint is GeneratorHint.Generated) {
                    node.checkUntouched(generatorHint)
                }
            }
            transientFields += variableNames.map {
                TransientField(Variable(variableType, it.toString()), node, generatorHint)
            }
        } else if (variableType.name == "io.objectbox.relation.ToOne") {
            // TODO
        } else if (has<Relation>(annotations)) {
            if (variableType.name == "java.util.List") {
                manyRelations += variableNames.map { parseRelationToMany(annotations, it, variableType) }
            } else {
                oneRelations += variableNames.map { parseRelationToOne(annotations, it, variableType) }
            }
        } else {
            properties += variableNames.map { parseProperty(annotations, it, node, variableType) }
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

    private fun getGeneratorHint(annotations: List<Annotation>): GeneratorHint? {
        return if (has<Keep>(annotations)) {
            GeneratorHint.Keep
        } else {
            annotations.proxy<Generated>()?.let { GeneratorHint.Generated(it.hash) }
        }
    }

    private fun hasNotNull(annotations: List<Annotation>): Boolean {
        return annotations.any {
            val name = it.typeName.fullyQualifiedName.substringAfterLast('.')
            name == "NotNull" || name == "NonNull" || name == "Nonnull"
        }
    }

    private inline fun <reified A> has(annotations: List<Annotation>): Boolean {
        return annotations.any { it.hasType(A::class) }
    }

    /** Tries to find annotation of specified list, and if any, then create proxy for it */
    private inline fun <reified T : kotlin.Annotation> List<Annotation>.proxy(): T? {
        return find { it.hasType(T::class) }?.let { AnnotationProxy<T>(it) }
    }


    private fun parseRelationToOne(fa: MutableList<Annotation>, fieldName: SimpleName, variableType: VariableType)
            : ToOneRelation {
        val proxy = fa.proxy<Relation>()!!
        return ToOneRelation(
                variable = Variable(variableType, fieldName.toString()),
                // In ObjectBox, we always use a id property (at least for now), defaults to name + "Id" if absent
                targetIdField = proxy.idProperty.nullIfBlank() ?: fieldName.toString() + "Id",
                isNotNull = hasNotNull(fa),
                unique = false //fa.has<Unique>()
        )
    }

    private fun parseRelationToMany(fa: MutableList<Annotation>, fieldName: SimpleName, variableType: VariableType)
            : ToManyRelation {
        val proxy = fa.proxy<Relation>()!!
//        val orderByAnnotation = fa.proxy<OrderBy>()
        return ToManyRelation(
                variable = Variable(variableType, fieldName.toString()),
                mappedBy = proxy.idProperty.nullIfBlank()
//                ,joinOnProperties = proxy.joinProperties.map { JoinOnProperty(it.name, it.referencedName) },
//                order = orderByAnnotation?.let {
//                    val spec = it.value
//                    if (spec.isBlank()) {
//                        emptyList()
//                    } else {
//                        try {
//                            parseIndexSpec(spec)
//                        } catch (e: IllegalArgumentException) {
//                            throw RuntimeException("Can't parse @OrderBy.value for " +
//                                    "${typeDeclaration?.name}.${fieldName} because of: ${e.message}.", e)
//                        }
//                    }
//                }
        )
    }

    private fun parseProperty(fa: MutableList<Annotation>, fieldName: SimpleName,
                              astNode: FieldDeclaration, variableType: VariableType): ParsedProperty {
        val property = fa.proxy<Property>()
        val index = fa.proxy<Index>()
        val id = fa.proxy<Id>()
        val uid = fa.proxy<Uid>()

        return ParsedProperty(
                variable = Variable(variableType, fieldName.toString()),
                astNode = astNode,
                idParams = id?.let { EntityIdParams(false /*it.monotonic*/, it.assignable) },
                index = index?.let { PropertyIndex(null, false /* TODO indexAnnotation.unique*/) },
                isNotNull = astNode.type.isPrimitiveType || hasNotNull(fa),
                dbName = property?.nameInDb?.nullIfBlank(),
                uid = if (uid?.value != 0L) uid?.value else null,
                customType = findConvert(fieldName, fa),
                fieldAccessible = !Modifier.isPrivate(astNode.modifiers)
        )
    }

    private fun findConvert(fieldName: SimpleName, fa: MutableList<Annotation>): CustomType? {
        val convert: Annotation? = fa.find { it.hasType(Convert::class) }
        if (convert !is NormalAnnotation) {
            return null
        }

        val converterClassName = (convert["converter"] as? TypeLiteral)?.type?.typeName
        val columnType = (convert["dbType"] as? TypeLiteral)?.type
        if (converterClassName == null || columnType == null) {
            throw RuntimeException(
                    "@Convert attributes absent for field '$fieldName' in ${typeDeclaration?.name?.identifier}:" +
                            convert.lineNumber + ". Example: @Convert(converter=\"..\", dbType=\"..\")")
        }
        return CustomType(converterClassName, columnType.toVariableType())
    }

    override fun visit(node: MethodDeclaration): Boolean = isEntity

    override fun endVisit(node: MethodDeclaration) {
        val generatorHint = getGeneratorHint(methodAnnotations)
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
        properties.forEach {
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
        if (outerClassName == qualifiedNames[0]) {
            // check if inner class is static, otherwise warn
            if (!staticInnerClasses.contains(qualifiedNames[1])) {
                throw IllegalArgumentException("Inner class $typeClassName in $outerClassName has to be static. " +
                        "Only static classes are supported if converters or custom types (@Convert) are defined as inner classes.")
            }
        }
    }

    /**
     * Collects parsed information into ParsedEntity, sets javaFile and source
     * @return null if parsed class it is not an entity
     * */
    fun createParsedEntity(javaFile: File, source: String): ParsedEntity? {
        return if (isEntity) {
            // we only know about all inner classes after visiting all nodes, so do inner type and converter checks here
            checkInnerCustomTypes()

            val node = typeDeclaration!!
            ParsedEntity(
                    name = node.name.identifier,
                    schema = schemaName,
                    active = active,
                    properties = properties,
                    transientFields = transientFields,
                    constructors = constructors,
                    methods = methods,
                    node = node,
                    imports = imports,
                    packageName = packageName ?: "",
                    dbName = entityDbName,
                    uid = if (entityUid != null && entityUid != 0L) entityUid else null,
                    toOneRelations = oneRelations,
                    toManyRelations = manyRelations,
                    sourceFile = javaFile,
                    source = source,
                    keepSource = keepSource,
                    createInDb = createTable,
                    generateConstructors = generateConstructors,
                    generateGettersSetters = generateGettersSetters,
                    protobufClassName = protobufClassName,
                    notNullAnnotation = usedNotNullAnnotation,
                    lastFieldDeclaration = lastField
            )
        } else {
            null
        }
    }
}