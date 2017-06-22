package io.objectbox.gradle.transform

import io.objectbox.annotation.Entity
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.NotFoundException
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.FieldInfo
import javassist.bytecode.Opcode
import javassist.bytecode.SignatureAttribute
import javassist.bytecode.annotation.Annotation
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File


class ClassTransformer(val debug: Boolean = false) {
    object Const {
        val entityAnnotationName = Entity::class.qualifiedName

        val toOne = "io/objectbox/relation/ToOne"
        val toOneDescriptor = "L$toOne;"

        val toMany = "io/objectbox/relation/ToMany"
        val toManyDescriptor = "L$toMany;"

        val boxStoreFieldName = "__boxStore"
        val boxStoreClass = "io.objectbox.BoxStore"

        val cursorClass = "io.objectbox.Cursor"
        val cursorAttachEntityMethodName = "attachEntity"

        val listClass = "java/util/List"
        val listDescriptor = "L$listClass;"

        val genericSignatureT =
                SignatureAttribute.ClassSignature(arrayOf(SignatureAttribute.TypeParameter("T"))).encode()!!
    }

    var totalCountTransformed = 0
    var totalCountCopied = 0

    fun probeClass(file: File): ProbedClass {
        DataInputStream(BufferedInputStream(file.inputStream())).use {
            val classFile = ClassFile(it)
            val name = classFile.name
            val javaPackage = name.substringBeforeLast('.', "")
            if (!classFile.isAbstract) {
                if (Const.cursorClass == classFile.superclass) {
                    return return ProbedClass(file = file, name = name, javaPackage = javaPackage, isCursor = true)
                } else {
                    var annotation = getEntityAnnotation(classFile)
                    if (annotation != null) {
                        @Suppress("UNCHECKED_CAST")
                        val fields = classFile.fields as List<FieldInfo>
                        return ProbedClass(
                                file = file,
                                name = name,
                                javaPackage = javaPackage,
                                isEntity = true,
                                listFieldTypes = extractAllListTypes(fields),
                                hasBoxStoreField = fields.any { it.name == Const.boxStoreFieldName },
                                hasToOneRef = hasClassRef(classFile, Const.toOne, Const.toOneDescriptor),
                                hasToManyRef = hasClassRef(classFile, Const.toMany, Const.toManyDescriptor)
                        )
                    }
                }
            }
            return return ProbedClass(file = file, name = name, javaPackage = javaPackage)
        }
    }

    private fun extractAllListTypes(fields: List<FieldInfo>): List<String> {
        return fields.filter { it.descriptor == Const.listDescriptor }.map {
            val signatureAttr = it.getAttribute(SignatureAttribute.tag) as SignatureAttribute
            val classSignature = SignatureAttribute.toClassSignature(signatureAttr.signature)
            val typeArguments = classSignature.superClass.typeArguments
            (typeArguments?.singleOrNull()?.type as? SignatureAttribute.ClassType)?.name
        }.filterNotNull()
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
        @Suppress("UNCHECKED_CAST")
        return classFile.constPool.classNames.any { it is String && it == className }
                || (classFile.fields as List<FieldInfo>).any { it.descriptor == classDescriptorName }
    }

    fun transformOrCopyClasses(probedClasses: List<ProbedClass>, outDir: File) {
        val startTime = System.currentTimeMillis()
        val classPool = createClassPool()
        val transformedClasses = mutableSetOf<ProbedClass>()

        transformEntities(probedClasses, outDir, classPool, transformedClasses)
        // Transform Cursors after entities because this depends on entity CtClasses added to the ClassPool
        transformCursors(probedClasses, outDir, classPool, transformedClasses)

        probedClasses.filter { !transformedClasses.contains(it) }.forEach { classToCopy ->
            val targetFile = File(outDir, classToCopy.name.replace('.', '/') + ".class")
            classToCopy.file.copyTo(targetFile)
        }
        val time = System.currentTimeMillis() - startTime
        val transformed = transformedClasses.size
        val copied = probedClasses.size - transformed
        totalCountTransformed += transformed
        totalCountCopied += copied
        System.out.println("Transformed $transformed entities and copied $copied classes in $time ms")
    }

