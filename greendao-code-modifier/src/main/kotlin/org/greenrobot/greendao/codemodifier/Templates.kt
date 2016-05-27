package org.greenrobot.greendao.codemodifier

import org.greenrobot.greendao.generator.Entity
import org.greenrobot.greendao.generator.ToManyBase
import org.greenrobot.greendao.generator.ToOne
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

    private fun get(path : String) = config.getTemplate(path)

    object entity {
        private val constructor = get("entity/constructor.ftl")
        private val daoSessionSetter = get("entity/dao_session_setter.ftl")
        private val oneRelationSetter = get("entity/one_relation_setter.ftl")
        private val oneRelationGetter = get("entity/one_relation_getter.ftl")
        private val oneRelationPeek = get("entity/one_relation_peek.ftl")
        private val manyRelationGetter = get("entity/many_relation_getter.ftl")
        private val manyRelationReset = get("entity/many_relation_reset.ftl")
        private val fieldGet = get("entity/field_get.ftl")
        private val fieldSet = get("entity/field_set.ftl")

        val activeDelete = get("entity/active_delete.ftl")
        val activeUpdate = get("entity/active_update.ftl")
        val activeRefresh = get("entity/active_refresh.ftl")

        fun constructor(className: String, fields: List<EntityField>, notNullAnnotation: String) : String =
            constructor(mapOf("className" to className, "fields" to fields, "notNullAnnotation" to notNullAnnotation))

        fun daoSessionSetter(entity : Entity) : String =
            daoSessionSetter(mapOf("entity" to entity))

        fun oneRelationSetter(one: ToOne, notNullAnnotation: String) : String =
            oneRelationSetter(mapOf("toOne" to one, "notNullAnnotation" to notNullAnnotation))

        fun oneRelationGetter(one: ToOne, entity: Entity) : String =
            oneRelationGetter(mapOf("entity" to entity, "toOne" to one))

        fun oneRelationPeek(one: ToOne) : String =
            oneRelationPeek(mapOf("toOne" to one))

        fun manyRelationGetter(many: ToManyBase, entity: Entity) : String =
            manyRelationGetter(mapOf("toMany" to many, "entity" to entity))

        fun manyRelationReset(many: ToManyBase) : String =
            manyRelationReset(mapOf("toMany" to many))

        fun fieldGet(variable: Variable) : String =
            fieldGet(mapOf("variable" to variable))

        fun fieldSet(variable: Variable) : String =
            fieldSet(mapOf("variable" to variable))
    }
}

operator fun Template.invoke(bindings : Map<String, Any> = emptyMap()) : String {
    val writer = StringWriter()
    this.process(bindings, writer)
    return writer.toString()
}