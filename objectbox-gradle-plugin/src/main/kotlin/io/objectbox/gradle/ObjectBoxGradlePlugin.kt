package io.objectbox.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.util.PatternFilterable
import org.greenrobot.greendao.codemodifier.ObjectBoxGenerator
import org.greenrobot.greendao.codemodifier.SchemaOptions
import io.objectbox.gradle.Closure
import io.objectbox.gradle.ObjectBoxOptions
import io.objectbox.gradle.sourceProvider
import io.objectbox.gradle.whenSourceProviderAvailable
import java.io.File
import java.util.*

class ObjectBoxGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.logger.debug("ObjectBox plugin starting...")
        project.extensions.create("objectbox", ObjectBoxOptions::class.java, project)

        // Use afterEvaluate so order of applying the plugins in consumer projects does not matter
        project.afterEvaluate {
            val version = getVersion()
            project.logger.debug("objectbox plugin ${version} preparing tasks...")
            val candidatesFile = project.file("build/cache/objectbox-candidates.list")
            val sourceProvider = project.sourceProvider
            val encoding = sourceProvider.encoding ?: "UTF-8"

            val taskArgs = mapOf("type" to DetectEntityCandidatesTask::class.java)
            val prepareTask = project.task(taskArgs, "objectboxPrepare") as DetectEntityCandidatesTask
            prepareTask.sourceFiles = sourceProvider.sourceTree().matching(Closure { pf: PatternFilterable ->
                pf.include("**/*.java")
            })
            prepareTask.candidatesListFile = candidatesFile
            prepareTask.version = version
            prepareTask.charset = encoding

            val objectboxTask = createObjectBoxTask(project, candidatesFile, encoding, version)
            objectboxTask.dependsOn(prepareTask)

            project.tasks.forEach {
                if (it is JavaCompile) {
                    project.logger.debug("Make ${it.name} depend on objectbox")
                    addObjectBoxTask(objectboxTask, it)
                }
            }

            project.tasks.whenTaskAdded {
                if (it is JavaCompile) {
                    project.logger.debug("Make just added task ${it.name} depend on objectbox")
                    addObjectBoxTask(objectboxTask, it)
                }
            }
        }
    }

    private fun addObjectBoxTask(objectboxTask: Task, javaTask: JavaCompile) {
        javaTask.dependsOn(objectboxTask)
        // ensure generated files are on classpath, just adding a srcDir seems not enough
        javaTask.setSource(objectboxTask.outputs.files + javaTask.source)
    }

    private fun createObjectBoxTask(project: Project, candidatesFile: File, encoding: String, version: String): Task {
        val options = project.extensions.getByType(ObjectBoxOptions::class.java)
        val targetGenDir = options.targetGenDir?: File(project.buildDir, "generated/source/objectbox")
        if (options.targetGenDir == null) {
            project.whenSourceProviderAvailable {
                it.addSourceDir(targetGenDir)
            }
        }
        val generateTask = project.task("objectbox").apply {
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

                ObjectBoxGenerator(
                        options.formatting.data,
                        options.skipTestGeneration,
                        encoding
                ).run(candidatesFiles, schemaOptions)
            }
        }
        return generateTask
    }

    private fun getVersion(): String {
        return ObjectBoxGradlePlugin::class.java.getResourceAsStream(
                "/io/objectbox/gradle/version.properties")?.let {
            val properties = Properties()
            properties.load(it)
            properties.getProperty("version") ?: throw RuntimeException("No version in version.properties")
        } ?: "0"
    }

    private fun collectSchemaOptions(daoPackage: String?, genSrcDir: File, options: ObjectBoxOptions)
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