    private fun createClassPool(): ClassPool {
        val classPool = ClassPool(null)
        classPool.makeClass(Const.boxStoreClass)
        val cursorCtClass = classPool.makeClass(Const.cursorClass)
        cursorCtClass.addField(CtField.make("${Const.boxStoreClass} boxStoreForEntities;", cursorCtClass))
        classPool.makeClass(Const.toOne).genericSignature = Const.genericSignatureT
        classPool.makeClass(Const.toMany).genericSignature = Const.genericSignatureT
        return classPool
    }


    private fun transformEntities(probedClasses: List<ProbedClass>, outDir: File, classPool: ClassPool,
                                  transformedClasses: MutableSet<ProbedClass>) {
        probedClasses.filter { it.isEntity }.forEach { entityClass ->
            entityClass.file.inputStream().use {
                if (debug) println("Preparing entity ${entityClass.name}")
                val ctClass = classPool.makeClass(it)
                try {
                    if (entityClass.hasToOneRef || entityClass.hasToManyRef) {
                        if (transformRelationEntity(ctClass, outDir)) {
                            transformedClasses.add(entityClass)
                        }
                    }
                } catch (e: Exception) {
                    throw TransformException("Could not transform entity class: ${ctClass.name}", e)
                }
            }
        }
    }

    private fun transformRelationEntity(ctClass: CtClass, outDir: File): Boolean {
        if (debug) println("Transforming entity with relations: ${ctClass.name}")
        var changed = false
        var boxStoreField = ctClass.declaredFields.find { it.name == Const.boxStoreFieldName }
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

    private fun transformCursors(probedClasses: List<ProbedClass>, outDir: File, classPool: ClassPool,
                                 transformedClasses: MutableSet<ProbedClass>) {
        probedClasses.filter { it.isCursor }.forEach { cursorClass ->
            cursorClass.file.inputStream().use {
                val ctClass = classPool.makeClass(it)
                try {
                    if (transformCursor(ctClass, outDir, classPool)) {
                        transformedClasses.add(cursorClass)
                    }
                } catch (e: Exception) {
                    throw TransformException("Could not transform Cursor class: ${ctClass.name}", e)
                }
            }
        }
    }

    private fun transformCursor(ctClass: CtClass, outDir: File, classPool: ClassPool): Boolean {
        val attachCtMethod = ctClass.declaredMethods?.singleOrNull { it.name == Const.cursorAttachEntityMethodName }
        if (attachCtMethod != null) {
            val signature = attachCtMethod.signature
            if (!signature.startsWith("(L") || !signature.endsWith(";)V") || signature.contains(',')) {
                throw TransformException(
                        "Bad signature for ${ctClass.name}.${Const.cursorAttachEntityMethodName}: $signature")
            }

            val existingCode = attachCtMethod.methodInfo.codeAttribute.code
            if (existingCode.size != 1 || existingCode[0] != Opcode.RETURN.toByte()) {
                throw TransformException(
                        "Expected empty method body for ${ctClass.name}.${Const.cursorAttachEntityMethodName} " +
                                "but was ${existingCode.size} long")
            }

            checkEntityIsInClassPool(classPool, signature)

            val code = "\$1.${Const.boxStoreFieldName} = \$0.boxStoreForEntities;"
            attachCtMethod.setBody(code)
            ctClass.writeFile(outDir.absolutePath)
            return true
        } else return false
    }

    private fun checkEntityIsInClassPool(classPool: ClassPool, signature: String) {
        val entityClass = signature.drop(2).dropLast(3).replace('/', '.')
        var entityCtClass: CtClass? = null
        try {
            entityCtClass = classPool.get(entityClass) // find() seems to do something else!?
        } catch (e: NotFoundException) {
            // entityCtClass just keeps null
        }
        if (entityCtClass == null) {
            System.out.println("Warning: cursor transformer did not find entity class $entityClass")
            entityCtClass = classPool.makeClass(entityClass)
            val fieldCode = "transient ${Const.boxStoreClass} ${Const.boxStoreFieldName};"
            entityCtClass.addField(CtField.make(fieldCode, entityCtClass))
        }
    }


}
