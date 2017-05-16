package io.objectbox.codemodifier

import io.objectbox.generator.BoxGenerator
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.Schema
import org.greenrobot.eclipse.jdt.core.JavaCore
import org.greenrobot.eclipse.jdt.core.dom.FieldDeclaration
import org.greenrobot.eclipse.jdt.core.dom.VariableDeclarationFragment
import org.greenrobot.eclipse.jdt.internal.compiler.impl.CompilerOptions
import java.io.File
import java.util.Hashtable

/**
 * Main generator triggered by plugin.
 * - triggers parsing of entities
 * - runs generation of dao classes within {@link org.greenrobot.greendao.generator.DaoGenerator}
 * - runs parsing and transformation of Entity classes using [EntityClassTransformer]
 */
// TODO refactor, e.g. entity transformation could be completely delegated to [EntityClassTransformer]
class ObjectBoxGenerator(val formattingOptions: FormattingOptions? = null,
                         val skipTestGeneration: List<String> = emptyList(),
                         val daoCompat: Boolean = false,
                         encoding: String = "UTF-8") {

    companion object {
        val JAVA_LEVEL: String = CompilerOptions.VERSION_1_7
    }

    val jdtOptions: Hashtable<String, String> = JavaCore.getOptions()

    val entityParser: EntityParser

    init {
        jdtOptions.put(CompilerOptions.OPTION_Source, JAVA_LEVEL)
        jdtOptions.put(CompilerOptions.OPTION_Compliance, JAVA_LEVEL)
        // it could be the encoding is never used by JDT itself for our use case, but just to be sure (and for future)
        jdtOptions.put(CompilerOptions.OPTION_Encoding, encoding)

        entityParser = EntityParser(jdtOptions, encoding)
    }

    /** Triggered by plugin. */
    fun run(sourceFiles: Iterable<File>, schemaOptions: Map<String, SchemaOptions>) {
        require(schemaOptions.size > 0) { "There should be options for at least one schema" }

        val parsedEntities = entityParser.parseEntityFiles(sourceFiles)
        if (parsedEntities.isEmpty()) {
            System.err.println("No entities found")
        }
        parsedEntities.forEach { entry ->
            val (schemaName, schemaEntities) = entry
            val options = schemaOptions[schemaName]
                    ?: throw RuntimeException("Undefined schema \"$schemaName\"" +
                    " (referenced in entities: " + "${schemaEntities.joinToString()}). " +
                    "Please, define non-default schemas explicitly inside build.gradle")
            generateSchema(schemaEntities, options)
        }
    }

    fun generateSchema(parsedEntities: List<ParsedEntity>, options: SchemaOptions) {
        val outputDir = options.outputDir
        val testsOutputDir = options.testsOutputDir

        val idSync = IdSync(options.idModelFile)
        idSync.sync(parsedEntities)

        // take explicitly specified package name, or package name of the first entity
        val schema = Schema(options.name, options.version, options.daoPackage ?: parsedEntities.first().packageName)
        schema.lastEntityId = idSync.lastEntityId
        schema.lastIndexId = idSync.lastIndexId
        val mapping: Map<ParsedEntity, Entity> =
                GreendaoModelTranslator.convert(parsedEntities, schema, options.daoPackage, idSync)

        if (skipTestGeneration.isNotEmpty()) {
            schema.entities.forEach { e ->
                val qualifiedName = "${e.javaPackage}.${e.className}"
                e.isSkipGenerationTest = skipTestGeneration.any { qualifiedName.endsWith(it) }
            }
        }

        outputDir.mkdirs()
        testsOutputDir?.mkdirs()

        BoxGenerator(daoCompat).generateAll(schema, outputDir.path, outputDir.path, testsOutputDir?.path)

        // modify existing entity classes after using DaoGenerator, because not all schema properties are available before
        // for each entity add missing fields/methods/constructors
        parsedEntities.forEach { parsedEntity ->
            if (parsedEntity.keepSource) {
                checkClass(parsedEntity)
                println("Keeping source for ${parsedEntity.name}")
            } else {
                transformClass(idSync, parsedEntity, mapping)
            }
        }

        val keptClasses = parsedEntities.count { it.keepSource }
        val keptMethods = parsedEntities.sumBy { it.constructors.count { it.keep } + it.methods.count { it.keep } }
        if (keptClasses + keptMethods > 0) {
            System.err.println(
                    "Kept source for $keptClasses classes and $keptMethods methods because of @Keep annotation")
        }
    }

    private fun checkClass(parsedEntity: ParsedEntity) {
        val fieldVars = parsedEntity.properties.map { it.variable }
        if (parsedEntity.constructors.none() {
            it.hasFullSignature(parsedEntity.name, fieldVars)
        }) {
            throw RuntimeException(
                    "Can't find constructor for entity ${parsedEntity.name} with all persistent fields. " +
                            "Note parameter names of such constructor should be equal to field names"
            )
        }
    }

    private fun transformClass(idSync: IdSync, parsedEntity: ParsedEntity, mapping: Map<ParsedEntity, Entity>) {
        val entity = mapping[parsedEntity]!!
        val transformer = EntityClassTransformer(parsedEntity, jdtOptions, formattingOptions)

        transformer.ensureImport("io.objectbox.annotation.Generated")
        transformer.ensureImport("io.objectbox.annotation.apihint.Internal")

        // add everything (fields, constructors, methods) in reverse as transformer writes in reverse direction
        if (entity.active) {
            transformer.ensureImport("io.objectbox.BoxStore")

            generateToManyRelations(entity, transformer)
            generateToOneRelations(entity, parsedEntity, transformer)
        }

        generateGettersAndSetters(parsedEntity, transformer)
        generateConstructors(parsedEntity, transformer)

        if (entity.active) {
            // Currently myBox is not populated in native code
//            val entityType = VariableType("${entity.javaPackage}.${entity.className}", false, entity.className)
//            transformer.defField("__myBox", VariableType("io.objectbox.Box", false, "Box", listOf(entityType)),
//                    "Used for active entity operations.")

            val type = VariableType("io.objectbox.BoxStore", false, "BoxStore")
            transformer.defineTransientGeneratedField("__boxStore", type, "Used to resolve relations")
        }

        parsedEntity.propertiesToGenerate.forEach {
            transformer.defineProperty(it.variable.name, it.variable.type)
        }

        // add UID values to any UID entity/property annotations that are missing them: @Uid -> @Uid(42L)
        val idSyncEntity = idSync.get(parsedEntity)
        transformer.checkInsertUidAnnotationValue(parsedEntity.node, idSyncEntity.uid)
        parsedEntity.properties.forEach { property ->
            if (property.astNode != null) {
                val idSyncProperty = idSync.get(property)
                transformer.checkInsertUidAnnotationValue(property.astNode, idSyncProperty.uid)
            }
        }

        transformer.writeToFile()
    }

    private fun generateConstructors(parsedEntity: ParsedEntity, transformer: EntityClassTransformer) {
        if (parsedEntity.generateConstructors) {
            // check there is need to generate default constructor to do not hide implicit one
            val properties = parsedEntity.properties
            if (properties.isNotEmpty()
                    && parsedEntity.constructors.none { it.parameters.isEmpty() && !it.generated }) {
                transformer.defConstructor(emptyList()) {
                    """ @Generated($HASH_STUB)
                        public ${parsedEntity.name}() {
                        }"""
                }
            }

            // generate all fields constructor
            transformer.defConstructor(properties.map { it.variable.type.name }) {
                Templates.entity.constructor(parsedEntity.name, parsedEntity.properties,
                        parsedEntity.notNullAnnotation ?: "@NotNull")
            }
        } else {
            // DAOs require at minimum a default constructor, so:
            transformer.ensureDefaultConstructor()
        }
    }

    private fun generateGettersAndSetters(parsedEntity: ParsedEntity, transformer: EntityClassTransformer) {
        // add everything (fields, set before get) in reverse as transformer writes in reverse direction
        parsedEntity.properties.reversed().filter {
            // always add getter/setter if daoCompat is enabled as compat DAO classes use them
            // otherwise only add getter/setter if field is not visible for Cursor
            daoCompat || !it.fieldAccessible
        }.forEach { field ->
            // only define missing getters/setters to avoid overwriting customized getters/setters
            transformer.defMethodIfMissing("set${field.variable.name.capitalize()}", field.variable.type.name) {
                Templates.entity.fieldSet(field.variable)
            }

            transformer.defMethodIfMissing("get${field.variable.name.capitalize()}") {
                Templates.entity.fieldGet(field.variable)
            }
        }
    }

    private fun generateToOneRelations(entity: Entity, parsedEntity: ParsedEntity, transformer: EntityClassTransformer) {
        if (entity.toOneRelations.isEmpty()) return;
        transformer.ensureImport("io.objectbox.relation.ToOne")

        // add everything in reverse as transformer writes in reverse direction
        val toOnes = entity.toOneRelations.reversed()

        // Methods first, then fields TODO have two insertion points in AST for a) fields and b) methods
        toOnes.filter { !it.isPlainToOne }.forEach { toOne ->
                val targetIdProperty = toOne.targetIdProperty
                transformer.defMethod("set${toOne.name.capitalize()}", toOne.targetEntity.className) {
                    if (parsedEntity.notNullAnnotation == null && targetIdProperty.isNotNull) {
                        // Not yet supported
                        //transformer.ensureImport("io.objectbox.annotation.NotNull")
                    }
                    Templates.entity.oneRelationSetter(toOne, parsedEntity.notNullAnnotation ?: "@NotNull")
                }

                val getterName = "get${toOne.name.capitalize()}"
                transformer.defMethod(getterName) {
                    Templates.entity.oneRelationGetter(toOne, entity)
                }
        }

        // Fields
        toOnes.forEach { toOne ->
            if(toOne.isPlainToOne) {
                val field = toOne.parsedElement as FieldDeclaration
                field.fragments().forEach { fragment ->
                    if(fragment is VariableDeclarationFragment) {
                        if(fragment.initializer == null) {
                            val initCode = "new ToOne<>(this, ${entity.className}_.${toOne.name})"
                            transformer.addInitializer(field, fragment.name.identifier, initCode)
                        }
                    }
                }
            } else {
                val toOneTypeArgs = listOf(
                        VariableType(toOne.targetEntity.className, false, toOne.targetEntity.javaPackage)
                )
                val variableType = VariableType("ToOne", false, "ToOne", toOneTypeArgs)
                val assignment = "new ToOne<>(this, ${entity.className}_.${toOne.name})"
                transformer.defineTransientGeneratedField("${toOne.nameToOne}", variableType, null, null, assignment)
            }
        }
    }

    private fun generateToManyRelations(entity: Entity, transformer: EntityClassTransformer) {
        if (entity.toManyRelations.isEmpty()) return
        transformer.ensureImport("io.objectbox.relation.ToMany")

        // add everything in reverse as transformer writes in reverse direction
        entity.toManyRelations.reversed().forEach { toMany ->
            transformer.ensureImport("${toMany.targetEntity.javaPackage}.${toMany.targetEntity.className}_")

            val field = toMany.parsedElement as FieldDeclaration
            field.fragments().forEach { fragment ->
                if(fragment is VariableDeclarationFragment) {
                    if(fragment.initializer == null) {
                        val initCode = "new ToMany<>(this, ${entity.className}_.${toMany.name})"
                        transformer.addInitializer(field, fragment.name.identifier, initCode)
                    }
                }
            }
        }
    }

}