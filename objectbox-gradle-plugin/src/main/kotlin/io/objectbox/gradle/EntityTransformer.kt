package io.objectbox.gradle

import io.objectbox.annotation.Entity
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.FieldInfo
import javassist.bytecode.annotation.Annotation
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File


class EntityTransformer() {
    object Const {
        val entityAnnotationName = Entity::class.qualifiedName

        val toOne = "io/objectbox/relation/ToOne"
        val toOneDescriptor = "L$toOne;"

        val toMany = "io/objectbox/relation/ToMany"
        val toManyDescriptor = "L$toMany;"

        val boxStoreFieldName = "__boxStore"
        val boxStoreDescriptor = "Lio.objectbox.BoxStore;"
    }

    fun probeClassAsEntity(file: File): ProbedEntity? {
        DataInputStream(BufferedInputStream(file.inputStream())).use {
            val classFile = ClassFile(it)
            if (!classFile.isAbstract) {
                var annotation = getEntityAnnotation(classFile)
                if (annotation != null) {
                    val fields = classFile.fields as List<FieldInfo>
                    return ProbedEntity(
                            file = file,
                            name = classFile.name,
                            hasBoxStoreField = fields.any { it.name == Const.boxStoreFieldName },
                            hasToOne = hasClassRef(classFile, Const.toOne, Const.toOneDescriptor),
                            hasToMany = hasClassRef(classFile, Const.toMany, Const.toManyDescriptor)
                    )
                }
            }
        }
        return null
    }

    private fun getEntityAnnotation(classFile: ClassFile): Annotation? {
        var annotationsAttribute = classFile.getAttribute(AnnotationsAttribute.visibleTag) as AnnotationsAttribute?
        var annotation = annotationsAttribute?.getAnnotation(Const.entityAnnotationName)
        if (annotation == null) {
            annotationsAttribute = classFile.getAttribute(AnnotationsAttribute.invisibleTag) as AnnotationsAttribute?
            annotation = annotationsAttribute?.getAnnotation(Const.entityAnnotationName)
        }
        return annotation
    }

    private fun hasClassRef(classFile: ClassFile, className: String, classDescriptorName: String): Boolean {
        // Fields may be of type List, so also check class names (was OK for Customer test entity at least)
        return classFile.constPool.classNames.any { it is String && it == className }
                || (classFile.fields as List<FieldInfo>).any { it.descriptor == classDescriptorName }
    }
}
