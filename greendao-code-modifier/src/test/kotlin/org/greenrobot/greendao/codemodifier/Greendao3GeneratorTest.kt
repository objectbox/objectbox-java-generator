package org.greenrobot.greendao.codemodifier

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

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
    fun testCreateConstructor() {
        generateAndAssertFile("CreateConstructor")
    }

    @Test
    fun testKeepCustomConstructor() {
        generateAndAssertFile("KeepCustomConstructor")
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
        // copy the input file to the test directory
        val inputFile = File(samplesDirectory, baseFileName + "Input.java")
        val targetFile = inputFile.copyTo(File(testDirectory, baseFileName + "Actual.java"), true)

        // run the generator over the file
        Greendao3Generator(formattingOptions).run(listOf(targetFile), mapOf("default" to schemaOptions))

        // check if the modified file matches the expected output file
        val actualSource = targetFile.readText(charset("UTF-8"))
        val expectedSource = File(samplesDirectory, baseFileName + "Expected.java").readText(charset("UTF-8"))
        assertEquals(expectedSource, actualSource)
    }

}
