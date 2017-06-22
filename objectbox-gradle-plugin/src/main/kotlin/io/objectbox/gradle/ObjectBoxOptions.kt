package io.objectbox.gradle

import groovy.lang.Closure
import groovy.lang.GroovyObjectSupport
import org.gradle.api.Project
import io.objectbox.codemodifier.FormattingOptions
import io.objectbox.codemodifier.Tabulation
import java.io.File

/**
 * Gradle plugin extension, which collects all ObjectBox options
 *
 * NOTE class should be opened because gradle inherits from it
 */
open class ObjectBoxOptions(val project: Project) {

    /**
     * Base directory where generated DAO classes should be put (default: ${buildDir}/generated/source/objectbox).
     * Note that this must be configured to be a src directory. When the default (no-value) is used, the dir is added
     * as a src folder automatically.
     *
     * @see targetGenDirTests
     */
    var targetGenDir: File? = null

    /**
     * Base directory where generated unit tests should be put.
     *
     * @see generateTests
     */
    var targetGenDirTests: File = project.file("src/androidTest/java")

    /**
     * Version of the default database schema, you can use to update your schema.
     *
     * @see SchemaOptionsExtension.version
     */
    var schemaVersion = 1

    /**
     * Whether unit tests should be automatically generated
     */
    var generateTests = false

    /**
     * List of entities class names, for which test generation should be skipped
     *
     * Acceptable values:
     *
     * - simple class name  (e.g. "Order")
     * - fully-qualified class name (e.g. "com.myapp.db.Order")
     * - partly-qualified class name (e.g. "db.Order")
     *
     * @see generateTests
     */
    var skipTestGeneration = mutableListOf<String>()

    var daoCompat = false

    var allowApt = false

    // TODO use it
    var debugTransform = false

    internal val formatting = FormattingExtension()
    internal val schemas = SchemasExtension(project)

    /** @see targetGenDir */
    fun targetGenDir(dir: File) {
        this.targetGenDir = dir
    }

    /** @see targetGenDir */
    fun targetGenDir(path: String) {
        this.targetGenDir = project.file(path)
    }

    /** @see targetGenDirTests */
    fun targetGenDirTests(dir: File) {
        this.targetGenDirTests = dir
    }

    /** @see targetGenDirTests */
    fun targetGenDirTests(path: String) {
        this.targetGenDirTests = project.file(path)
    }

    /** @see schemaVersion */
    fun schemaVersion(version: Int) {
        this.schemaVersion = version
    }

    /**
     * Configures formatting with closure for the code generated inside @Entity annotated classes.
     *
     * Example:
     * ```
     * objectbox {
     *   // ...
     *   formatting {
     *      tabulation space:4
     *      lineWidth 120
     *   }
     *   // ...
     * }
     * ```
     *
     * By default objectbox generator tries to detect preferred tabulation and line width by analyzing Entity's source.
     * However if you are not satisfied with the results, you can enforce the desired formatting.
     * If any formatting option is not specified, then automatic detection is performed
     *
     * @see FormattingExtension for available properties
     */
    fun formatting(closure: Closure<*>) {
        closure.delegate = formatting
        closure.call()
    }

    /**
     * Configures additional schemas with closure
     *
     * Usually this is not required, but if there is need to manage separate schemas, you have to configure them.
     *
     * 1. Specify schema name for each entity with @Entity(schema=...)
     * 2. Describe schemas like this:
     * ```
     * objectbox {
     *    schemaVersion 124
     *    genSrcDir "src/greendao-gen-src/java"
     *
     *    schemas {
     *        mySchema1 {
     *          version = 2 // custom version
     *          daoPackage "com.example.myapp.myschema" // custom daoPackage
     *          // inherits genSrcdir from default schema ("src/greendao-gen-src/java", see above)
     *        }
     *
     *        anotherSchema // with no aditional options, inherits daoPackage, version and genSrcDir from default schema
     *    }
     * }
     * ```
     *
     * Each schema manages its own set of properties.
     * Default values are inherited from default schema, which is configurable directly inside `greendao { ... }`
     *
     * @see SchemaOptionsExtension for available properties
     */
    fun schemas(closure: Closure<*>) {
        closure.delegate = schemas
        closure.call()
    }

