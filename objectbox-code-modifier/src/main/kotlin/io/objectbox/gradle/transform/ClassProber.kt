/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2025 ObjectBox Ltd.
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

import javassist.bytecode.ClassFile
import javassist.bytecode.FieldInfo
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File


/**
 * Probes a class (byte code) for properties required during transformation, notably determines if it is an entity
 * or cursor class.
 *
 * @see ClassTransformer
 */
class ClassProber {

    /**
     * Probes the class inside a byte code [file] for properties used during transformation.
     *
     * @param outDir See [ProbedClass.outDir]
     */
    fun probeClass(file: File, outDir: File): ProbedClass {
        try {
            DataInputStream(BufferedInputStream(file.inputStream())).use { input ->
                val classFile = ClassFile(input)
                val name = classFile.name
                val javaPackage = name.substringBeforeLast('.', "")

                // Cursor class
                if (!classFile.isAbstract && ClassConst.cursorClass == classFile.superclass) {
                    return ProbedClass(
                        outDir = outDir,
                        file = file,
                        name = name,
                        javaPackage = javaPackage,
                        isCursor = true
                    )
                }

                // @Entity or @BaseEntity class
                val entityAnnotation = classFile.exGetAnnotation(ClassConst.entityAnnotationName)
                val isEntity = !classFile.isAbstract && entityAnnotation != null
                val isBaseEntity = classFile.exGetAnnotation(ClassConst.baseEntityAnnotationName) != null
                if (isEntity || isBaseEntity) {
                    @Suppress("UNCHECKED_CAST") val fields = classFile.fields as List<FieldInfo>
                    return ProbedClass(
                        outDir = outDir,
                        file = file,
                        name = name,
                        superClass = classFile.superclass,
                        javaPackage = javaPackage,
                        isEntity = isEntity,
                        isBaseEntity = !isEntity,
                        listFieldTypes = extractAllListTypes(fields),
                        hasBoxStoreField = fields.any { it.name == ClassConst.boxStoreFieldName },
                        hasToOneRef = hasClassRef(classFile, ClassConst.toOne, ClassConst.toOneDescriptor),
                        hasToManyRef = hasClassRef(classFile, ClassConst.toMany, ClassConst.toManyDescriptor),
                        interfaces = if (isEntity) classFile.interfaces.toList() else listOf()
                    )
                }

                // non-@BaseEntity entity super class, EntityInfo class, any other class
                return ProbedClass(
                    outDir = outDir,
                    file = file,
                    name = name,
                    superClass = classFile.superclass,
                    javaPackage = javaPackage,
                    isEntityInfo = classFile.interfaces.any { it == ClassConst.entityInfo }
                )
            }
        } catch (e: Exception) {
            val msg = "Could not probe class file \"${file.absolutePath}\""
            throw TransformException(msg, e)
        }
    }

    private fun extractAllListTypes(fields: List<FieldInfo>): List<String> {
        return fields.mapNotNull {
            val targetClassType = it.exGetSingleGenericTypeArgumentOrNull()
            if (ClassConst.listDescriptor != it.descriptor
                || targetClassType == null
                || it.exIsTransient()
                || it.exGetAnnotation(ClassConst.transientAnnotationName) != null
                || it.exGetAnnotation(ClassConst.convertAnnotationName) != null
            ) {
                // exclude:
                // - not List,
                // - no target entity,
                // - is transient,
                // - is annotated with @Transient or @Convert
                // Note: except for the focus on List this detection should be in sync
                // with ClassTransformer#findRelationFields.
                null
            } else {
                targetClassType.name
            }
        }
    }

    private fun hasClassRef(classFile: ClassFile, className: String, classDescriptorName: String): Boolean {
        // Fields may be of type List, so also check class names (was OK for Customer test entity at least)
        @Suppress("UNCHECKED_CAST")
        return classFile.constPool.classNames.any { it is String && it == className }
                || (classFile.fields as List<FieldInfo>).any { it.descriptor == classDescriptorName }
    }

}
