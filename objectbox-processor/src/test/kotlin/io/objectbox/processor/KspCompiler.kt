/*
 * ObjectBox Build Tools
 * Copyright (C) 2026 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.objectbox.processor

import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSNode
import io.objectbox.processor.ksp.ObjectBoxKspProcessor
import java.io.File
import java.nio.file.Files

/**
 * Runs the KSP processor on Java and Kotlin sources using the standalone KSP implementation
 * (no Kotlin compiler invocation or Gradle build required).
 */
class KspCompiler {

    /**
     * Created by the last [compile] call.
     */
    var processor: ObjectBoxKspProcessor? = null

    /**
     * Runs the KSP processor on the given sources with the given processor options and returns
     * the exit code, collected messages and the directory containing generated Java sources.
     */
    fun compile(sourceFiles: List<SourceFile>, options: Map<String, String>): KspCompilation {
        val workingDir = Files.createTempDirectory("objectbox-ksp-test").toFile()
        val sourcesDir = File(workingDir, "sources").apply { mkdirs() }
        sourceFiles.forEach { it.writeTo(sourcesDir) }

        val outputDir = File(workingDir, "output")
        val javaOutputDirectory = File(outputDir, "java")

        val provider = SymbolProcessorProvider { environment: SymbolProcessorEnvironment ->
            ObjectBoxKspProcessor(environment).also { processor = it }
        }

        val kspConfig = KSPJvmConfig.Builder().apply {
            moduleName = "test"
            projectBaseDir = workingDir
            sourceRoots = listOf(sourcesDir)
            javaSourceRoots = listOf(sourcesDir)
            outputBaseDir = outputDir
            cachesDir = File(workingDir, "caches")
            classOutputDir = File(outputDir, "classes")
            kotlinOutputDir = File(outputDir, "kotlin")
            javaOutputDir = javaOutputDirectory
            resourceOutputDir = File(outputDir, "resources")
            // Sources reference the ObjectBox annotations and library classes, which are on
            // the classpath of this test JVM, so just pass along everything.
            libraries = System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .map { File(it) }
                .filter { it.exists() }
            jdkHome = File(System.getProperty("java.home"))
            jvmTarget = "1.8"
            languageVersion = "2.0"
            apiVersion = "2.0"
            processorOptions = options
        }.build()

        val logger = CollectingKspLogger()
        val exitCode = KotlinSymbolProcessing(kspConfig, listOf(provider), logger).execute()

        return KspCompilation(exitCode, logger, javaOutputDirectory)

    }

    /**
     * A Java or Kotlin source file (detected by [fileName] extension) to be processed.
     */
    class SourceFile(private val fileName: String, private val source: String) {

        fun writeTo(sourcesDir: File) {
            // Place the file in a directory structure matching its package (like on disk in a
            // regular project; the Java source model warns or errors on mismatch).
            val packagePath = PACKAGE_PATTERN.find(source)
                ?.groupValues?.get(1)?.replace('.', '/') ?: ""
            val packageDir = File(sourcesDir, packagePath).apply { mkdirs() }
            File(packageDir, fileName).writeText(source)
        }

        companion object {
            private val PACKAGE_PATTERN = Regex("""package\s+([\w.]+)""")

            fun java(fullyQualifiedName: String, source: String): SourceFile {
                return SourceFile(fullyQualifiedName.substringAfterLast('.') + ".java", source)
            }

            fun kotlin(fileName: String, source: String): SourceFile {
                require(fileName.endsWith(".kt")) { "Kotlin file name must end with .kt" }
                return SourceFile(fileName, source)
            }

            /**
             * Reads a Java source file from test resources, like `SimpleEntity` for `SimpleEntity.java`.
             */
            fun javaResource(resourceName: String): SourceFile {
                val resourcePath = "$resourceName.java"
                val source = KspCompiler::class.java.classLoader.getResource(resourcePath)?.readText()
                    ?: throw IllegalArgumentException("Test resource not found: $resourcePath")
                return SourceFile(resourcePath.substringAfterLast('/'), source)
            }
        }
    }

    /**
     * The result of running the KSP processor: exit code, all logged messages and the directory
     * with generated Java sources.
     */
    class KspCompilation(
        private val exitCode: KotlinSymbolProcessing.ExitCode,
        private val logger: CollectingKspLogger,
        private val javaOutputDirectory: File
    ) {

        val messages: List<String>
            get() = logger.messages()

        val succeeded: Boolean
            get() = exitCode == KotlinSymbolProcessing.ExitCode.OK

        val errors: List<String>
            get() = logger.errors

        val warnings: List<String>
            get() = logger.warnings

        /**
         * Returns the generated source file for the given class, or null if it was not generated.
         */
        fun generatedSourceFileOrNull(qualifiedName: String): File? {
            val file = File(javaOutputDirectory, qualifiedName.replace('.', '/') + ".java")
            return if (file.exists()) file else null
        }
    }

    /**
     * Collects messages, prefixed with their severity like "error: " or "warning: ".
     */
    class CollectingKspLogger : KSPLogger {

        private val messages = mutableListOf<String>()

        fun messages(): List<String> = messages

        val errors: List<String>
            get() = messages.filter { it.startsWith("$ERROR: ") }

        val warnings: List<String>
            get() = messages.filter { it.startsWith("$WARNING: ") }

        private fun add(severity: String, message: String) {
            messages.add("$severity: $message")
        }

        override fun logging(message: String, symbol: KSNode?) = add(LOGGING, message)
        override fun info(message: String, symbol: KSNode?) = add(INFO, message)
        override fun warn(message: String, symbol: KSNode?) = add(WARNING, message)
        override fun error(message: String, symbol: KSNode?) = add(ERROR, message)
        override fun exception(e: Throwable) = add(ERROR, e.toString())

        companion object {
            const val LOGGING = "logging"
            const val INFO = "info"
            const val WARNING = "warning"
            const val ERROR = "error"
        }
    }

}
