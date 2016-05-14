package org.greenrobot.greendao.gradle

import groovy.lang.Closure
import groovy.lang.GroovyObjectSupport
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.greenrobot.greendao.codemodifier.FormattingOptions
import org.greenrobot.greendao.codemodifier.Tabulation
import java.io.File

/**
 * Gradle plugin extension, which collects all greenDAO options
 *
 * NOTE class should be opened because gradle inherits from it
 */
open class GreendaoOptions(val project: Project) {
    /**
     * The only required property to set up. Limits scan of entities classes to one of the following rules:
     *
     * - a package name, e.g. "com.example.app.entities"
     * - directory path, e.g. "src/main/java/com/example/app/entities"
     * - gradle file collection, e.g. fileTree(dir: "src/main/java/com/example/app/entities", include: "*Entity.java")
     *
     * In case package name or directory path is specified, the plugin also scans internal packages/directories
     *
     * Note, precision of the rule reflects performance of the build.
     * The difference can be more significant for big projects.
     * For maximum performance ensure your entities package does not contain anything except
     * classes annotated with @Entity, otherwise consider usage of [Project.fileTree]
     */
    var entities: Any? = null
        set(value) {
            field = value
            if (value is String && daoPackage == null && value.matches(Regex("[a-z._]+"))) {
                daoPackage = value
            }
        }

    /**
     * Package name for generated DAO (EntityDao, DaoMaster, DaoSession)
     * The value is optional, by default package name is taken from source entities
     */
    var daoPackage: String? = null

    /**
     * Base directory where generated DAO classes should be put
     * @see testsGenSrcDir
     */
    var genSrcDir: File = project.file("src/generated/main/java")

    /**
     * Base directory where generated unit tests should be put.
     *
     * @see generateTests
     */
    var testsGenSrcDir: File = project.file("src/androidTest/java")

    /**
     * Version of the default schema.
     * This is available then at runtime and useful for updating schema.
     *
     * @see SchemaOptionsExtension.version
     */
    var schemaVersion = 1

    /**
     * Whether the DB should be encrypted. Disabled by default.
     *
     * FIXME: Not yet supported by the plugin
     */
    var encrypt = false

    /**
     * Whether unit tests should be automatically generated on a next build
     */
    var generateTests = false

    /**
     * Whether the output folder should be also scanned for @Entity annotated classes
     * Useful, if [genSrcDir] equals to main source directory
     */
    var scanOutputDir = false

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

    internal val formatting = FormattingExtension()
    internal val schemas = SchemasExtension(project)

    /**
     * Specifies [FileCollection] which contains entities classes
     */
    fun entities(value: FileCollection) {
        this.entities = value
    }

    /**
     * Specifies package name or directory path which contains entities classes
     */
    fun entities(pathOrPackage: String) {
        this.entities = pathOrPackage
    }

    /** @see genSrcDir */
    fun genSrcDir(dir: File) {
        this.genSrcDir = dir
    }

    /** @see genSrcDir */
    fun genSrcDir(path: String) {
        this.genSrcDir = project.file(path)
    }

    /** @see testsGenSrcDir */
    fun testsGenSrcDir(dir: File) {
        this.testsGenSrcDir = dir
    }

    /** @see testsGenSrcDir */
    fun testsGenSrcDir(path: String) {
        this.testsGenSrcDir = project.file(path)
    }

    /** @see schemaVersion */
    fun schemaVersion(version: Int) {
        this.schemaVersion = version
    }

    /** @see daoPackage */
    fun daoPackage(pkg: String) {
        this.daoPackage = pkg
    }

    /**
     * Configures formatting with closure for the code generated inside @Entity annotated classes.
     *
     * Example:
     * ```
     * greendao {
     *   // ...
     *   formatting {
     *      tabulation space:4
     *      lineWidth 120
     *   }
     *   // ...
     * }
     * ```
     *
     * By default greenDAO generator tries to detect preferred tabulation and line width by analyzing Entity's source.
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
     * greendao {
     *    daoPackage "com.example.myapp.defaultdao" // configures default daoPackage
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

    /** @see encrypt */
    fun encrypt(value: Boolean) {
        encrypt = value
    }

    /** @see generateTests */
    fun generateTests(value: Boolean) {
        this.generateTests = value
    }

    /** @see scanOutputDir */
    fun scanOutputDir(value: Boolean) {
        this.scanOutputDir = value
    }

    /** @see skipTestGeneration */
    fun skipTestGeneration(vararg value: String) {
        this.skipTestGeneration.addAll(value)
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
            require(size > 0) { "Size should be greater than 0"}
            data.tabulation = when(key.toLowerCase()) {
                "tab" -> Tabulation('\t', size)
                "space" -> Tabulation(' ', size)
                else -> throw IllegalArgumentException("Unsupported tab char. Use 'space' or 'tab'")
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
    /** @see GreendaoOptions.schemaVersion */
    var version: Int? = null

    /** @see GreendaoOptions.encrypt */
    var encrypt: Boolean? = null

    /** @see GreendaoOptions.daoPackage */
    var daoPackage: String? = null

    /** @see GreendaoOptions.genSrcDir */
    var genSrcDir: File? = null

    /** @see GreendaoOptions.testsGenSrcDir */
    var testsGenSrcDir: File? = null

    /** @see GreendaoOptions.version */
    fun version(value: Int) {
        this.version = value
    }

    /** @see GreendaoOptions.encrypt */
    fun encrypt(value: Boolean) {
        this.encrypt = value
    }

    /** @see GreendaoOptions.daoPackage */
    fun daoPackage(value: String) {
        this.daoPackage = value
    }

    /** @see GreendaoOptions.genSrcDir */
    fun genSrcDir(value: File) {
        this.genSrcDir = value
    }

    /** @see GreendaoOptions.genSrcDir */
    fun genSrcDir(value: String) {
        this.genSrcDir = project.file(value)
    }

    /** @see GreendaoOptions.testsGenSrcDir */
    fun testsGenSrcDir(value: File) {
        this.testsGenSrcDir = value
    }

    /** @see GreendaoOptions.testsGenSrcDir */
    fun testsGenSrcDir(value: String) {
        this.testsGenSrcDir = project.file(value)
    }
}