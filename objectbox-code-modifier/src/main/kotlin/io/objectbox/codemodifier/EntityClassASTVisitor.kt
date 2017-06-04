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
    // TODO do we need all those members (vs. parsed model)?

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
    var keepSource = false
    var createTable = true
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
                        // schemaName = entityAnnotation.schema
                        if (node is NormalAnnotation) {
                            // protobufClassName = (node["protobuf"] as? TypeLiteral)?.type?.typeName?.nullIfBlank()
                            if (protobufClassName != null && entityDbName == null) {
                                // TODO remove this requirement (the following is just a workaround to fill
                                // protobufEntity.dbName):
                                // explicitly require table name so the user is aware where both DAOs store their data
                                throw ParseException("Set nameInDb in the ${parent.name} @Entity annotation. " +
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
                    node.hasType(NameInDb::class) -> {
                        entityDbName = AnnotationProxy<NameInDb>(node).value.nullIfBlank()
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
        val variableNames = variableNames(node)
        val variableType = node.type.toVariableType()

        // check how the field(s) should be treated
        if (fieldAnnotations.any { it.typeName.fullyQualifiedName == "Transient" }
                || Modifier.isTransient(node.modifiers) || Modifier.isStatic(node.modifiers)) {
            // field is considered transient (@Transient, transient or static)
            val generatorHint = getGeneratorHint(fieldAnnotations)
            if (generatorHint != null) {
                // check code is not changed
                if (generatorHint is GeneratorHint.Generated) {
                    node.checkUntouched(generatorHint)
                }
            }
            transientFields += variableNames.map {
                TransientField(Variable(variableType, it.toString()), node, generatorHint)
            }
        } else {
            for (variableName in variableNames) {
                parseNonTransientField(node, fieldAnnotations, variableType, variableName)
            }
        }

        // check what type of not-null annotation is used
        if (usedNotNullAnnotation == null) {
            usedNotNullAnnotation = fieldAnnotations.find {
                // TODO there are more null related relations
                it.typeName.fullyQualifiedName == "NotNull" || it.typeName.fullyQualifiedName == "NonNull"
            }?.typeName?.fullyQualifiedName?.let { "@" + it }
        }

        // clear all collected annotations for this field
        fieldAnnotations.clear()
        lastField = node
    }

    /** There can be multiple variables defined like "int a, b, c" */
    private fun variableNames(node: FieldDeclaration): List<SimpleName> {
        val variableNames = node.fragments()
                .filter { it is VariableDeclarationFragment }
                .map { (it as VariableDeclarationFragment).name }
        return variableNames
    }

    private fun parseNonTransientField(node: FieldDeclaration, annotations: MutableList<Annotation>,
                                       variableType: VariableType, variableName: SimpleName) {
        if (variableType.name == "io.objectbox.relation.ToOne" && !has<Generated>(fieldAnnotations)) {
            if (Modifier.isPrivate(node.modifiers)) {
                throw exceptionWithLocation("Currently, ToOne's may not be private, change it to package visible: " +
                        variableName.identifier, node)
            }
            oneRelations += parseRelationToOne(annotations, variableName, variableType, node, true)
        } else if (!has<Convert>(annotations) &&
                (variableType.name == "java.util.List" || variableType.name == "io.objectbox.relation.ToMany")) {
            manyRelations += parseRelationToMany(annotations, variableName, variableType, node)
        } else if (has<Relation>(annotations)) {
            oneRelations += parseRelationToOne(annotations, variableName, variableType, node, false)
        } else {
            properties += parseProperty(node, annotations, variableType, variableName)
        }
    }

    private fun exceptionWithLocation(msg: String, node: ASTNode): ParseException {
        var additionalInfo = if (node is FieldDeclaration) {
            val variableNames = variableNames(node).joinToString()
            "${node.type.typeName} $variableNames in "
        } else ""

        return ParseException(msg + " ($additionalInfo${typeDeclaration?.name?.identifier}:${node.lineNumber})")
    }

    private fun ASTNode.checkUntouched(hint: GeneratorHint.Generated) {
        val originalCode = source.substring(startPosition..(startPosition + length - 1))
        if (hint.hash != -1 && hint.hash != CodeCompare.codeHash(originalCode)) {
            val place = when (this) {
                is MethodDeclaration -> if (this.isConstructor) "Constructor" else "Method '$name'"
                is FieldDeclaration -> "Field '${originalCode.trim()}'"
                else -> "Node"
            }
            val codePlace = "${typeDeclaration?.name?.identifier}:$lineNumber"
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
            annotations.proxy<Generated>()?.let { GeneratorHint.Generated(it.value) }
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


    /**
     * @param plainToOne if there is a ToOne without a relation property of the target type
     * @param variableType either ToOne (plainToOne) or the target entity property
     * @param node either ToOne (plainToOne) or the target entity property
     */
    private fun parseRelationToOne(annotations: MutableList<Annotation>, fieldName: SimpleName,
                                   variableType: VariableType, node: FieldDeclaration, plainToOne: Boolean)
            : ToOneRelation {

        val targetType = if (plainToOne) {
            variableType.typeArguments?.singleOrNull()
                    ?: throw exceptionWithLocation("You must specify a type argument for ToOne", node)
        } else variableType

        return ToOneRelation(
                variable = Variable(variableType, fieldName.toString()),
                targetType = targetType,
                targetIdName = annotations.proxy<Relation>()?.idProperty?.nullIfBlank(),
                targetIdDbName = annotations.proxy<NameInDb>()?.value,
                uid = annotations.proxy<Uid>()?.value,
                isNotNull = hasNotNull(annotations),
                variableIsToOne = plainToOne,
                astNode = node,
                unique = false, //fa.has<Unique>()
                // If field is not a (plain) to-one, we generate to to-one field in an accessible way
                toOneFieldAccessible = !plainToOne || !Modifier.isPrivate(node.modifiers)
        )
    }

    private fun parseRelationToMany(fa: MutableList<Annotation>, fieldName: SimpleName, variableType: VariableType,
                                    node: FieldDeclaration): ToManyRelation {
        val backlink = fa.proxy<Backlink>()
                ?: throw exceptionWithLocation("For now, all ToMany relations must be backlinks" +
                "(annotate with @Backlink with a ToOne counterpart in the target entity)", node)

        //        val orderByAnnotation = fa.proxy<OrderBy>()
        return ToManyRelation(
                variable = Variable(variableType, fieldName.toString()),
                backlinkName = backlink.to.nullIfBlank(),
                astNode = node
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

    private fun parseProperty(astNode: FieldDeclaration, annotations: MutableList<Annotation>,
                              variableType: VariableType, fieldName: SimpleName): ParsedProperty {
        val nameInDb = annotations.proxy<NameInDb>()
        val index = annotations.proxy<Index>()
        val id = annotations.proxy<Id>()
        val uid = annotations.proxy<Uid>()

        return ParsedProperty(
                variable = Variable(variableType, fieldName.toString()),
                astNode = astNode,
                idParams = id?.let { EntityIdParams(false /*it.monotonic*/, it.assignable) },
                index = index?.let { PropertyIndex(null, false /* TODO indexAnnotation.unique*/) },
                isNotNull = astNode.type.isPrimitiveType || hasNotNull(annotations),
                dbName = nameInDb?.value?.nullIfBlank(),
                uid = if (uid?.value != 0L) uid?.value else null,
                customType = findConvert(astNode, fieldName, annotations),
                fieldAccessible = !Modifier.isPrivate(astNode.modifiers)
        )
    }

    private fun findConvert(astNode: FieldDeclaration, fieldName: SimpleName,
                            annotations: MutableList<Annotation>): CustomType? {
        val convertAnnotation: Annotation = annotations.find { it.hasType(Convert::class) } ?: return null
        val convert = convertAnnotation as? NormalAnnotation
        val converterClassName = (convert?.get("converter") as? TypeLiteral)?.type?.typeName
        val columnType = (convert?.get("dbType") as? TypeLiteral)?.type
        if (converterClassName == null || columnType == null) {
            throw exceptionWithLocation("@Convert attributes absent for field '$fieldName'. Example: " +
                    "@Convert(converter=\"..\", dbType=\"..\")", astNode)
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
                    generateConstructors = true,
                    protobufClassName = protobufClassName,
                    notNullAnnotation = usedNotNullAnnotation,
                    lastFieldDeclaration = lastField
            )
        } else {
            null
        }
    }
}