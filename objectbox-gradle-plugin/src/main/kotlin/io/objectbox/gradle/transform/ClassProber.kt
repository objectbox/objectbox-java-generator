package io.objectbox.gradle.transform

import javassist.bytecode.ClassFile
import javassist.bytecode.FieldInfo
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File


class ClassProber {

    fun probeClass(file: File): ProbedClass {
        try {
            DataInputStream(BufferedInputStream(file.inputStream())).use {
                val classFile = ClassFile(it)
                val name = classFile.name
                val javaPackage = name.substringBeforeLast('.', "")

                // Cursor class
                if (!classFile.isAbstract && ClassConst.cursorClass == classFile.superclass) {
                    return ProbedClass(file = file, name = name, javaPackage = javaPackage, isCursor = true)
                }

                // @Entity or @BaseEntity class
                val entityAnnotation = classFile.exGetAnnotation(ClassConst.entityAnnotationName)
                val isEntity = !classFile.isAbstract && entityAnnotation != null
                val isBaseEntity = classFile.exGetAnnotation(ClassConst.baseEntityAnnotationName) != null
                if (isEntity || isBaseEntity) {
                    @Suppress("UNCHECKED_CAST") val fields = classFile.fields as List<FieldInfo>
                    return ProbedClass(
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
        return fields.filter {
            it.descriptor == ClassConst.listDescriptor && !it.exIsTransient()
                    && it.exGetAnnotation(ClassConst.transientAnnotationName) == null
        }.map {
            it.exGetSingleGenericTypeArgumentOrNull()?.name
        }.filterNotNull()
    }

    private fun hasClassRef(classFile: ClassFile, className: String, classDescriptorName: String): Boolean {
        // Fields may be of type List, so also check class names (was OK for Customer test entity at least)
        @Suppress("UNCHECKED_CAST")
        return classFile.constPool.classNames.any { it is String && it == className }
                || (classFile.fields as List<FieldInfo>).any { it.descriptor == classDescriptorName }
    }

}
