package io.objectbox.gradle.transform

import javassist.bytecode.ClassFile
import javassist.bytecode.FieldInfo
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File


class ClassProber(val debug: Boolean) {

    fun probeClass(file: File): ProbedClass {
        try {
            DataInputStream(BufferedInputStream(file.inputStream())).use {
                val classFile = ClassFile(it)
                val name = classFile.name
                val javaPackage = name.substringBeforeLast('.', "")
                if (!classFile.isAbstract) {
                    if (ClassConst.cursorClass == classFile.superclass) {
                        return ProbedClass(file = file, name = name, superClass = null, javaPackage = javaPackage,
                                isCursor = true)
                    } else {
                        val annotation = classFile.exGetAnnotation(ClassConst.entityAnnotationName)
                        @Suppress("UNCHECKED_CAST")
                        val fields = classFile.fields as List<FieldInfo>
                        return ProbedClass(
                                file = file,
                                name = name,
                                superClass = classFile.superclass,
                                javaPackage = javaPackage,
                                isEntity = annotation != null,
                                isEntityInfo = classFile.interfaces.any { it == ClassConst.entityInfo },
                                listFieldTypes = extractAllListTypes(fields),
                                hasBoxStoreField = fields.any { it.name == ClassConst.boxStoreFieldName },
                                hasToOneRef = hasClassRef(classFile, ClassConst.toOne, ClassConst.toOneDescriptor),
                                hasToManyRef = hasClassRef(classFile, ClassConst.toMany, ClassConst.toManyDescriptor)
                        )
                    }
                }
                return ProbedClass(file = file, name = name, superClass = classFile.superclass,
                        javaPackage = javaPackage,
                        isEntityInfo = classFile.interfaces.any { it == ClassConst.entityInfo })
            }
        } catch (e: Exception) {
            val msg = "Could not probe class file \"${file.absolutePath}\""
            throw TransformException(msg, e)
        }
    }

    fun inheritSuperClassFlags(probedClasses: List<ProbedClass>) {
        probedClasses.filter { it.isEntity }.forEach { entity ->
            inheritSuperClassFlagsImpl(probedClasses, entity.superClass, entity)
        }
    }

    private fun inheritSuperClassFlagsImpl(probedClasses: List<ProbedClass>, superClassName: String?, probedClass: ProbedClass) {
        if (superClassName == null) {
            return
        }
        probedClasses.find { it.name == superClassName }?.let { superClass ->
            if (debug) println("Entity '${probedClass.name}' inherits from '${superClassName}'")

            inheritSuperClassFlagsImpl(probedClasses, superClass.superClass, probedClass)

            // TODO ut: make those fields immutable again
            probedClass.listFieldTypes = probedClass.listFieldTypes.plus(superClass.listFieldTypes)
            probedClass.hasBoxStoreField = probedClass.hasBoxStoreField || superClass.hasBoxStoreField
            probedClass.hasToOneRef = probedClass.hasToOneRef || superClass.hasToOneRef
            probedClass.hasToManyRef = probedClass.hasToManyRef || superClass.hasToManyRef
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
