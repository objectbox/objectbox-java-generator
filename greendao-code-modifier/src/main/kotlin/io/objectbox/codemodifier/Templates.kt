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
        private val relationToOneToOneGetter = get("entity/relation_to_one_ToOneGetter.ftl")

        private val relationToManyGetter = get("entity/relation_to_many_getter.ftl")
        private val relationToManyReset = get("entity/relation_to_many_reset.ftl")

        private val fieldGet = get("entity/field_get.ftl")
        private val fieldSet = get("entity/field_set.ftl")

        val activeRemove = get("entity/active_remove.ftl")
        val activePut = get("entity/active_put.ftl")
        val activeRefresh = get("entity/active_refresh.ftl")

        fun constructor(className: String, properties: List<ParsedProperty>, notNullAnnotation: String): String =
                constructor(mapOf(
                        "className" to className,
                        "properties" to properties,
                        "notNullAnnotation" to notNullAnnotation))

        fun oneRelationSetter(one: ToOne, notNullAnnotation: String): String =
                relationToOneSetter(mapOf("toOne" to one, "notNullAnnotation" to notNullAnnotation))

        fun oneRelationGetter(one: ToOne, entity: Entity): String =
                relationToOneGetter(mapOf("entity" to entity, "toOne" to one))

        fun oneRelationToOneGetter(one: ToOne, entity: Entity): String =
                relationToOneToOneGetter(mapOf("entity" to entity, "toOne" to one))

        fun manyRelationGetter(many: ToManyBase, entity: Entity): String =
                relationToManyGetter(mapOf("toMany" to many, "entity" to entity))

        fun manyRelationReset(many: ToManyBase): String = relationToManyReset(mapOf("toMany" to many))

        fun fieldGet(variable: Variable): String = fieldGet(mapOf("variable" to variable))
        fun fieldSet(variable: Variable): String = fieldSet(mapOf("variable" to variable))

        fun activePut(entity: Entity): String = activePut(mapOf("entity" to entity))
        fun activeRemove(entity: Entity): String = activeRemove(mapOf("entity" to entity))
        fun activeRefresh(entity: Entity): String = activeRefresh(mapOf("entity" to entity))
    }
}

operator fun Template.invoke(bindings: Map<String, Any> = emptyMap()): String {
    val writer = StringWriter()
    this.process(bindings, writer)
    return writer.toString()
}