package org.greenrobot.greendao.codemodifier

import org.greenrobot.greendao.generator.DaoGenerator
import org.greenrobot.greendao.generator.Entity
import org.greenrobot.greendao.generator.Schema
import java.io.File

/**
 * Main generator.
 * - runs generation of dao classes within {@link org.greenrobot.greendao.generator.DaoGenerator}
 * - runs parsing and transformation of Entity classes using {@link EntityClassTransformer}
 */
class Greendao3Generator(formattingOptions: FormattingOptions? = null,
                         val skipTestGeneration: List<String> = emptyList(),
                         encoding: String = "UTF-8") {
    val context = JdtCodeContext(formattingOptions, encoding)

    fun run(sourceFiles: Iterable<File>,
            schemaOptions: Map<String, SchemaOptions>) {
        require(schemaOptions.size > 0) { "There should be options for at least one schema" }

        val classesByDir = sourceFiles.map { it.parentFile }.distinct().map {
            it to it.getJavaClassNames()
        }.toMap()

        val start = System.currentTimeMillis()
        val entities = sourceFiles.asSequence()
                .map {
                    val entity = context.parse(it, classesByDir[it.parentFile]!!)
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
        println("Parsed ${entities.size} entities in $time ms among ${sourceFiles.count()} source files: " +
                "${entities.asSequence().map { it.name }.joinToString()}")

        if (entities.isNotEmpty()) {
            entities.groupBy { it.schema }.forEach { entry ->
                val (schemaName, schemaEntities) = entry
                val options = schemaOptions[schemaName] ?: run {
                    val affectedEntities = entities.filter { it.schema == schemaName }.map { it.name }.joinToString()
                    throw RuntimeException(
                            """
                        Undefined schema \"$schemaName\" (referenced in entities: $affectedEntities).
                        Please, define non-default schemas explicitly inside build.gradle
                        """.trimIndent()
                    )
                }

                generateSchema(schemaEntities, options)
            }
        } else {
            System.err.println("No entities found among specified files")
        }
    }

    fun generateSchema(entities: List<ParsedEntity>, options: SchemaOptions) {
        val outputDir = options.outputDir
        val testsOutputDir = options.testsOutputDir

        // take explicitly specified package name, or package name of the first entity
        val schema = Schema(options.name, options.version, options.daoPackage ?: entities.first().packageName)
        val mapping = GreendaoModelTranslator.translate(entities, schema, options.daoPackage)

        if (skipTestGeneration.isNotEmpty()) {
            schema.entities.forEach { e ->
                val qualifiedName = "${e.javaPackage}.${e.className}"
                e.isSkipGenerationTest = skipTestGeneration.any { qualifiedName.endsWith(it) }
            }
        }

        outputDir.mkdirs()
        testsOutputDir?.mkdirs()

        DaoGenerator().generateAll(schema, outputDir.path, outputDir.path, testsOutputDir?.path)

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
        val daoPackage = entity.schema.defaultJavaPackage
        val transformer = context.transformer(parsedEntity)

        transformer.ensureImport("org.greenrobot.greendao.annotation.Generated")
        transformer.annotateLegacyKeepFields()

        // add everything (fields, constructors, methods) in reverse as transformer writes in reverse direction
        val daoSessionVarName = "${entity.schema.prefix}DaoSession"
        if (entity.active) {
            transformer.ensureImport("org.greenrobot.greendao.DaoException")

            transformer.defMethod("__setDaoSession", "$daoPackage.$daoSessionVarName") {
                Templates.entity.daoSessionSetter(entity)
            }

            generateActiveMethodsAndFields(transformer)
            generateToManyRelations(entity, transformer)
            generateToOneRelations(entity, parsedEntity, transformer)
        }

        generateGettersAndSetters(parsedEntity, transformer)
        generateConstructors(parsedEntity, transformer)

        if (entity.active) {
            transformer.defField("myDao", VariableType("${entity.javaPackageDao}.${entity.classNameDao}", false, entity.classNameDao),
                    "Used for active entity operations.")
            transformer.defField("daoSession", VariableType("$daoPackage.$daoSessionVarName", false, daoSessionVarName),
                    "Used to resolve relations")
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
            transformer.ensureImport("${toOne.targetEntity.javaPackageDao}.${toOne.targetEntity.classNameDao}")

            // define methods
            transformer.defMethod("set${toOne.name.capitalize()}", toOne.targetEntity.className) {
                if (parsedEntity.notNullAnnotation == null && toOne.fkProperties[0].isNotNull) {
                    transformer.ensureImport("org.greenrobot.greendao.annotation.NotNull")
                }
                Templates.entity.oneRelationSetter(toOne, parsedEntity.notNullAnnotation ?: "@NotNull")
            }

            if (!toOne.isUseFkProperty) {
                transformer.defMethod("peak${toOne.name.capitalize()}") {
                    Templates.entity.oneRelationPeek(toOne)
                }
            }

            transformer.defMethod("get${toOne.name.capitalize()}") {
                Templates.entity.oneRelationGetter(toOne, entity)
            }

            // define fields
            if (toOne.isUseFkProperty) {
                transformer.defField("${toOne.name}__resolvedKey",
                        VariableType(toOne.resolvedKeyJavaType[0], false, toOne.resolvedKeyJavaType[0]))
            } else {
                transformer.defField("${toOne.name}__refreshed", VariableType("boolean", true, "boolean"))
            }
        }
    }

    private fun generateToManyRelations(entity: Entity, transformer: EntityClassTransformer) {
        // add everything in reverse as transformer writes in reverse direction
        entity.toManyRelations.reversed().forEach { toMany ->
            transformer.ensureImport("${toMany.targetEntity.javaPackageDao}.${toMany.targetEntity.classNameDao}")

            transformer.defMethod("reset${toMany.name.capitalize()}") {
                Templates.entity.manyRelationReset(toMany)
            }

            transformer.defMethod("get${toMany.name.capitalize()}") {
                Templates.entity.manyRelationGetter(toMany, entity)
            }
        }
    }

    private fun generateActiveMethodsAndFields(transformer: EntityClassTransformer) {
        // add everything in reverse as transformer writes in reverse direction
        transformer.defMethod("update") {
            Templates.entity.activeUpdate()
        }

        transformer.defMethod("refresh") {
            Templates.entity.activeRefresh()
        }

        transformer.defMethod("delete") {
            Templates.entity.activeDelete()
        }
    }
}