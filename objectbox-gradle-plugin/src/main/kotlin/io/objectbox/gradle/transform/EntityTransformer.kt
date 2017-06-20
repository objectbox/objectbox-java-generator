package io.objectbox.gradle.transform

import io.objectbox.annotation.Entity
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.FieldInfo
import javassist.bytecode.SignatureAttribute
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
        val boxStoreClass = "io.objectbox.BoxStore"
        val boxStoreDescriptor = "Lio.objectbox.BoxStore;"

        val genericSignatureT =
                SignatureAttribute.ClassSignature(arrayOf(SignatureAttribute.TypeParameter("T"))).encode()!!
    }

    fun probeClass(file: File): ProbedClass {
        DataInputStream(BufferedInputStream(file.inputStream())).use {
            val classFile = ClassFile(it)
            val name = classFile.name
            val javaPackage = name.substringBeforeLast('.', "")
            if (!classFile.isAbstract) {
                var annotation = getEntityAnnotation(classFile)
                if (annotation != null) {
                    val fields = classFile.fields as List<FieldInfo>
                    return ProbedClass(
                            file = file,
                            name = name,
                            javaPackage = javaPackage,
                            isEntity = true,
                            hasBoxStoreField = fields.any { it.name == Const.boxStoreFieldName },
                            hasToOne = hasClassRef(classFile, Const.toOne, Const.toOneDescriptor),
                            hasToMany = hasClassRef(classFile, Const.toMany, Const.toManyDescriptor)
                    )
                }
            }
            return return ProbedClass(file = file, name = name, javaPackage = javaPackage)
        }
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

    fun transformOrCopyClasses(probedClasses: List<ProbedClass>, outDir: File) {
        val startTime = System.currentTimeMillis()
        var countTransformed=0
        var countCopied=0
        val classPool = ClassPool(null)
        classPool.makeClass(Const.boxStoreClass)
        classPool.makeClass(Const.toOne).genericSignature = Const.genericSignatureT
        classPool.makeClass(Const.toMany).genericSignature = Const.genericSignatureT
        for (probedClass in probedClasses) {
            var transformed = false
            if (probedClass.hasToOne || probedClass.hasToMany) {
                probedClass.file.inputStream().use {
                    val ctClass = classPool.makeClass(it)
                    transformed = transformRelationEntity(ctClass, outDir)
                }
            }
            if (transformed) {
                countTransformed++;
            } else {
                val targetFile = File(outDir, probedClass.name.replace('.', '/'))
                probedClass.file.copyTo(targetFile)
                countCopied++;
            }
        }
        val time = System.currentTimeMillis() - startTime
        System.out.println("Transformed $countTransformed entities and copied $countCopied classes in $time ms")
    }

    private fun transformRelationEntity(ctClass: CtClass, outDir: File): Boolean {
        var changed = false
        var boxStoreField = ctClass.declaredFields.find { it.name == "__boxStore" }
        if (boxStoreField == null) {
            boxStoreField = CtField.make("transient ${Const.boxStoreClass} ${Const.boxStoreFieldName};", ctClass)
            ctClass.addField(boxStoreField)
            changed = true
        }
//        ctClass.declaredFields.filter { it.type.name == Const.toOne }.forEach { toOneField ->
//            val x=  toOneField.genericSignature
//        }
        if (changed) {
            ctClass.writeFile(outDir.absolutePath)
        }
        return changed
    }

}
