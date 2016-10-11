package org.greenrobot.greendao.codemodifier

import org.junit.Assert.assertEquals
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
            testsOutputDir = File(testDirectory, "test/java")
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

    @Test
    fun testAllTestFiles() {
        // NOTE: test may output multiple failed files, make sure to scroll up :)

        // get a list of all input test files
        val testFiles = ArrayList<String>()
        samplesDirectory.listFiles().filter { it.nameWithoutExtension.endsWith("Input") }.forEach {
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

    fun generateAndAssertFile(baseFileName : String) {
        val inputFileName = "${baseFileName}Input.java"
        val actualFileName = "${baseFileName}Actual.java"
        val expectedFileName = "${baseFileName}Expected.java"

        // copy the input file to the test directory
        val inputFile = File(samplesDirectory, inputFileName)
        if(inputFile.readText().contains("@Convert")) {
            // TODO allow @Convert again
            return;
        }
        val targetFile = inputFile.copyTo(File(testDirectory, actualFileName), true)

        // run the generator over the file
        try {
            ObjectBoxGenerator(formattingOptions).run(listOf(targetFile), mapOf("default" to schemaOptions))
        } catch (ex: RuntimeException) {
            throw RuntimeException("Could not run generator on " + inputFileName, ex);
        }

        // check if the modified file matches the expected output file
        val actualSource = targetFile.readText()
        val expectedSource = File(samplesDirectory, expectedFileName).readText()

        collector.checkSucceeds({
            assertEquals("${expectedFileName} does not match with ${actualFileName}", expectedSource, actualSource)
        })
    }

}