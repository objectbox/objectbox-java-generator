package org.greenrobot.greendao.codemodifier

import org.greenrobot.greendao.generator.DaoGenerator
import org.greenrobot.greendao.generator.Entity
import org.greenrobot.greendao.generator.Schema
import java.io.File

/**
 * Main generator.
 * - runs parsing and transformation of Entity classes.
 * - runs generation of dao classes within {@link org.greenrobot.greendao.generator.DaoGenerator}
 */
class Greendao3Generator(formattingOptions: FormattingOptions? = null,
                         val skipTestGeneration: List<String> = emptyList(),
                         val encoding: String = "UTF-8") {
    val context = JdtCodeContext(formattingOptions, encoding)

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

        if (entities.isNotEmpty()) {
            entities.groupBy { it.schema }.forEach { entry ->
                val (schemaName, schemaEntities) = entry
                val options = schemaOptions[schemaName] ?: throw RuntimeException("Undefined schema $schemaName")

                generateSchema(schemaEntities, options)
            }
        } else {
            System.err.println("No entities found among specified files")
        }
    }

    fun generateSchema(entities: List<EntityClass>, options: SchemaOptions) {
        // take explicitly specified package name, or package name of the first entity
        val daoPackage = options.daoPackage ?: entities.first().packageName
        val outputDir = options.outputDir
        val testsOutputDir = options.testsOutputDir

        val schema = Schema(options.name, options.version, daoPackage)
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
                    @Generated(hash = $HASH_STUB)
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
                ensureImport("org.greenrobot.greendao.DaoException")

                val daoSessionVarName = "${entity.schema.prefix}DaoSession"
                defField("daoSession", VariableType("$daoPackage.$daoSessionVarName", false, daoSessionVarName),
                    "Used to resolve relations")
                defField("myDao", VariableType("$daoPackage.${entity.classNameDao}", false, entity.classNameDao),
                    "Used for active entity operations.")

                defMethod("__setDaoSession", "$daoPackage.$daoSessionVarName") {
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
        }
    }
}