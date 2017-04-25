package io.obectbox.codemodifier

import io.objectbox.codemodifier.FormattingOptions
import io.objectbox.codemodifier.ObjectBoxGenerator
import io.objectbox.codemodifier.SchemaOptions
import org.junit.Assert.assertEquals
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
        val testFilePrefix = "NoGettersSettersIfVisible"
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

    fun generateAndAssertFile(baseFileName: String) {
        val inputFileName = "${baseFileName}Input.java"
        val actualFileName = "${baseFileName}Actual.java"
        val expectedFileName = "${baseFileName}Expected.java"

        // copy the input file to the test directory
        val inputFile = File(samplesDirectory, inputFileName)
        val inputContent = inputFile.readText()
        if (inputContent.contains("generateGettersSetters = false")) {
            // TODO allow generateGettersSetters again
            println("!!! Disabled $inputFileName")
            return
        }
        val actualFile = inputFile.copyTo(File(testDirectory, actualFileName), true)

        // run the generator over the file
        try {
            ObjectBoxGenerator(formattingOptions).run(listOf(actualFile), mapOf("default" to schemaOptions))
        } catch (ex: RuntimeException) {
            throw RuntimeException("Could not run generator on " + inputFileName, ex)
        }

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
