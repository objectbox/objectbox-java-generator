package org.greenrobot.greendao.codemodifier

import java.io.File

object GreendaoGeneration {
    @JvmStatic fun main(args: Array<String>) {
        val files = listOf("Note", "Order", "Customer", "Employee", "EmployeeOrder").map {
            File("greendao-example/src/main/java/com/example/greendao/${it}.java")
        }

        val formattingOptions = FormattingOptions().apply {
            lineWidth = 120
        }

        val schemaOptions = SchemaOptions(
            name = "default",
            version = 1,
            encrypt = false,
            daoPackage = "com.example.greendao",
            outputDir = File("greendao-example/src/generated/main/java"),
            testsOutputDir = File("greendao-example/src/generated/test/java")
        )

        val notesSchemaOptions = schemaOptions.copy(
            name = "notes",
            daoPackage = "com.example.greendao.notes",
            version = 2
        )

        Greendao3Generator(formattingOptions).run(files, mapOf(
            "default" to schemaOptions,
            "notes" to notesSchemaOptions
        ))
    }
}