    /** @see generateTests */
    fun generateTests(value: Boolean) {
        this.generateTests = value
    }

    /** @see skipTestGeneration */
    fun skipTestGeneration(vararg value: String) {
        this.skipTestGeneration.addAll(value)
    }

    /**
     * If enabled, will generate DAO classes for compatibility with greenDAO.
     */
    fun daoCompat(value: Boolean) {
        this.daoCompat = value
    }

}

class FormattingExtension {
    internal var data = FormattingOptions(null, null)

    /**
     * Specifies tabulation for the code generated inside @Entity annotated classes, e.g.:
     * ```
     * tabulation space:4
     * tabulation space:2
     * tabulation tab:1
     * ```
     *
     * If not specified, tabulation is detected automatically by greenDAO source generator
     */
    fun tabulation(spec: Map<String, Any>) {
        if (spec.size > 0) {
            val key = spec.entries.first().key
            val size = spec[key] as Int
            require(size > 0) { "ObjectBox formatting: tabulation size should be greater than 0" }
            data.tabulation = when (key.toLowerCase()) {
                "tab" -> Tabulation('\t', size)
                "space" -> Tabulation(' ', size)
                else -> throw IllegalArgumentException("ObjectBox formatting: Unsupported tab char. Use 'space' or 'tab'")
            }
        }
    }

    /**
     * Specifies line width for the code generated inside @Entity annotated classes, e.g.:
     * ```
     * lineWidth 80
     * ```
     *
     * If not specified, greenDAO source generator takes the next ten after the length of the longest line
     */
    fun lineWidth(width: Int) {
        require(width > 0) { "Width should be greater than 0" }
        data.lineWidth = width
    }
}

/**
 * Expandable class to define schemas
 * Allows to define schema names dynamically, like so:
 * schemas {
 *      schema1
 *      schema2 {
 *          // ... options ...
 *      }
 * }
 */
class SchemasExtension(val project: Project) : GroovyObjectSupport() {
    val schemasMap = mutableMapOf<String, SchemaOptionsExtension>()

    /** groovy's callback if property is missing */
    fun propertyMissing(name: String): Any? {
        return schemasMap.getOrPut(name, { SchemaOptionsExtension(project) })
    }

    /** groovy's callback is method is missing */
    fun methodMissing(name: String, args: Any?): Any? {
        val schema = schemasMap.getOrPut(name, { SchemaOptionsExtension(project) })
        if (args is Array<*>) {
            (args[0] as? Closure<*>)?.let { closure ->
                closure.delegate = schema
                closure.resolveStrategy = Closure.DELEGATE_ONLY
                closure.call()
                Unit
            } ?: throw IllegalArgumentException("Schema definition expected")
        }
        return schema
    }
}

/**
 * Collects per schema properties
 */
class SchemaOptionsExtension(val project: Project) {
    /** @see ObjectBoxOptions.schemaVersion */
    var version: Int? = null

    /** @see ObjectBoxOptions.daoPackage */
    var daoPackage: String? = null

    /** @see ObjectBoxOptions.targetGenDirTests */
    var targetGenDirTests: File? = null

    /** @see ObjectBoxOptions.schemaVersion */
    fun version(value: Int) {
        this.version = value
    }

    /** @see ObjectBoxOptions.daoPackage */
    fun daoPackage(value: String) {
        this.daoPackage = value
    }

    /** @see ObjectBoxOptions.targetGenDirTests */
    fun targetGenDirTests(value: File) {
        this.targetGenDirTests = value
    }

    /** @see ObjectBoxOptions.targetGenDirTests */
    fun targetGenDirTests(value: String) {
        this.targetGenDirTests = project.file(value)
    }
}