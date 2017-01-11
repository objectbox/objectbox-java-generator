package org.greenrobot.greendao.codemodifier

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

    private val testDirectory: File = File("build/tmp/greendaoTest/")

    private val formattingOptions: FormattingOptions = FormattingOptions().apply {
        this.lineWidth = 120
    }

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
        val testFilePrefix = "LegacyKeepFields"
        generateAndAssertFile(testFilePrefix)
    }

    // NOTE: test may output multiple failed files, make sure to scroll up :)
    @Test
    fun testAllTestFiles() {
        // get a list of all input test files
        val testFiles = ArrayList<String>()
        samplesDirectory.listFiles().filter { it.isFile && it.nameWithoutExtension.endsWith("Input") }.forEach {
            val testName = it.nameWithoutExtension.substringBeforeLast("Input", "")
            if (testName.length > 0) {
                testFiles.add(testName)
            }
        }

        // run the generator on each and check output
        testFiles.forEach {
            ensureEmptyTestDirectory()
            generateAndAssertFile(it)
        }
    }

    // NOTE: test may output multiple failed files, make sure to scroll up :)
    @Test
    @Ignore
    fun testAllTestDirectories() {
        samplesDirectory.listFiles().filter { it.isDirectory && it.name.endsWith("-input") }.forEach {
            ensureEmptyTestDirectory()
            generateAndAssertDirectory(it)
        }
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
            throw RuntimeException("Could not run generator on " + inputFileName, ex);
        }

        checkSameFileContent(actualFile, File(samplesDirectory, expectedFileName))
    }

    private fun checkSameFileContent(actualFile: File, expectedFile: File) {
        val actualSource = actualFile.readText()
        val expectedSource = expectedFile.readText()

        collector.checkSucceeds({
            assertEquals("${expectedFile.name} does not match with ${actualFile.name}", expectedSource, actualSource)
        })
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
            throw RuntimeException("Could not run generator on " + dir, ex);
        }

        dirExpected.listFiles().forEach {
            val actualFile = File(testDirectory, it.name)
            checkSameFileContent(actualFile, it)
        }
    }

}
