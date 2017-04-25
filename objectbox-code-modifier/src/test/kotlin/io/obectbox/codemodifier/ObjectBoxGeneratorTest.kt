package io.obectbox.codemodifier

import io.objectbox.codemodifier.FormattingOptions
import io.objectbox.codemodifier.ObjectBoxGenerator
import io.objectbox.codemodifier.ParsedEntity
import io.objectbox.codemodifier.SchemaOptions
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.idsync.IdSyncModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import java.io.File
import java.util.ArrayList

class ObjectBoxGeneratorTest {

    @get:Rule val collector = ErrorCollector()

    private val samplesDirectory: File = File("test-files/")

    private val testDirectory: File = File("build/tmp/objectboxTest/")

    private val formattingOptions: FormattingOptions = FormattingOptions().apply {
        this.lineWidth = 120
    }

    /** Convenience to overwrite expected files with actual files */
    private val UPDATE_EXPECTED_FILES = false

    private val schemaOptions: SchemaOptions = SchemaOptions(
            name = "default",
            version = 1,
            daoPackage = null,
            outputDir = File(testDirectory, "main/java"),
            testsOutputDir = File(testDirectory, "test/java"),
            idModelFile = File(testDirectory, "test-id-model.json")
    )

    @Before
    fun ensureEmptyTestDirectory() {
        testDirectory.deleteRecursively()
        assert(!testDirectory.isDirectory || testDirectory.list().isEmpty())
    }

    /**
     * Configure and run on demand. Easier for bug hunting.
     */
    @Test
    @Ignore
    fun testSingleTestFile() {
        val testFilePrefix = "AnnotationConstant"
        ensureEmptyTestDirectory()
        generateAndAssertFile(testFilePrefix)
    }

    @Test
    fun testFileInsertUid() {
        ensureEmptyTestDirectory()

        // to always insert the same UID we use a pre-defined model file
        // copy model file over test model file
        File(samplesDirectory, "insert-uid/insert-uid-model.json").copyTo(schemaOptions.idModelFile, true)

        generateAndAssertFile("insert-uid/InsertUid")
    }

    @Test
    fun testFileChangeUid() {
        ensureEmptyTestDirectory()

        // to always insert the same UID we use a pre-defined model file
        // copy model file over test model file
        File(samplesDirectory, "insert-uid/change-uid-model.json").copyTo(schemaOptions.idModelFile, true)

        val baseFileName = "insert-uid/ChangeUid"
        val actualFile = generateFile(baseFileName)

        val parsedEntity = tryParseEntity(actualFile!!.readText())
        assertNotNull("Parsing updated entity failed.", parsedEntity)
        val model = IdSync(schemaOptions.idModelFile).justRead()
        assertNotNull("Reading updated model failed.", model)

        assertProperty(model!!, parsedEntity!!, "generateNew", 5, 1661365307719275952)
        assertProperty(model, parsedEntity, "generateNewWithIndex", 6, 5183165565872484426)
    }

    private fun assertProperty(model: IdSyncModel, parsedEntity: ParsedEntity, name: String, idExpected: Int,
                               uidOriginal: Long) {
        val parsedProperty = parsedEntity.properties.find { it.variable.name == name }
        assertNotNull("Could not find $name in entity.", parsedProperty)

        assertTrue("$name @Uid value should no longer be -1", parsedProperty!!.uid != -1L)
        assertTrue("$name @Uid value should change", parsedProperty.uid != uidOriginal)

        val entity = model.entities.first()
        val property = entity.properties.find { it.name == name }
        assertNotNull("Could not find $name in model.", property)

        assertTrue("$name id should change", property!!.modelId == idExpected)
        assertTrue("$name uid should change", property.uid != uidOriginal)
        assertTrue("$name old uid should be retired", model.retiredPropertyUids!!.contains(uidOriginal))
    }

    // NOTE: test may output multiple failed files, make sure to scroll up :)
    @Test
    fun testAllTestFiles() {
        // get a list of all input test files
        val testFiles = ArrayList<String>()
        samplesDirectory.listFiles().filter { it.isFile && it.nameWithoutExtension.endsWith("Input") }.forEach {
            val testName = it.nameWithoutExtension.substringBeforeLast("Input", "")
            if (testName.isNotEmpty()) {
                testFiles.add(testName)
            }
        }

        val additionalChecks = mapOf(
                "IdAfterProperty" to { checkIdAfterProperty() },
                "IdOnly" to { checkIdOnlyCursor() }
        )
        var additionalChecksInvoked = 0

        // run the generator on each and check output
        testFiles.forEach {
            ensureEmptyTestDirectory()
            generateAndAssertFile(it)

            val additionalCheck = additionalChecks[it]
            if (additionalCheck != null) {
                additionalCheck.invoke()
                additionalChecksInvoked++
            }
        }
        assertEquals("Not all additional checks invoked", additionalChecks.size, additionalChecksInvoked)
    }

