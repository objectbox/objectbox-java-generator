/*
 * ObjectBox Build Tools
 * Copyright (C) 2022-2024 ObjectBox Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle.transform

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import io.objectbox.BoxStore
import io.objectbox.Cursor
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Transient
import io.objectbox.logging.log
import io.objectbox.logging.logWarning
import io.objectbox.relation.RelationInfo
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode

/**
 * Using the ASM Tree API visits a single entity or cursor class and transforms it.
 * See [transformEntity] and [transformCursor].
 *
 * This is a [ClassNode], a special ASM [ClassVisitor] of the ASM Tree API
 * that already implements all visit methods. Implementers, like this, only override
 * [visitEnd] which is called when all info about the class is collected and is
 * ready to be changed and then passed on to the next visitor.
 */
class ObjectBoxAsmClassVisitor(
    private val apiVersion: Int,
    private val nextClassVisitor: ClassVisitor?,
    private val classContext: ClassContext,
    private val debug: Boolean
) : ClassNode(apiVersion) {

    val stats = ClassTransformerStats()

    private val entityAnnotationDescriptor = Type.getType(Entity::class.java).descriptor
    private val transientAnnotationDescriptor = Type.getType(Transient::class.java).descriptor
    private val convertAnnotationDescriptor = Type.getType(Convert::class.java).descriptor
    private val cursorName = Type.getType(Cursor::class.java).internalName
    private val boxStoreDescriptor = Type.getType(BoxStore::class.java).descriptor
    private val toOneType = Type.getType(ToOne::class.java)
    private val toOneName = toOneType.internalName
    private val toOneDescriptor = toOneType.descriptor
    private val toManyType = Type.getType(ToMany::class.java)
    private val toManyName = toManyType.internalName
    private val toManyDescriptor = toManyType.descriptor
    private val relationInfoType = Type.getType(RelationInfo::class.java)
    private val relationInfoDescriptor = relationInfoType.descriptor
    private val listType = Type.getType(List::class.java)
    private val listName = listType.internalName
    private val listDescriptor = listType.descriptor

    override fun visitEnd() {
        // The whole class has been visited and all fields of the base class are now initialized.
        // Can now make any desired changes to this class.

        // Transform an @Entity class
        val isEntity = invisibleAnnotations?.find { it.desc == entityAnnotationDescriptor } != null
        if (isEntity) {
            transformEntity()
        }
        // Transform a Cursor class
        else if (cursorName == superName) {
            transformCursor()
        }

        // After all changes are made, delegate all visit calls to next class visitor
        // (which is typically one that writes the changes).
        if (nextClassVisitor != null) {
            accept(nextClassVisitor)
        }
    }

    data class RelationField(
        val node: FieldNode,
        val isManyRelation: Boolean
    ) {
        val name: String = node.name
        override fun toString(): String = "'$name' (isManyRelation=$isManyRelation)"
    }

    /**
     * Ensures a BoxStore field exists and relation fields are initialized.
     *
     * Finds fields that are ObjectBox relations. Then, if there are some,
     * transforms the class with [ensureBoxStoreField] and [transformConstructors]
     * to initialize relation fields.
     */
    private fun transformEntity() {
        val relationFields = fields.mapNotNull { field ->
            // Exclude:
            // - is transient,
            // - is annotated with @Transient or @Convert
            // - not ToOne or ToMany,
            // - not List of @Entity class,
            // Note: this detection should be in sync with ClassTransformer#findRelationFields
            if (field.access and Opcodes.ACC_TRANSIENT != 0) {
                return@mapNotNull null
            }
            val hasTransientOrConvertAnnotation = field.invisibleAnnotations
                ?.any { transientAnnotationDescriptor == it.desc || convertAnnotationDescriptor == it.desc }
            if (hasTransientOrConvertAnnotation == true) {
                return@mapNotNull null
            }
            if (field.desc == toOneDescriptor) {
                // ToOne
                stats.toOnesFound++
                RelationField(field, isManyRelation = false)
            } else if (field.desc == toManyDescriptor
                || (field.desc == listDescriptor && field.signature.isListOfEntity())
            ) {
                // ToMany or List of @Entity annotated type
                stats.toManyFound++
                RelationField(field, isManyRelation = true)
            } else {
                null
            }
        }
        val hasRelations = relationFields.isNotEmpty()

        if (hasRelations) {
            ensureBoxStoreField()
            transformConstructors(relationFields)
        }
    }

    private fun String.isListOfEntity(): Boolean {
        var visit = 0
        var isList = false
        var isListOfEntity = false
        SignatureReader(this).accept(
            object : SignatureVisitor(apiVersion) {
                override fun visitClassType(name: String) {
                    super.visitClassType(name)
                    visit++
                    // First visit: outer type is List.
                    if (visit == 1 && name == listName) {
                        isList = true
                    }
                    // Second visit: type of list is an @Entity annotated class.
                    if (visit == 2 && isList) {
                        val classData = classContext.loadClassData(name.replace("/", "."))
                        if (classData != null) {
                            isListOfEntity = classData.classAnnotations.contains(ClassConst.entityAnnotationName)
                        }
                    }
                }
            })
        return isListOfEntity
    }

    /**
     * If there is a BoxStore field, makes sure it's not private. If there is none, adds one.
     */
    private fun ensureBoxStoreField() {
        val boxStoreField = fields.find { it.name == ClassConst.boxStoreFieldName }
        if (boxStoreField != null) {
            // Exists, ensure it is not private.
            // Note: this is currently also guaranteed by the compiler
            // as the related Cursor class accesses the field.
            val isPrivate = boxStoreField.access.and(Opcodes.ACC_PRIVATE) != 0
            if (isPrivate) {
                if (debug) log("$name Remove private access from BoxStore field.")
                boxStoreField.access = boxStoreField.access.xor(Opcodes.ACC_PRIVATE)
                stats.boxStoreFieldsMadeVisible++
            }
        } else {
            // Does not exist, add one.
            if (debug) log("$name Add BoxStore field.")
            fields.add(
                FieldNode(
                    /* access = */ Opcodes.ACC_TRANSIENT,
                    /* name = */ ClassConst.boxStoreFieldName,
                    /* descriptor = */ boxStoreDescriptor,
                    /* signature = */ null,
                    /* value = */ null
                )
            )
            stats.boxStoreFieldsAdded++
        }
    }

    /**
     * Transforms constructors of the visited class that do not call other constructors to add initializers for relation
     * fields. For [relationFields] that are already initialized, prints a warning instead.
     */
    private fun transformConstructors(relationFields: List<RelationField>) {
        val initializedRelationFields = mutableSetOf<String>()
        for (methodNode in methods.filter { it.name == "<init>" }) {
            // Skip constructors that call another (this()) constructor to avoid initializing fields multiple times.
            // This would also overwrite potential changes to relation fields made in the called constructor.
            // Note: calling another constructor might not be the first INVOKESPECIAL op,
            // Kotlin's synthetic constructors (to support default parameters) call "this" last.
            val invokeSpecialThis = methodNode.instructions.find {
                it.opcode == Opcodes.INVOKESPECIAL && (it as MethodInsnNode).owner == name
            }
            if (invokeSpecialThis != null) {
                if (debug) log("$name Skip constructor ${methodNode.desc} calling another constructor.")
                continue
            }

            // Find the first INVOKESPECIAL op: as above skips constructors where INVOKESPECIAL calls another
            // constructor (this() calls), assumes the first INVOKESPECIAL op of this constructor must be a
            // "super()" call which initializes the object.
            val invokeSpecialSuper = methodNode.instructions.find { it.opcode == Opcodes.INVOKESPECIAL }

            stats.constructorsCheckedForTransform++
            val initializedFields = methodNode.instructions.getInitializedFields()
            for (relationField in relationFields) {
                val relationFieldName = relationField.name
                if (initializedFields.contains(relationFieldName)) {
                    initializedRelationFields.add(relationFieldName)
                } else {
                    val isManyRelation = relationField.isManyRelation
                    val relationTypeName = if (isManyRelation) toManyName else toOneName
                    val initializeRelationInstructions = InsnList().apply {
                        add(VarInsnNode(Opcodes.ALOAD, 0))
                        add(TypeInsnNode(Opcodes.NEW, relationTypeName))
                        add(InsnNode(Opcodes.DUP))
                        add(VarInsnNode(Opcodes.ALOAD, 0))
                        add(
                            FieldInsnNode(
                                Opcodes.GETSTATIC,
                                "${name}_",
                                relationFieldName,
                                relationInfoDescriptor
                            )
                        )
                        add(
                            MethodInsnNode(
                                Opcodes.INVOKESPECIAL,
                                relationTypeName,
                                "<init>",
                                "(Ljava/lang/Object;$relationInfoDescriptor)V"
                            )
                        )
                        add(FieldInsnNode(Opcodes.PUTFIELD, name, relationFieldName, relationField.node.desc))
                    }
                    // Insert after the first INVOKESPECIAL op to ensure "this" used above (ALOAD 0) is initialized
                    // and any changes made to relation fields (e.g. add ToOne target) by the existing instructions
                    // is not overwritten.
                    methodNode.instructions.insert(invokeSpecialSuper, initializeRelationInstructions)
                    if (debug) log("$name, constructor ${methodNode.desc}: added initializer for $relationField.")
                    if (isManyRelation) stats.toManyInitializerAdded++ else stats.toOnesInitializerAdded++
                }
            }
        }

        // Only print relation init warning once for each entity class.
        if (initializedRelationFields.isNotEmpty()) {
            val fieldNames = initializedRelationFields.joinToString()
            log("In '$name' relation fields ($fieldNames) are initialized, make sure to read ${TextSnippet.URL_RELATIONS_INIT_MAGIC}")
        }
    }

    private fun InsnList.getInitializedFields(): Set<String> {
        return filter { it.opcode == Opcodes.PUTFIELD }
            .map { it as FieldInsnNode }
            .map { it.name }
            .toSet()
    }

    /**
     * Extracts the entity class name from the Cursor type, finds the attach method,
     * checks its signature is as expected and warns if it does contain existing code.
     * Then transforms the attach method to add code assigning the BoxStore field
     * added by the entity transformer. If the attach method already assigns the field,
     * warns instead.
     */
    private fun transformCursor() {
        val entityName = signature.getCursorEntity()
            ?: throw InvalidUserCodeException("$name Cursor class does not have expected type parameter.")
        val attachMethod = methods.find { it.name == ClassConst.cursorAttachEntityMethodName }
            ?: return
        val descriptor = attachMethod.desc
        if (descriptor != "(L$entityName;)V") {
            throw InvalidUserCodeException(
                "$name The signature of ${ClassConst.cursorAttachEntityMethodName} is not as expected, but was '$descriptor'."
            )
        }

        // Warn if body is not empty.
        val actualInstructions = attachMethod.instructions.filterNot { it is LabelNode || it is LineNumberNode }
        if (actualInstructions.size > 1 || actualInstructions[0].opcode != Opcodes.RETURN) {
            logWarning("${name}.${ClassConst.cursorAttachEntityMethodName} body expected to be empty, might lead to unexpected behavior.")
        }

        // Skip if store field is already put.
        val putsBoxStoreField = actualInstructions.find {
            it is FieldInsnNode
                    && it.opcode == Opcodes.PUTFIELD
                    && it.name == ClassConst.boxStoreFieldName
        } != null
        if (putsBoxStoreField) {
            log(
                "$name.${ClassConst.cursorAttachEntityMethodName} assigns " +
                        "${ClassConst.boxStoreFieldName}, make sure to read ${TextSnippet.URL_RELATIONS_INIT_MAGIC}."
            )
            return
        }

        // Add instructions to put store field.
        val putBoxStoreField = InsnList().apply {
            add(VarInsnNode(Opcodes.ALOAD, 1))
            add(VarInsnNode(Opcodes.ALOAD, 0))
            add(FieldInsnNode(Opcodes.GETFIELD, cursorName, ClassConst.cursorBoxStoreFieldName, boxStoreDescriptor))
            add(FieldInsnNode(Opcodes.PUTFIELD, entityName, ClassConst.boxStoreFieldName, boxStoreDescriptor))
        }
        attachMethod.instructions.insert(putBoxStoreField)
        if (debug) log("$name: set BoxStore field in attach method ${attachMethod.desc}.")
        stats.countTransformed++
    }

    private fun String.getCursorEntity(): String? {
        var entityName: String? = null
        SignatureReader(this).accept(
            object : SignatureVisitor(apiVersion) {
                override fun visitClassType(name: String) {
                    super.visitClassType(name)
                    // First visit: outer type is Cursor.
                    // Second visit: type parameter E of Cursor<E>.
                    if (name != cursorName) {
                        entityName = name
                    }
                }
            })
        return entityName
    }

    interface ObjectBoxAsmClassVisitorParams : InstrumentationParameters {
        @get:Internal
        val debug: Property<Boolean>
    }

    abstract class Factory : AsmClassVisitorFactory<ObjectBoxAsmClassVisitorParams> {
        override fun createClassVisitor(
            classContext: ClassContext,
            nextClassVisitor: ClassVisitor
        ): ClassVisitor {
            return ObjectBoxAsmClassVisitor(
                apiVersion = instrumentationContext.apiVersion.get(),
                nextClassVisitor = nextClassVisitor,
                classContext = classContext,
                debug = parameters.get().debug.get()
            )
        }

        // Must be thread-safe.
        override fun isInstrumentable(classData: ClassData): Boolean {
            // If implementing Cursor
            return classData.superClasses.contains(ClassConst.cursorClass)
                    // If annotated with @Entity
                    || classData.classAnnotations.contains(ClassConst.entityAnnotationName)
        }
    }

}