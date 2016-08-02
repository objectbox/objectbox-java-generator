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
                .map { context.parse(it, classesByDir[it.parentFile]!!) }
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

    fun generateSchema(entities: List<EntityClass>, options: SchemaOptions) {
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

    private fun checkClass(entityClass: EntityClass) {
        val fieldsInConstructorOrder = entityClass.getFieldsInConstructorOrder()
        val noConstructor = fieldsInConstructorOrder == null && run {
            val fieldVars = entityClass.fields.map { it.variable }
            entityClass.constructors.none {
                it.hasFullSignature(entityClass.name, fieldVars)
            }
        }
        if (noConstructor) {
            throw RuntimeException(
                    "Can't find constructor for entity ${entityClass.name} with all persistent fields. " +
                            "Note parameter names of such constructor should be equal to field names"
            )
        }
    }

    // TODO refactor into separate class and divide into several methods?
    private fun transformClass(entityClass: EntityClass, mapping: Map<EntityClass, Entity>) {
        val entity = mapping[entityClass]!!
        val daoPackage = entity.schema.defaultJavaPackage

        context.transform(entityClass) {
            ensureImport("org.greenrobot.greendao.annotation.Generated")

            val fieldsInOrder = entityClass.getFieldsInConstructorOrder() ?: entityClass.fields

            if (entityClass.generateConstructors) {
                // check there is need to generate default constructor to do not hide implicit one
                if (fieldsInOrder.isNotEmpty()
                        && entityClass.constructors.none { it.parameters.isEmpty() && !it.generated }) {
                    defConstructor(emptyList()) {
                        """
                    @Generated(hash = $HASH_STUB)
                    public ${entityClass.name}() {
                    }
                    """
                    }
                }

                // generate all fields constructor
                defConstructor(fieldsInOrder.map { it.variable.type.name }) {
                    Templates.entity.constructor(entityClass.name, entityClass.fields,
                            entityClass.notNullAnnotation ?: "@NotNull")
                }
            }

            // define missing getters and setters
            entityClass.fields.forEach { field ->
                // define first set, because the transformer will write then in an opposite direction
                defMethodIfMissing("set${field.variable.name.capitalize()}", field.variable.type.name) {
                    Templates.entity.fieldSet(field.variable)
                }

                defMethodIfMissing("get${field.variable.name.capitalize()}") {
                    Templates.entity.fieldGet(field.variable)
                }
            }

            if (entity.active) {
                ensureImport("org.greenrobot.greendao.DaoException")

                val daoSessionVarName = "${entity.schema.prefix}DaoSession"
                defField("daoSession", VariableType("$daoPackage.$daoSessionVarName", false, daoSessionVarName),
                        "Used to resolve relations")
                defField("myDao", VariableType("${entity.javaPackageDao}.${entity.classNameDao}", false, entity.classNameDao),
                        "Used for active entity operations.")

                defMethod("__setDaoSession", "$daoPackage.$daoSessionVarName") {
                    Templates.entity.daoSessionSetter(entity)
                }

                entity.toOneRelations.forEach { toOne ->
                    ensureImport("${toOne.targetEntity.javaPackageDao}.${toOne.targetEntity.classNameDao}")

                    // define fields
                    if (toOne.isUseFkProperty) {
                        defField("${toOne.name}__resolvedKey",
                                VariableType(toOne.resolvedKeyJavaType[0], false, toOne.resolvedKeyJavaType[0]))
                    } else {
                        defField("${toOne.name}__refreshed", VariableType("boolean", true, "boolean"))
                    }

                    defMethod("get${toOne.name.capitalize()}") {
                        Templates.entity.oneRelationGetter(toOne, entity)
                    }

                    if (!toOne.isUseFkProperty) {
                        defMethod("peak${toOne.name.capitalize()}") {
                            Templates.entity.oneRelationPeek(toOne)
                        }
                    }

                    defMethod("set${toOne.name.capitalize()}", toOne.targetEntity.className) {
                        if (entityClass.notNullAnnotation == null && toOne.fkProperties[0].isNotNull) {
                            ensureImport("org.greenrobot.greendao.annotation.NotNull")
                        }
                        Templates.entity.oneRelationSetter(toOne, entityClass.notNullAnnotation ?: "@NotNull")
                    }
                }

                entity.toManyRelations.forEach { toMany ->
                    ensureImport("${toMany.targetEntity.javaPackageDao}.${toMany.targetEntity.classNameDao}")

                    defMethod("get${toMany.name.capitalize()}") {
                        Templates.entity.manyRelationGetter(toMany, entity)
                    }

                    defMethod("reset${toMany.name.capitalize()}") {
                        Templates.entity.manyRelationReset(toMany)
                    }
                }

                defMethod("delete") {
                    Templates.entity.activeDelete()
                }

                defMethod("update") {
                    Templates.entity.activeUpdate()
                }

                defMethod("refresh") {
                    Templates.entity.activeRefresh()
                }
            }
        }
    }
}