    fun checkIdAfterProperty() {
        val propertiesFile = File(testDirectory, "/main/java/io/objectbox/codemodifier/test/Note_.java")
        assertTrue(propertiesFile.exists())
        var content = propertiesFile.readText()
        assertTrue(content, content.contains("Property __ID_PROPERTY = id;"))

        val cursorFile = File(testDirectory, "/main/java/io/objectbox/codemodifier/test/NoteCursor.java")
        assertTrue(cursorFile.exists())
        content = cursorFile.readText()
        assertTrue(content, content.contains("entity.setId(__assignedId);"))
    }

    fun checkIdOnlyCursor() {
        val cursorFile = File(testDirectory, "/main/java/io/objectbox/codemodifier/test/IdOnlyCursor.java")
        assertTrue(cursorFile.exists())
        val content = cursorFile.readText()
        assertTrue(content, content.contains("entity.setId(__assignedId);"))
    }

    // NOTE: test may output multiple failed files, make sure to scroll up :)
    @Test
    fun testAllTestDirectories() {
        val additionalChecks = mapOf("relation-input" to { checkRelations() })
        var additionalChecksInvoked = 0
        samplesDirectory.listFiles().filter { it.isDirectory && it.name.endsWith("-input") }.forEach {
            ensureEmptyTestDirectory()
            generateAndAssertDirectory(it)

            val additionalCheck = additionalChecks[it.name]
            if (additionalCheck != null) {
                additionalCheck.invoke()
                additionalChecksInvoked++
            }
        }
        assertEquals("Not all additional checks invoked", additionalChecks.size, additionalChecksInvoked)
    }

    fun checkRelations() {
        val myObjectBoxFile = File(testDirectory, "/main/java/io/objectbox/codemodifier/test/MyObjectBox.java")
        assertTrue(myObjectBoxFile.exists())
        val content = myObjectBoxFile.readText()
        assertTrue(content, content.contains(".indexId("))
    }

    fun generateFile(baseFileName: String): File? {
        val inputFileName = "${baseFileName}Input.java"
        val actualFileName = "${baseFileName}Actual.java"

        // copy the input file to the test directory
        val inputFile = File(samplesDirectory, inputFileName)
        val inputContent = inputFile.readText()
        if (inputContent.contains("generateGettersSetters = false")) {
            // TODO allow generateGettersSetters again
            println("!!! Disabled $inputFileName")
            return null
        }
        val actualFile = inputFile.copyTo(File(testDirectory, actualFileName), true)

        // run the generator over the file
        try {
            ObjectBoxGenerator(formattingOptions).run(listOf(actualFile), mapOf("default" to schemaOptions))
        } catch (ex: RuntimeException) {
            throw RuntimeException("Could not run generator on " + inputFileName, ex)
        }

        return actualFile
    }

    fun generateAndAssertFile(baseFileName: String) {
        val actualFile = generateFile(baseFileName) ?: return

        val expectedFileName = "${baseFileName}Expected.java"
        checkSameFileContent(actualFile, File(samplesDirectory, expectedFileName))
    }

    private fun checkSameFileContent(actualFile: File, expectedFile: File): Boolean {
        val actualSource = actualFile.readText()
        val expectedSource = expectedFile.readText()

        val sameFileContent = collector.checkSucceeds({
            assertEquals("${expectedFile.name} does not match with ${actualFile.name}", expectedSource, actualSource)
            true
        }) ?: false
        if (!sameFileContent && UPDATE_EXPECTED_FILES) {
            actualFile.copyTo(expectedFile, overwrite = true)
        }

        return sameFileContent
    }

    fun generateAndAssertDirectory(dir: File) {
        val dirExpected = File(dir.absolutePath.substringBeforeLast("-input") + "-expected")
        assertTrue(dirExpected.name, dirExpected.exists())

        dir.copyRecursively(testDirectory)
        val files = testDirectory.listFiles().asList()

        // run the generator over the file
        try {
            ObjectBoxGenerator(formattingOptions).run(files, mapOf("default" to schemaOptions))
        } catch (ex: RuntimeException) {
            throw RuntimeException("Could not run generator on " + dir, ex)
        }

        dirExpected.listFiles().forEach {
            val actualFile = File(testDirectory, it.name)
            checkSameFileContent(actualFile, it)
        }
    }

}
