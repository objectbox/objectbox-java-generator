package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.dom.*
import java.io.File

/**
 * @param name is fully qualified name (if it was resolved)
 * @param originalName original type name how it was appeared in the source (qualified or not)
 * */
data class VariableType(val name: String, val isPrimitive: Boolean, val originalName: String,
                        val typeArguments: List<VariableType>? = null) {
    val simpleName : String
        get() = name.substringAfterLast('.')
}

data class Variable(val type : VariableType, val name : String)

data class EntityField(val variable : Variable,
                       val id: TableId? = null,
                       val index: PropertyIndex? = null,
                       val isNotNull: Boolean = false,
                       val columnName: String? = null,
                       val customType: CustomType? = null,
                       val unique: Boolean = false)

data class TransientField(val variable: Variable,
                          override val node: FieldDeclaration,
                          override val hint: GeneratorHint?) : Generatable<FieldDeclaration>

data class CustomType(val converterClassName: String,
                      val columnJavaType: VariableType)

data class TableId(val autoincrement: Boolean, val orderDesc: Boolean)

data class PropertyIndex(val name: String?, val unique: Boolean)

data class TableIndex(val name: String?, val fields: List<OrderProperty>, val unique: Boolean)

data class OrderProperty(val name: String, val order: Order)

data class Method(val name: String, val parameters: List<Variable>,
                  override val node: MethodDeclaration,
                  override val hint: GeneratorHint?) : Generatable<MethodDeclaration> {
    fun hasSignature(name: String, paramsTypes: List<String>) : Boolean {
        return this.name == name
            && (this.parameters.map { it.type.name } == paramsTypes
            || this.parameters.map { it.type.simpleName } == paramsTypes)
    }

    fun hasFullSignature(name: String, params: List<Variable>) : Boolean {
        return this.name == name && this.parameters == params
    }
}

data class JoinOnProperty(val source: String, val target: String)

data class JoinEntitySpec(val entityName: String, val sourceIdProperty: String, val targetIdProperty: String)

data class OneRelation(val variable: Variable, val foreignKeyField: String? = null, val columnName: String? = null,
                       val isNotNull: Boolean = false, val unique: Boolean = false)

data class ManyRelation(val variable: Variable, val mappedBy: String? = null,
                        val joinOnProperties: List<JoinOnProperty> = emptyList(),
                        val joinEntitySpec: JoinEntitySpec? = null,
                        val order: List<OrderProperty>? = null)

data class EntityClass(val name: String, val schema: String,
                       val active: Boolean,
                       val fields: List<EntityField>, val transientFields: List<TransientField>,
                       val constructors: List<Method>, val methods: List<Method>,
                       val node: TypeDeclaration,
                       val imports: List<ImportDeclaration>, val packageName: String,
                       val tableName: String?,
                       val oneRelations: List<OneRelation>, val manyRelations: List<ManyRelation>,
                       val indexes: List<TableIndex>,
                       val sourceFile: File, val source: String,
                       val keepSource: Boolean,
                       val createTable: Boolean,
                       val notNullAnnotation: String?) {
    val qualifiedClassName: String
        get() = "$packageName.$name"
}

enum class GeneratorHint {
    KEEP, GENERATED
}

enum class Order { ASC, DESC }

interface Generatable<NodeType : BodyDeclaration> {
    val hint: GeneratorHint?
    val node: NodeType
    val generated: Boolean
        get() = hint == GeneratorHint.GENERATED
    val keep: Boolean
        get() = hint == GeneratorHint.KEEP
}