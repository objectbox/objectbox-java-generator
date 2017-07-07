package io.objectbox.gradle.transform

import javassist.bytecode.ClassFile
import javassist.bytecode.FieldInfo
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File


class ClassProber(val debug: Boolean = false) {

    fun probeClass(file: File): ProbedClass {
        DataInputStream(BufferedInputStream(file.inputStream())).use {
            val classFile = ClassFile(it)
            val name = classFile.name
            val javaPackage = name.substringBeforeLast('.', "")
            if (!classFile.isAbstract) {
                if (ClassConst.cursorClass == classFile.superclass) {
                    return ProbedClass(file = file, name = name, javaPackage = javaPackage, isCursor = true)
                } else {
                    var annotation = classFile.exGetAnnotation(ClassConst.entityAnnotationName)
                    if (annotation != null) {
                        @Suppress("UNCHECKED_CAST")
                        val fields = classFile.fields as List<FieldInfo>
                        return ProbedClass(
                                file = file,
                                name = name,
                                javaPackage = javaPackage,
                                isEntity = true,
                                listFieldTypes = extractAllListTypes(fields),
                                hasBoxStoreField = fields.any { it.name == ClassConst.boxStoreFieldName },
                                hasToOneRef = hasClassRef(classFile, ClassConst.toOne, ClassConst.toOneDescriptor),
                                hasToManyRef = hasClassRef(classFile, ClassConst.toMany, ClassConst.toManyDescriptor)
                        )
                    }
                }
            }
            return ProbedClass(file = file, name = name, javaPackage = javaPackage,
                    isEntityInfo = classFile.interfaces.any { it == ClassConst.entityInfo })
        }
    }

    private fun extractAllListTypes(fields: List<FieldInfo>): List<String> {
        return fields.filter {
            it.descriptor == ClassConst.listDescriptor && !it.exIsTransient()
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
