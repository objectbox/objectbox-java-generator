package org.greenrobot.greendao.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.util.PatternFilterable
import org.greenrobot.greendao.codemodifier.Greendao3Generator
import org.greenrobot.greendao.codemodifier.SchemaOptions
import java.io.File
import java.util.*

class Greendao3GradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.logger.debug("greendao plugin starting...")
        project.extensions.create("greendao", GreendaoOptions::class.java, project)

        // Use afterEvaluate so order of applying the plugins in consumer projects does not matter
        project.afterEvaluate {
            val version = getVersion()
            project.logger.debug("greendao plugin ${version} preparing tasks...")
            val candidatesFile = project.file("build/cache/greendao-candidates.list")
            val sourceProvider = project.sourceProvider
            val encoding = sourceProvider.encoding ?: "UTF-8"

            val taskArgs = mapOf("type" to DetectEntityCandidatesTask::class.java)
            val prepareTask = project.task(taskArgs, "greendaoPrepare") as DetectEntityCandidatesTask
            prepareTask.sourceFiles = sourceProvider.sourceTree().matching(Closure { pf: PatternFilterable ->
                pf.include("**/*.java")
            })
            prepareTask.candidatesListFile = candidatesFile
            prepareTask.version = version
            prepareTask.charset = encoding

            val greendaoTask = createGreendaoTask(project, candidatesFile, encoding, version)
            greendaoTask.dependsOn(prepareTask)

            project.tasks.forEach {
                if (it is JavaCompile) {
                    project.logger.debug("Make ${it.name} depend on greendao")
                    addGreenDaoTask(greendaoTask, it)
                }
            }

            project.tasks.whenTaskAdded {
                if (it is JavaCompile) {
                    project.logger.debug("Make just added task ${it.name} depend on greendao")
                    addGreenDaoTask(greendaoTask, it)
                }
            }
        }
    }

    private fun addGreenDaoTask(greendaoTask: Task, javaTask: JavaCompile) {
        javaTask.dependsOn(greendaoTask)
        // ensure generated files are on classpath, just adding a srcDir seems not enough
        javaTask.setSource(greendaoTask.outputs.files + javaTask.source)
    }

    private fun createGreendaoTask(project: Project, candidatesFile: File, encoding: String, version: String): Task {
        val options = project.extensions.getByType(GreendaoOptions::class.java)
        val targetGenDir = options.targetGenDir?: File(project.buildDir, "generated/source/greendao")
        if (options.targetGenDir == null) {
            project.whenSourceProviderAvailable {
                it.addSourceDir(targetGenDir)
            }
        }
        val generateTask = project.task("greendao").apply {
            logging.captureStandardOutput(LogLevel.INFO)

            inputs.file(candidatesFile)
            inputs.property("plugin-version", version)
            inputs.property("source-encoding", encoding)

            val schemaOptions = collectSchemaOptions(options.daoPackage, targetGenDir, options)

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

                Greendao3Generator(
                        options.formatting.data,
                        options.skipTestGeneration,
                        encoding
                ).run(candidatesFiles, schemaOptions)
            }
        }
        return generateTask
    }

    private fun getVersion(): String {
        return Greendao3GradlePlugin::class.java.getResourceAsStream(
                "/org/greenrobot/greendao/gradle/version.properties")?.let {
            val properties = Properties()
            properties.load(it)
            properties.getProperty("version") ?: throw RuntimeException("No version in version.properties")
        } ?: "0"
    }

    private fun collectSchemaOptions(daoPackage: String?, genSrcDir: File, options: GreendaoOptions)
            : MutableMap<String, SchemaOptions> {
        val defaultOptions = SchemaOptions(
                name = "default",
                version = options.schemaVersion,
                daoPackage = daoPackage,
                outputDir = genSrcDir,
                testsOutputDir = if (options.generateTests) options.targetGenDirTests else null
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
                    } else null
            )
        }.associateTo(schemaOptions, { it.name to it })
        return schemaOptions
    }

}