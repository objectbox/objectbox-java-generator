package io.objectbox.codemodifier

import io.objectbox.generator.model.Entity
import io.objectbox.generator.model.ToManyBase
import io.objectbox.generator.model.ToOne
import freemarker.template.Configuration
import freemarker.template.Template
import java.io.StringWriter

/**
 * Collection and central access point of freemarker templates
 * Goals:
 *  - provide static access to templates inside resources folder
 *  - provide statically-typed parameters for templates
 * */
object Templates {
    private val config = Configuration(Configuration.VERSION_2_3_23)

    init {
        config.setClassForTemplateLoading(this.javaClass, '/' + this.javaClass.`package`.name.replace('.', '/'));
    }

    private fun get(path: String) = config.getTemplate(path)

    object entity {
        private val constructor = get("entity/constructor.ftl")
        private val relationToOneSetter = get("entity/relation_to_one_setter.ftl")
        private val relationToOneGetter = get("entity/relation_to_one_getter.ftl")

        private val fieldGet = get("entity/field_get.ftl")
        private val fieldSet = get("entity/field_set.ftl")

        fun constructor(className: String, properties: List<ParsedProperty>, notNullAnnotation: String): String =
                constructor(mapOf(
                        "className" to className,
                        "properties" to properties,
                        "notNullAnnotation" to notNullAnnotation))

        fun oneRelationSetter(one: ToOne, notNullAnnotation: String): String =
                relationToOneSetter(mapOf("toOne" to one, "notNullAnnotation" to notNullAnnotation))

        fun oneRelationGetter(one: ToOne, entity: Entity): String =
                relationToOneGetter(mapOf("entity" to entity, "toOne" to one))

        fun fieldGet(variable: Variable): String = fieldGet(mapOf("variable" to variable))
        fun fieldSet(variable: Variable): String = fieldSet(mapOf("variable" to variable))
    }
}

operator fun Template.invoke(bindings: Map<String, Any> = emptyMap()): String {
    val writer = StringWriter()
    this.process(bindings, writer)
    return writer.toString()
}