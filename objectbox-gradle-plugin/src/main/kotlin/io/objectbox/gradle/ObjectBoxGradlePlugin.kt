package io.objectbox.gradle

import io.objectbox.codemodifier.ObjectBoxGenerator
import io.objectbox.codemodifier.SchemaOptions
import io.objectbox.gradle.ProjectEnv.Const.name
import io.objectbox.gradle.transform.ObjectBoxAndroidTransform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.util.PatternFilterable
import java.io.File

class ObjectBoxGradlePlugin : Plugin<Project> {

    val buildTracker = BuildTracker("LegacyGradlePlugin")

    override fun apply(project: Project) {
        val env = ProjectEnv(project)
        env.logDebug("$name plugin starting...")

        try {
            val version = env.objectBoxVersion
            project.logger.debug("$name plugin $version preparing tasks...")

            addJavaModifierTasks(env)

            if (env.hasAndroidPlugin) {
                // Cannot use afterEvaluate to register transform, thus out plugin must be applied after Android
                ObjectBoxAndroidTransform.Registration.to(project)
            }
        } catch (e: Throwable) {
            buildTracker.trackFatal("Applying plugin failed", e)
            throw e
        }
    }

    private fun addJavaModifierTasks(env: ProjectEnv) {
        val project = env.project
        val sourceProvider = when {
            env.hasJavaPlugin -> JavaPluginSourceProvider(project)
            env.hasAndroidPlugin -> AndroidPluginSourceProvider(project)
            else -> throw RuntimeException("No Java/Android plugin found. Apply ObjectBox plugin AFTER those.")
        }
        val taskArgs = mapOf("type" to DetectEntityCandidatesTask::class.java)
        val prepareTask = project.task(taskArgs, "${name}Prepare") as DetectEntityCandidatesTask
        prepareTask.sourceFiles = sourceProvider.sourceTree().matching(Closure { pf: PatternFilterable ->
            pf.include("**/*.java")
        })
        val candidatesFile = project.file("build/cache/$name-candidates.list")
        val encoding = sourceProvider.encoding ?: "UTF-8"
        prepareTask.candidatesListFile = candidatesFile
        prepareTask.version = env.objectBoxVersion
        prepareTask.charset = encoding
        prepareTask.group = name
        prepareTask.description = "Finds entity source files for $name"

        val writeToBuildFolder = env.options.targetGenDir == null
        val targetGenDir = if (writeToBuildFolder)
            File(project.buildDir, "generated/source/$name") else env.options.targetGenDir!!

        val objectboxTask = createObjectBoxTask(env, candidatesFile, targetGenDir, encoding)
        objectboxTask.dependsOn(prepareTask)

        sourceProvider.addGeneratorTask(objectboxTask, targetGenDir, writeToBuildFolder)
    }

    private fun createObjectBoxTask(env: ProjectEnv, candidatesFile: File, targetGenDir: File, encoding: String): Task {
        val project = env.project
        val generateTask = project.task(name).apply {
            logging.captureStandardOutput(LogLevel.INFO)

            inputs.file(candidatesFile)
            inputs.property("plugin-version", env.objectBoxVersion)
            inputs.property("source-encoding", encoding)

            val schemaOptions = collectSchemaOptions(project, targetGenDir, env.options)

            schemaOptions.forEach { e ->
                inputs.property("schema-${e.key}", e.value.toString())
            }

            val outputFileTree = project.fileTree(targetGenDir, Closure { pf: PatternFilterable ->
                pf.include("**/*Dao.java", "**/DaoSession.java", "**/DaoMaster.java")
            })
            outputs.files(outputFileTree)

            if (env.options.generateTests) {
                outputs.dir(env.options.targetGenDirTests)
            }

            doFirst {
                buildTracker.trackBuild(env)
            }

            doLast {
                require(candidatesFile.exists()) {
                    "Candidates file does not exist. Can't continue"
                }

                // read candidates file skipping first for timestamp
                val candidatesFiles = candidatesFile.readLines().asSequence().drop(1).map { File(it) }.asIterable()

                ObjectBoxGenerator(
                        env.options.formatting.data,
                        env.options.skipTestGeneration,
                        env.options.daoCompat,
                        encoding
                ).run(candidatesFiles, schemaOptions)

            }
        }
        generateTask.group = name
        generateTask.description = "Generates source files for $name"
        return generateTask
    }

    private fun collectSchemaOptions(project: Project, genSrcDir: File, options: ObjectBoxOptions)
            : MutableMap<String, SchemaOptions> {
        val idModelDir = project.mkdir("objectbox-models")
        val defaultOptions = SchemaOptions(
                name = "default",
                version = options.schemaVersion,
                daoPackage = null, // TODO remove completely
                outputDir = genSrcDir,
                testsOutputDir = if (options.generateTests) options.targetGenDirTests else null,
                idModelFile = File(idModelDir, "default.json")
        )

        val schemaOptions = mutableMapOf("default" to defaultOptions)

        options.schemas.schemasMap.map { e ->
            val (name, schemaExt) = e
            SchemaOptions(
                    name = name,
                    version = schemaExt.version ?: defaultOptions.version,
                    daoPackage = schemaExt.daoPackage ?: defaultOptions.daoPackage?.let { "$it.$name" },
                    outputDir = defaultOptions.outputDir,
                    testsOutputDir = if (options.generateTests) {
                        schemaExt.targetGenDirTests ?: defaultOptions.testsOutputDir
                    } else null,
                    idModelFile = File(idModelDir, "$name.json")
            )
        }.associateTo(schemaOptions, { it.name to it })
        return schemaOptions
    }

}