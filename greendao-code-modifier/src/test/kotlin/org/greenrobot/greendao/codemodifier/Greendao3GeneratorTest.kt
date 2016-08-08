package org.greenrobot.greendao.codemodifier

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.ArrayList

class Greendao3GeneratorTest {

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

    @Test
    fun testAllTestFiles() {
        // WARNING: this test will *abort on the first failure*, so it may hide any other files failing

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

    @Test
    fun testCreateConstructor() {
        generateAndAssertFile("CreateConstructor")
    }

    @Test
    fun testKeepCustomConstructor() {
        generateAndAssertFile("KeepCustomConstructor")
    }

    @Test
    fun testKeepDefaultConstructor() {
        // a default constructor is required by the entity DAO, ensure it is never removed
        generateAndAssertFile("KeepDefaultConstructor")
    }

    @Test
    fun testNoConstructor() {
        generateAndAssertFile("NoConstructor")
    }

    @Test
    fun testRecreateConstructor() {
        // deleting one constructor should properly re-create the missing one instead of replacing the remaining one
        // currently the new constructor is inserted after the current one
        generateAndAssertFile("RecreateConstructor")
    }

    @Test
    fun testReplaceConstructorNewProperties() {
        generateAndAssertFile("ReplaceConstructorNewProperties")
    }

    fun generateAndAssertFile(baseFileName : String) {
        val inputFileName = "${baseFileName}Input.java"
        val actualFileName = "${baseFileName}Actual.java"
        val expectedFileName = "${baseFileName}Expected.java"

        // copy the input file to the test directory
        val inputFile = File(samplesDirectory, inputFileName)
        val targetFile = inputFile.copyTo(File(testDirectory, actualFileName), true)

        // run the generator over the file
        Greendao3Generator(formattingOptions).run(listOf(targetFile), mapOf("default" to schemaOptions))

        // check if the modified file matches the expected output file
        val actualSource = targetFile.readText(charset("UTF-8"))
        val expectedSource = File(samplesDirectory, expectedFileName).readText(charset("UTF-8"))
        assertEquals("${expectedFileName} does not match with ${actualFileName}", expectedSource, actualSource)
    }

}
