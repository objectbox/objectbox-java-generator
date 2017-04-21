package io.obectbox.codemodifier

import io.objectbox.codemodifier.FormattingOptions
import io.objectbox.codemodifier.ObjectBoxGenerator
import io.objectbox.codemodifier.SchemaOptions
import java.io.File

object GreendaoGeneration {
    @JvmStatic fun main(args: Array<String>) {
        val files = listOf("notes/Note", "orders/Order", "orders/Customer",
                "orders/Employee", "orders/EmployeeOrder").map {
            File("../greendao-example/src/main/java/com/example/greendao/${it}.java")
        }

        val formattingOptions = FormattingOptions().apply {
            lineWidth = 120
        }

        val schemaOptions = SchemaOptions(
                name = "default",
                version = 1,
                daoPackage = null,
                outputDir = File("../greendao-example/src/generated/main/java"),
                testsOutputDir = File("../greendao-example/src/generated/test/java"),
                idModelFile = File("test-GreendaoGeneration.json")
        )

//        val notesSchemaOptions = schemaOptions.copy(
//            name = "notes",
//            daoPackage = "com.example.greendao.notes",
//            version = 2
//        )

        ObjectBoxGenerator(formattingOptions).run(files, mapOf(
                "default" to schemaOptions
//            "notes" to notesSchemaOptions
        ))
    }
}