package io.objectbox.codemodifier

import org.greenrobot.eclipse.jdt.core.dom.BodyDeclaration
import org.greenrobot.eclipse.jdt.core.dom.FieldDeclaration
import org.greenrobot.eclipse.jdt.core.dom.ImportDeclaration
import org.greenrobot.eclipse.jdt.core.dom.MethodDeclaration
import org.greenrobot.eclipse.jdt.core.dom.TypeDeclaration
import java.io.File

/**
 * @param name fully qualified name (if it was resolved)
 * @param originalName original type name how it appeared in the source (qualified or not)
 */
data class VariableType(
        /** fully qualified name (if it was resolved) */
        val name: String,
        val isPrimitive: Boolean,

        /** original type name how it appeared in the source (qualified or not)*/
        val originalName: String,
        val typeArguments: List<VariableType>? = null
) {
    /** Non-qualified name  */
    val simpleName: String
        get() = name.substringAfterLast('.')
}

data class Variable(val type: VariableType, val name: String)

data class ParsedProperty(
        val variable: Variable,
        val astNode: FieldDeclaration? = null,
        val idParams: EntityIdParams? = null,
        var index: PropertyIndex? = null,
        val isNotNull: Boolean = false,
        val dbName: String? = null,
        var uid: Long? = null,
        val customType: CustomType? = null,
        val unique: Boolean = false,
        val fieldAccessible: Boolean = false
)

data class TransientField(val variable: Variable,
                          override val node: FieldDeclaration,
                          override val hint: GeneratorHint?) : Generatable<FieldDeclaration>

data class CustomType(val converterClassName: String,
                      val columnJavaType: VariableType)

data class EntityIdParams(val autoincrement: Boolean, val assignable: Boolean)

data class PropertyIndex(val name: String?, val unique: Boolean)

data class OrderProperty(val name: String, val order: Order)

data class Method(
        val name: String,
        val parameters: List<Variable>,
        override val node: MethodDeclaration,
        override val hint: GeneratorHint?
) : Generatable<MethodDeclaration> {
    fun hasSignature(name: String, paramsTypes: List<String>): Boolean {
        return this.name == name
                && (this.parameters.map { it.type.name } == paramsTypes
                || this.parameters.map { it.type.simpleName } == paramsTypes)
    }

    fun hasFullSignature(name: String, params: List<Variable>): Boolean {
        return this.name == name && this.parameters == params
    }
}

data class JoinOnProperty(val source: String, val target: String)

data class JoinEntitySpec(val entityName: String, val sourceIdProperty: String, val targetIdProperty: String)

data class ToOneRelation(
        val variable: Variable,
        val targetIdField: String? = null,
        val isNotNull: Boolean = false,
        val unique: Boolean = false,
        val astNode: FieldDeclaration? = null,
        val variableIsToOne: Boolean = false
)

data class ToManyRelation(
        val variable: Variable,
        val mappedBy: String? = null,
        val joinOnProperties: List<JoinOnProperty> = emptyList(),
        val joinEntitySpec: JoinEntitySpec? = null,
        val order: List<OrderProperty>? = null,
        val astNode: FieldDeclaration? = null
)

data class ParsedEntity(
        val name: String,
        val schema: String,
        val active: Boolean,
        val properties: MutableList<ParsedProperty>,
        val transientFields: List<TransientField>,
        val constructors: List<Method>,
        val methods: List<Method>,
        val node: TypeDeclaration,
        val imports: List<ImportDeclaration>,
        val packageName: String,
        val dbName: String?,
        var uid: Long? = null,
        val toOneRelations: List<ToOneRelation>,
        val toManyRelations: List<ToManyRelation>,
        val sourceFile: File,
        val source: String,
        val keepSource: Boolean,
        val createInDb: Boolean,
        val generateConstructors: Boolean,
        val generateGettersSetters: Boolean,
        val protobufClassName: String?,
        val notNullAnnotation: String?,
        val lastFieldDeclaration: FieldDeclaration?,
        /** Added to [properties] without existing yet: need to be generated during transformation */
        val propertiesToGenerate: MutableList<ParsedProperty> = mutableListOf<ParsedProperty>()
) {

    val qualifiedClassName: String
        get() = "$packageName.$name"

    val lastMethodDeclaration: MethodDeclaration?
        get() = methods.lastOrNull()?.node

    val lastConstructorDeclaration: MethodDeclaration?
        get() = constructors.lastOrNull()?.node

}

sealed class GeneratorHint {
    object Keep : GeneratorHint()

    class Generated(val hash: Int) : GeneratorHint() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return (other is Generated) && hash == other.hash
        }

        override fun hashCode(): Int {
            return hash
        }

        override fun toString(): String {
            return "Generated(hash=$hash)"
        }
    }
}

enum class Order { ASC, DESC }

interface Generatable<NodeType : BodyDeclaration> {
    val hint: GeneratorHint?
    val node: NodeType
    val generated: Boolean
        get() = hint is GeneratorHint.Generated
    val keep: Boolean
        get() = hint == GeneratorHint.Keep
}