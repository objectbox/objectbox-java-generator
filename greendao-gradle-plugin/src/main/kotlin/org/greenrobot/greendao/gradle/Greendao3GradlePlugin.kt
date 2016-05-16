package org.greenrobot.greendao.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.util.PatternFilterable
import org.greenrobot.greendao.codemodifier.Greendao3Generator
import org.greenrobot.greendao.codemodifier.SchemaOptions
import java.io.File
import java.util.*

class Greendao3GradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("greendao", GreendaoOptions::class.java, project)

        project.afterEvaluate {
            val options = project.extensions.getByType(GreendaoOptions::class.java)
            val genSrcDir = options.genSrcDir
            val version = getVersion()
            val schemaOptions = collectSchemaOptions(options.daoPackage, genSrcDir, options)
            val candidatesFile = project.file("build/cache/greendao-candidates.list")
            val sourceProvider = project.sourceProvider

            val prepareTask = project.task(
                mapOf("type" to DetectCandidatesTask::class.java), "greendaoPrepare") as DetectCandidatesTask
            prepareTask.sourceFiles = sourceProvider.sourceTree().matching(Closure { pf: PatternFilterable ->
                pf.include("**/*.java")
            })
            prepareTask.candidatesListFile = candidatesFile
            prepareTask.version = version

            // define task
            val generateTask = project.task("greendaoGenerate").apply {
                logging.captureStandardOutput(LogLevel.INFO)

                inputs.file(candidatesFile)

                inputs.property("plugin-version", version)

                // put schema options into inputs
                schemaOptions.forEach { e ->
                    inputs.property("schema-${e.key}", e.value.toString())
                }

                val outputFileTree = project.fileTree(genSrcDir, Closure { pf: PatternFilterable ->
                    pf.include("**/*Dao.java", "**/DaoSession.java", "**/DaoMaster.java")
                })
                outputs.files(outputFileTree)

                if (options.generateTests) {
                    outputs.dir(options.testsGenSrcDir)
                }

                doLast {
                    require(candidatesFile.exists()) {
                        "Candidates file does not exist. Can't continue"
                    }

                    // read candidates file skipping first for timestamp
                    val candidatesFiles = candidatesFile.readLines().asSequence().drop(1).map { File(it) }.asIterable()

                    Greendao3Generator(options.formatting.data, options.skipTestGeneration)
                        .run(candidatesFiles, schemaOptions)
                }
            }

            generateTask.dependsOn(prepareTask)

            project.tasks.filter { it.name.startsWith("compile") }.forEach {
                it.dependsOn(generateTask)
            }
        }
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
            encrypt = options.encrypt,
            daoPackage = daoPackage,
            outputDir = genSrcDir,
            testsOutputDir = if (options.generateTests) options.testsGenSrcDir else null
        )

        val schemaOptions = mutableMapOf("default" to defaultOptions)

        options.schemas.schemasMap.map { e ->
            val (name, schemaExt) = e
            SchemaOptions(
                name = name,
                version = schemaExt.version ?: defaultOptions.version,
                encrypt = schemaExt.encrypt ?: defaultOptions.encrypt,
                daoPackage = schemaExt.daoPackage ?: defaultOptions.daoPackage?.let { "$it.$name" },
                outputDir = schemaExt.genSrcDir ?: defaultOptions.outputDir,
                testsOutputDir = if (options.generateTests) {
                    schemaExt.testsGenSrcDir ?: defaultOptions.testsOutputDir
                } else null
            )
        }.associateTo(schemaOptions, { it.name to it })
        return schemaOptions
    }

}