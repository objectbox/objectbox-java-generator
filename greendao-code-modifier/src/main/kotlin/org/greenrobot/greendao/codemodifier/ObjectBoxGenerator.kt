package org.greenrobot.greendao.codemodifier

import io.objectbox.generator.BoxGenerator
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import org.greenrobot.greendao.generator.Entity
import org.greenrobot.greendao.generator.Schema
import org.greenrobot.idsync.IdSync
import java.io.File

/**
 * Main generator.
 * - triggers parsing of entities
 * - runs generation of dao classes within {@link org.greenrobot.greendao.generator.DaoGenerator}
 * - runs parsing and transformation of Entity classes using [EntityClassTransformer]
 */
// TODO refactor, e.g. entity transformation could be completely delegated to [EntityClassTransformer]
class ObjectBoxGenerator(val formattingOptions: FormattingOptions? = null,
                         val skipTestGeneration: List<String> = emptyList(),
                         val daoCompat: Boolean = false,
                         encoding: String = "UTF-8") {
    val jdtOptions: MutableMap<Any, Any> = JavaCore.getOptions()

    val entityClassParser: EntityClassParser

    init {
        jdtOptions.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_7)
        jdtOptions.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_7)
        // it could be the encoding is never used by JDT itself for our use case, but just to be sure (and for future)
        jdtOptions.put(CompilerOptions.OPTION_Encoding, encoding)

        entityClassParser = EntityClassParser(jdtOptions, encoding)
    }

    fun run(sourceFiles: Iterable<File>, schemaOptions: Map<String, SchemaOptions>) {
        require(schemaOptions.size > 0) { "There should be options for at least one schema" }

        val parsedEntities = parseEntityFiles(sourceFiles)
        if (parsedEntities.isEmpty()) {
            System.err.println("No entities found among specified files")
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

    private fun parseEntityFiles(sourceFiles: Iterable<File>): Map<String, List<ParsedEntity>> {
        val start = System.currentTimeMillis()
        val classesByDir = sourceFiles.map { it.parentFile }.distinct().map {
            it to it.getJavaClassNames()
        }.toMap()

        val parsedEntities = sourceFiles.asSequence()
                .map {
                    val entity = entityClassParser.parse(it, classesByDir[it.parentFile]!!)
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

    /**
     * Look at to-one relation ID properties to:
     * * generate missing properties
     * * add the index so IdSync can assign a index ID
     */
    private fun parse2ndPass(parsedEntities: List<ParsedEntity>) {
        // val parsedEntitiesByName = parsedEntities.groupBy { it.dbName ?: it.name }
        parsedEntities.forEach { parsedEntity ->
            parsedEntity.oneRelations.forEach { toOne ->
                val idName = toOne.foreignKeyField ?: throw RuntimeException("Unnamed idProperty for to-one " +
                        "@Relation")
                var parsedProperty: ParsedProperty? = parsedEntity.properties.find { it.variable.name == idName }
                if (parsedProperty == null) {
                    if (parsedEntity.keepSource) {
                        throw RuntimeException("No idProperty available with the name \"${toOne.foreignKeyField}\"" +
                                " (needed for @Relation)")
                    } else {
                        // Property does not exist yet, adding it to parsedEntity.propertiesToGenerate will take care
                        parsedProperty = ParsedProperty(variable = Variable(VariableType("long", true, "long"), idName))
                        parsedEntity.properties.add(parsedProperty)
                        parsedEntity.propertiesToGenerate.add(parsedProperty)
                    }
                }
                if (parsedProperty.index == null) {
                    parsedProperty.index = PropertyIndex(null, false)
                }
            }
        }
    }

    fun generateSchema(entities: List<ParsedEntity>, options: SchemaOptions) {
        val outputDir = options.outputDir
        val testsOutputDir = options.testsOutputDir

        val idSync = IdSync(options.idModelFile)
        idSync.sync(entities)

        // take explicitly specified package name, or package name of the first entity
        val schema = Schema(options.name, options.version, options.daoPackage ?: entities.first().packageName)
        schema.lastEntityId = idSync.lastEntityId
        schema.lastIndexId = idSync.lastIndexId
        val mapping: Map<ParsedEntity, Entity> =
                GreendaoModelTranslator.translate(entities, schema, options.daoPackage, idSync)

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
        entities.forEach { entityClass ->
            if (entityClass.keepSource) {
                checkClass(entityClass)
                println("Keep source for ${entityClass.name}")
            } else {
                transformClass(entityClass, mapping)
            }
        }

        val keptClasses = entities.count { it.keepSource }
        val keptMethods = entities.sumBy { it.constructors.count { it.keep } + it.methods.count { it.keep } }
        if (keptClasses + keptMethods > 0) {
            System.err.println(
                    "Kept source for $keptClasses classes and $keptMethods methods because of @Keep annotation")
        }
    }

    private fun checkClass(parsedEntity: ParsedEntity) {
        val propertiesInConstructorOrder = parsedEntity.getPropertiesInConstructorOrder()
        val noConstructor = propertiesInConstructorOrder == null && run {
            val fieldVars = parsedEntity.properties.map { it.variable }
            parsedEntity.constructors.none {
                it.hasFullSignature(parsedEntity.name, fieldVars)
            }
        }
        if (noConstructor) {
            throw RuntimeException(
                    "Can't find constructor for entity ${parsedEntity.name} with all persistent fields. " +
                            "Note parameter names of such constructor should be equal to field names"
            )
        }
    }

    private fun transformClass(parsedEntity: ParsedEntity, mapping: Map<ParsedEntity, Entity>) {
        val entity = mapping[parsedEntity]!!
        val transformer = EntityClassTransformer(parsedEntity, jdtOptions, formattingOptions)

        transformer.ensureImport("io.objectbox.annotation.Generated")

        // add everything (fields, constructors, methods) in reverse as transformer writes in reverse direction
        if (entity.active) {
            transformer.ensureImport("io.objectbox.Box")
            transformer.ensureImport("io.objectbox.BoxStore")
            transformer.ensureImport("io.objectbox.exception.DbDetachedException")
            transformer.ensureImport("io.objectbox.exception.DbException")

            generateActiveMethodsAndFields(entity, transformer)
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
            transformer.defineTransientGeneratedField("__boxStore", VariableType("io.objectbox.BoxStore", false, "BoxStore"),
                    "Used to resolve relations")
        }

        parsedEntity.propertiesToGenerate.forEach {
            transformer.defineProperty(it.variable.name, it.variable.type)
        }

        transformer.writeToFile()
    }

    private fun generateConstructors(parsedEntity: ParsedEntity, transformer: EntityClassTransformer) {
        if (parsedEntity.generateConstructors) {
            // check there is need to generate default constructor to do not hide implicit one
            val properties = parsedEntity.getPropertiesInConstructorOrder() ?: parsedEntity.properties
            if (properties.isNotEmpty()
                    && parsedEntity.constructors.none { it.parameters.isEmpty() && !it.generated }) {
                transformer.defConstructor(emptyList()) {
                    """ @Generated(hash = $HASH_STUB)
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
        if (!parsedEntity.generateGettersSetters) {
            println("Not generating getters or setters for ${parsedEntity.name}.")
            return
        }
        // define missing getters and setters
        // add everything (fields, set before get) in reverse as transformer writes in reverse direction
        parsedEntity.properties.reversed().forEach { field ->
            transformer.defMethodIfMissing("set${field.variable.name.capitalize()}", field.variable.type.name) {
                Templates.entity.fieldSet(field.variable)
            }

            transformer.defMethodIfMissing("get${field.variable.name.capitalize()}") {
                Templates.entity.fieldGet(field.variable)
            }
        }
    }

    private fun generateToOneRelations(entity: Entity, parsedEntity: ParsedEntity, transformer: EntityClassTransformer) {
        // add everything in reverse as transformer writes in reverse direction
        entity.toOneRelations.reversed().forEach { toOne ->
            // define methods
            transformer.defMethod("set${toOne.name.capitalize()}", toOne.targetEntity.className) {
                if (parsedEntity.notNullAnnotation == null && toOne.fkProperties[0].isNotNull) {
                    // Not yet supported
                    //transformer.ensureImport("io.objectbox.annotation.NotNull")
                }
                Templates.entity.oneRelationSetter(toOne, parsedEntity.notNullAnnotation ?: "@NotNull")
            }

            // Do we need peek at all? User can implement it if required.
//            if (!toOne.isUseFkProperty) {
//                transformer.defMethod("peek${toOne.name.capitalize()}") {
//                    Templates.entity.oneRelationPeek(toOne)
//                }
//            }

            transformer.defMethod("get${toOne.name.capitalize()}") {
                Templates.entity.oneRelationGetter(toOne, entity)
            }

            // define fields
            if (toOne.isUseFkProperty) {
                transformer.defineTransientGeneratedField("${toOne.name}__resolvedKey",
                        VariableType(toOne.resolvedKeyJavaType[0], false, toOne.resolvedKeyJavaType[0]))
            } else {
                transformer.defineTransientGeneratedField("${toOne.name}__refreshed", VariableType("boolean", true, "boolean"))
            }
        }
    }

    private fun generateToManyRelations(entity: Entity, transformer: EntityClassTransformer) {
        // add everything in reverse as transformer writes in reverse direction
        entity.toManyRelations.reversed().forEach { toMany ->
            transformer.ensureImport("${toMany.targetEntity.javaPackage}.${toMany.targetEntity.className}_")

            transformer.defMethod("reset${toMany.name.capitalize()}") {
                Templates.entity.manyRelationReset(toMany)
            }

            transformer.defMethod("get${toMany.name.capitalize()}") {
                Templates.entity.manyRelationGetter(toMany, entity)
            }
        }
    }

    private fun generateActiveMethodsAndFields(entity: Entity, transformer: EntityClassTransformer) {
        // add everything in reverse as transformer writes in reverse direction
        transformer.defMethod("put") {
            Templates.entity.activePut(entity)
        }

//        transformer.defMethod("refresh") {
//            Templates.entity.activeRefresh(entity)
//        }

        transformer.defMethod("remove") {
            Templates.entity.activeRemove(entity)
        }
    }
}