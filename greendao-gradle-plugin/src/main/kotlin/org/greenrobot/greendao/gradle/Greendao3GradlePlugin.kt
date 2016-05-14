package org.greenrobot.greendao.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
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

            val entities = options.entities
                ?: throw IllegalArgumentException("greenDAO: 'entities' property should be specified")

            val genSrcDir = options.genSrcDir

            val sourceProvider = project.sourceProvider

            val inputFiles = getInputFiles(
                project,
                entities,
                if (options.scanOutputDir) null else genSrcDir,
                sourceProvider
            )

            val (daoPackage, daoPackageDir) = getDaoPackageAndPackageDir(inputFiles, options, sourceProvider)

            val schemaOptions = collectSchemaOptions(daoPackage, genSrcDir, options)

            // define task
            val generateCode = project.task("generateGreenDao").apply {
                logging.captureStandardOutput(LogLevel.INFO)

                inputs.files(*inputFiles.toTypedArray())

                val version = getVersion()

                inputs.property("plugin-version", version)

                // put schema options into inputs
                schemaOptions.forEach { e ->
                    inputs.property("schema-${e.key}", e.value.toString())
                }

                // define task outputs
                outputs.dir(File(genSrcDir, daoPackageDir))
                if (options.generateTests) {
                    outputs.dir(options.testsGenSrcDir)
                }

                doLast {
                    Greendao3Generator(options.formatting.data, options.skipTestGeneration)
                        .run(inputFiles, schemaOptions)
                }
            }

            project.tasks.filter { it.name.startsWith("compile") }.forEach {
                it.dependsOn(generateCode)
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

    private fun collectSchemaOptions(daoPackage: String, genSrcDir: File, options: GreendaoOptions)
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
                daoPackage = schemaExt.daoPackage ?: "${defaultOptions.daoPackage}.$name",
                outputDir = schemaExt.genSrcDir ?: defaultOptions.outputDir,
                testsOutputDir = if (options.generateTests) {
                    schemaExt.testsGenSrcDir ?: defaultOptions.testsOutputDir
                } else null
            )
        }.associateTo(schemaOptions, { it.name to it })
        return schemaOptions
    }

    /**
     * Tries to resolve a package name for generated dao classes and corresponding relative path to that package
     * E.g. ("com.example.app", "com/example/app")
     */
    private fun getDaoPackageAndPackageDir(inputFiles: List<File>, options: GreendaoOptions,
                                           sourceProvider: SourceProvider): Pair<String, String> {
        return options.daoPackage?.let {
            Pair(it, it.replace('.', '/'))
        } ?: run {
            // detect package name from the path to the first input file
            val firstFile = inputFiles.first()
            val pkgDir = sourceProvider.sourceDirs()
                .find { firstFile.startsWith(it) }
                ?.let { firstFile.parentFile.relativeToOrNull(it)?.path }
                ?: throw RuntimeException("greenDAO: Can't determine package name for output dao classes. " +
                                                    "Please specify it explicitly with daoPackage option.")
            Pair(pkgDir.replace(File.separatorChar, '.'), pkgDir)
        }
    }

    /**
     * @return input java files
     * @throws IllegalArgumentException if no input files found
     * */
    private fun getInputFiles(project: Project, spec: Any, excludeDir: File?,
                              fileTreeProvider: SourceProvider) : List<File> {
        val files = when (spec) {
            is FileCollection -> spec.asFileTree.matching(Closure { pf: PatternFilterable ->
                pf.include("**/*.java")
            }).files

            is String -> spec.let {
                when {
                    // package name
                    it.matches(Regex("[a-z_.]+")) -> fileTreeProvider.sourceFiles().map { tree ->
                        tree.matching(Closure { pf: PatternFilterable ->
                            val path = it.replace('.', '/')
                            pf.include("$path/**/*.java")
                        }).files
                    }.flatten().toList()

                    // folder
                    else -> project.fileTree(it).apply { include("**/*.java") }.files
                }
            }
            else -> throw RuntimeException("greenDAO: entities should be package name, path or file collection")
        }.let {
            if (excludeDir != null) {
                it.filterNot { it.startsWith(excludeDir) }
            } else {
                it.toList()
            }
        }
        require(files.isNotEmpty()) {
            "greenDAO: No java files found among specified file collection. " +
                "Please check 'greendao.entities' property"
        }
        return files
    }
}