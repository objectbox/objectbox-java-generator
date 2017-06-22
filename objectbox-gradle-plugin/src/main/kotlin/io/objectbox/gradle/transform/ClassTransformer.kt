package io.objectbox.gradle.transform

import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.NotFoundException
import javassist.bytecode.Opcode
import java.io.File


class ClassTransformer(val debug: Boolean = false) {

    var totalCountTransformed = 0
    var totalCountCopied = 0

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
        classPool.makeClass(ClassConst.boxStoreClass)
        val cursorCtClass = classPool.makeClass(ClassConst.cursorClass)
        cursorCtClass.addField(CtField.make("${ClassConst.boxStoreClass} boxStoreForEntities;", cursorCtClass))
        classPool.makeClass(ClassConst.toOne).genericSignature = ClassConst.genericSignatureT
        classPool.makeClass(ClassConst.toMany).genericSignature = ClassConst.genericSignatureT
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
        var boxStoreField = ctClass.declaredFields.find { it.name == ClassConst.boxStoreFieldName }
        if (boxStoreField == null) {
            boxStoreField = CtField.make("transient ${ClassConst.boxStoreClass} ${ClassConst.boxStoreFieldName};", ctClass)
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
        val attachCtMethod = ctClass.declaredMethods?.singleOrNull { it.name == ClassConst.cursorAttachEntityMethodName }
        if (attachCtMethod != null) {
            val signature = attachCtMethod.signature
            if (!signature.startsWith("(L") || !signature.endsWith(";)V") || signature.contains(',')) {
                throw TransformException(
                        "Bad signature for ${ctClass.name}.${ClassConst.cursorAttachEntityMethodName}: $signature")
            }

            val existingCode = attachCtMethod.methodInfo.codeAttribute.code
            if (existingCode.size != 1 || existingCode[0] != Opcode.RETURN.toByte()) {
                throw TransformException(
                        "Expected empty method body for ${ctClass.name}.${ClassConst.cursorAttachEntityMethodName} " +
                                "but was ${existingCode.size} long")
            }

            checkEntityIsInClassPool(classPool, signature)

            val code = "\$1.${ClassConst.boxStoreFieldName} = \$0.boxStoreForEntities;"
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
            val fieldCode = "transient ${ClassConst.boxStoreClass} ${ClassConst.boxStoreFieldName};"
            entityCtClass.addField(CtField.make(fieldCode, entityCtClass))
        }
    }


}
