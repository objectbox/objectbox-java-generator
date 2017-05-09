package io.obectbox.codemodifier

import io.objectbox.codemodifier.GreendaoModelTranslator
import io.objectbox.codemodifier.ParsedEntity
import io.objectbox.codemodifier.VariableType
import io.objectbox.generator.idsync.IdSync
import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.Schema
import java.io.File
import java.util.*

open class ParseTestBase {

    val BarType = VariableType("com.example.Bar", false, "Bar")
    val BarItemType = VariableType("com.example.Bar.Item", false, "Bar.Item")
    val BarListType = VariableType("java.util.List", false, "List<Bar>", listOf(BarType))

    fun parse(code: String, classesInPackage: List<String> = emptyList()) =
            tryParseEntity(code, classesInPackage)

    /** Tests may opt to trigger additional tests via conversion */
    fun convertToGeneratorModel(parsedEntity: ParsedEntity): Entity {
        val parsedEntities = Collections.singletonList(parsedEntity)
        val jsonFile = File.createTempFile("objectbox-model-test-", ".json")
        try {
            val idSync = IdSync(jsonFile)
            idSync.sync(parsedEntities)

            // take explicitly specified package name, or package name of the first entity
            val packageName = parsedEntity.packageName
            val schema = Schema("default", 1, packageName)
            schema.lastEntityId = idSync.lastEntityId
            schema.lastIndexId = idSync.lastIndexId
            val mapping = GreendaoModelTranslator.convert(parsedEntities, schema, packageName, idSync)

            return mapping[parsedEntity]!!
        } finally {
            jsonFile.delete()
        }
    }

}
