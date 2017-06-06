package io.objectbox.gradle

import io.objectbox.codemodifier.ObjectBoxGenerator
import io.objectbox.codemodifier.SchemaOptions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.util.PatternFilterable
import java.io.File
import java.io.IOException
import java.util.Properties

class ObjectBoxGradlePlugin : Plugin<Project> {

    val name: String = "objectbox"
    val packageName: String = "io/objectbox"

    override fun apply(project: Project) {
        project.logger.debug("$name plugin starting...")
        project.extensions.create(name, ObjectBoxOptions::class.java, project)

        val version = getVersion()
        project.logger.debug("$name plugin $version preparing tasks...")
        val candidatesFile = project.file("build/cache/$name-candidates.list")
        val sourceProvider = getSourceProvider(project)
        val encoding = sourceProvider.encoding ?: "UTF-8"

        val taskArgs = mapOf("type" to DetectEntityCandidatesTask::class.java)
        val prepareTask = project.task(taskArgs, "${name}Prepare") as DetectEntityCandidatesTask
        prepareTask.sourceFiles = sourceProvider.sourceTree().matching(Closure { pf: PatternFilterable ->
            pf.include("**/*.java")
        })
        prepareTask.candidatesListFile = candidatesFile
        prepareTask.version = version
        prepareTask.charset = encoding
        prepareTask.group = name
        prepareTask.description = "Finds entity source files for $name"

        val options = project.extensions.getByType(ObjectBoxOptions::class.java)
        val writeToBuildFolder = options.targetGenDir == null
        val targetGenDir = if (writeToBuildFolder)
            File(project.buildDir, "generated/source/$name") else options.targetGenDir!!

        val objectboxTask = createObjectBoxTask(project, candidatesFile, options, targetGenDir, encoding, version)
        objectboxTask.dependsOn(prepareTask)

        sourceProvider.addGeneratorTask(objectboxTask, targetGenDir, writeToBuildFolder)

        // Cannot use afterEvaluate to register transform, thus out plugin must be applied after Android
        sourceProvider.registerTransform()
    }

    private fun createObjectBoxTask(project: Project, candidatesFile: File, options: ObjectBoxOptions,
                                    targetGenDir: File, encoding: String, version: String): Task {
        val generateTask = project.task(name).apply {
            logging.captureStandardOutput(LogLevel.INFO)

            inputs.file(candidatesFile)
            inputs.property("plugin-version", version)
            inputs.property("source-encoding", encoding)

            val schemaOptions = collectSchemaOptions(project, targetGenDir, options)

            schemaOptions.forEach { e ->
                inputs.property("schema-${e.key}", e.value.toString())
            }

            val outputFileTree = project.fileTree(targetGenDir, Closure { pf: PatternFilterable ->
                pf.include("**/*Dao.java", "**/DaoSession.java", "**/DaoMaster.java")
            })
            outputs.files(outputFileTree)

            if (options.generateTests) {
                outputs.dir(options.targetGenDirTests)
            }

            doLast {
                require(candidatesFile.exists()) {
                    "Candidates file does not exist. Can't continue"
                }

                // read candidates file skipping first for timestamp
                val candidatesFiles = candidatesFile.readLines().asSequence().drop(1).map { File(it) }.asIterable()

                ObjectBoxGenerator(
                        options.formatting.data,
                        options.skipTestGeneration,
                        options.daoCompat,
                        encoding
                ).run(candidatesFiles, schemaOptions)
            }
        }
        generateTask.group = name
        generateTask.description = "Generates source files for $name"
        return generateTask
    }

    private fun getVersion(): String {
        val properties = Properties()
        val stream = javaClass.getResourceAsStream("/$packageName/gradle/version.properties")
        stream?.use {
            try {
                properties.load(it)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return properties.getProperty("version") ?: "Unknown (bad build)"
    }

    private fun collectSchemaOptions(project: Project, genSrcDir: File, options: ObjectBoxOptions)
            : MutableMap<String, SchemaOptions> {
        val idModelDir = project.mkdir("objectbox-models")
        val defaultOptions = SchemaOptions(
                name = "default",
                version = options.schemaVersion,
                daoPackage = options.daoPackage,
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

    val ANDROID_PLUGINS = listOf(
            "android", "android-library", "com.android.application", "com.android.library"
    )

    /** @throws RuntimeException if no supported plugins applied */
    private fun getSourceProvider(project: Project): SourceProvider {
        when {
            project.plugins.hasPlugin("java") -> return JavaPluginSourceProvider(project)

            ANDROID_PLUGINS.any { project.plugins.hasPlugin(it) } -> return AndroidPluginSourceProvider(project)

            else -> throw RuntimeException("ObjectBox supports only Java and Android projects. " +
                    "None of the corresponding plugins have been applied to the project.")
        }
    }


}