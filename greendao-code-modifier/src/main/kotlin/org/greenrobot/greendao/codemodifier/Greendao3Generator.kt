package org.greenrobot.greendao.codemodifier

import de.greenrobot.daogenerator.DaoGenerator
import de.greenrobot.daogenerator.Entity
import de.greenrobot.daogenerator.Schema
import java.io.File

/**
 * Main generator.
 * - runs parsing and transformation of Entity classes.
 * - runs generation of dao classes within {@link de.greenrobot.daogenerator.DaoGenerator}
 * */
class Greendao3Generator(formattingOptions: FormattingOptions? = null,
                         val skipTestGeneration: List<String> = emptyList()) {
    val context = JdtCodeContext(formattingOptions)

    fun run(sourceFiles: Iterable<File>,
            schemaOptions: Map<String, SchemaOptions>) {
        require(schemaOptions.size > 0) { "There should be options for at least one schema"}

        val classesByDir = sourceFiles.map { it.parentFile }.distinct().map {
            it to it.getJavaClassNames()
        }.toMap()

        val start = System.currentTimeMillis();
        val entities = sourceFiles.asSequence()
            .map { context.parse(it, classesByDir[it.parentFile]!!) }
            .filterNotNull()
            .toList()

        val time = System.currentTimeMillis() - start;
        println("Parsed ${entities.size} entities in $time ms among ${sourceFiles.count()} source files: " +
                "${entities.asSequence().map { it.name }.joinToString()}")

        require(entities.isNotEmpty()) { "No entities found among specified files" }

        entities.groupBy { it.schema }.forEach { entry ->
            val (schemaName, schemaEntities) = entry
            val options = schemaOptions[schemaName] ?: throw RuntimeException("Undefined schema $schemaName")

            generateSchema(schemaEntities, options)
        }
    }

    fun generateSchema(entities: List<EntityClass>, options: SchemaOptions) {
        val daoPackage = options.daoPackage
        val outputDir = options.outputDir
        val testsOutputDir = options.testsOutputDir

        val schema = Schema(options.version, daoPackage)
        val mapping = GreendaoModelTranslator.translate(entities, schema)

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
                transformClass(daoPackage, entityClass, mapping)
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
        val noConstructor = entityClass.fieldsInConstructorOrder == null
            && run {
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

    private fun transformClass(daoPackage: String, entityClass: EntityClass, mapping: Map<EntityClass, Entity>) {
        val entity = mapping[entityClass]!!

        context.transform(entityClass) {
            ensureImport("org.greenrobot.greendao.annotation.Generated")

            val fieldsInOrder = entityClass.fieldsInConstructorOrder ?: entityClass.fields

            // check there is need to generate default constructor to do not hide implicit one
            if (fieldsInOrder.isNotEmpty()
                && entityClass.constructors.none { it.parameters.isEmpty() && !it.generated }) {
                defConstructor(emptyList()) {
                    """
                    @Generated
                    public ${entityClass.name}() {
                    }
                    """
                }
            }

            defConstructor(fieldsInOrder.map { it.variable.type.name }) {
                Templates.entity.constructor(entityClass.name, entityClass.fields,
                    entityClass.notNullAnnotation ?: "@NotNull" )
            }

            if (entity.active) {
                ensureImport("de.greenrobot.dao.DaoException")

                defField("daoSession", VariableType("$daoPackage.DaoSession", false, "DaoSession"),
                    "Used to resolve relations")
                defField("myDao", VariableType("$daoPackage.${entity.classNameDao}", false, entity.classNameDao),
                    "Used for active entity operations.")

                defMethod("__setDaoSession", "$daoPackage.DaoSession") {
                    Templates.entity.daoSessionSetter(entity)
                }

                entity.toOneRelations.forEach { toOne ->
                    ensureImport("$daoPackage.${toOne.targetEntity.classNameDao}")

                    // define fields
                    if (toOne.isUseFkProperty) {
                        defField("${toOne.name}__resolvedKey",
                            VariableType(toOne.resolvedKeyJavaType[0], false, toOne.resolvedKeyJavaType[0]))
                    } else {
                        defField("${toOne.name}__refreshed", VariableType("boolean", true, "boolean"))
                    }

                    defMethod("get${toOne.name.capitalize()}") {
                        Templates.entity.oneRelationGetter(toOne)
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
                    ensureImport("${daoPackage}.${toMany.targetEntity.classNameDao}")

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