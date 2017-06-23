package io.objectbox.gradle.transform

import io.objectbox.BoxStoreBuilder
import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor
import javassist.CtField
import javassist.Modifier
import javassist.NotFoundException
import javassist.bytecode.Opcode
import javassist.bytecode.SignatureAttribute
import java.io.File

class ClassTransformer(val debug: Boolean = false) {

    internal class Context(val probedClasses: List<ProbedClass>, val outDir: File) {
        val classPool = ClassPool(null)
        val transformedClasses = mutableSetOf<ProbedClass>()
        val entityTypes: Set<String> = probedClasses.filter { it.isEntity }.map { it.name }.toHashSet()
        val stats = ClassTransformerStats()

        init {
            // Notes:
            // 1) class pool does not use system path (problem: would infer with test Fakes)
            // 2) class pool cannot find ObjectBox classes on system path when run as Gradle plugin (OK in iJ)
            val objectBoxPath = BoxStoreBuilder::class.java.protectionDomain.codeSource.location.path
            classPool.appendClassPath(objectBoxPath)
            classPool.makeClass("java.lang.Object")
        }

        fun wasTransformed(probedClass: ProbedClass) = transformedClasses.contains(probedClass)
    }

    fun transformOrCopyClasses(probedClasses: List<ProbedClass>, outDir: File): ClassTransformerStats {
        val context = Context(probedClasses, outDir)

        transformEntities(context)
        // Transform Cursors after entities because this depends on entity CtClasses added to the ClassPool
        transformCursors(context)

        probedClasses.filter { !context.wasTransformed(it) }.forEach { (file, name) ->
            val targetFile = File(outDir, name.replace('.', '/') + ".class")
            file.copyTo(targetFile, overwrite = true)
        }

        context.stats.countTransformed = context.transformedClasses.size
        context.stats.countCopied = probedClasses.size - context.transformedClasses.size
        context.stats.done()

        return context.stats
    }

    private fun transformEntities(context: Context) {
        context.probedClasses.filter { it.isEntity }.forEach { entityClass ->
            entityClass.file.inputStream().use {
                val ctClass = context.classPool.makeClass(it)
                try {
                    if (transformEntity(context, ctClass, entityClass)) {
                        context.transformedClasses.add(entityClass)
                    }
                } catch (e: Exception) {
                    throw TransformException("Could not transform entity class \"${ctClass.name}\" (${e.message})", e)
                }
            }
        }
    }

    private fun transformEntity(context: Context, ctClass: CtClass, entityClass: ProbedClass): Boolean {
        val hasRelations = entityClass.hasRelation(context.entityTypes)
        if (debug) println("Checking to transform entity \"${ctClass.name}\" (has relations: $hasRelations)")
        var changed = false
        var boxStoreField = ctClass.declaredFields.find { it.name == ClassConst.boxStoreFieldName }
        if (boxStoreField != null && Modifier.isPrivate(boxStoreField.modifiers)) {
            boxStoreField.modifiers = boxStoreField.modifiers.xor(Modifier.PRIVATE)
            context.stats.boxStoreFieldsMadeVisible++
            changed = true
        } else if (boxStoreField == null && hasRelations) {
            boxStoreField = CtField.make("transient ${ClassConst.boxStoreClass} ${ClassConst.boxStoreFieldName};", ctClass)
            ctClass.addField(boxStoreField)
            context.stats.boxStoreFieldsAdded++
            changed = true
        }
        if (hasRelations) {
            val toOneFields = mutableListOf<Pair<CtField, SignatureAttribute.ClassType>>()
            ctClass.declaredFields.filter { it.fieldInfo.descriptor == ClassConst.toOneDescriptor }.forEach { toOneField ->
                val targetClassType = toOneField.fieldInfo.exGetSingleGenericTypeArgumentOrNull()
                        ?: throw TransformException("Cannot transform non-generic ToOne field:" +
                        "${ctClass.name}.${toOneField.name} (please add generic type parameter)")
                toOneFields += Pair(toOneField, targetClassType)
                context.stats.toOnesFound++
            }
            for (constructor in ctClass.constructors) {
                val initializedFields = getInitializedFields(ctClass, constructor)
                for ((toOneCtField, targetClassType) in toOneFields) {
                    if (!initializedFields.contains(toOneCtField.name)) {
                        val code = "\$0.${toOneCtField.name} = " +
                                "new ${ClassConst.toOne}((java.lang.Object) \$0, (${ClassConst.relationInfo}) null);"
                        constructor.insertAfter(code)
                        context.stats.toOnesInitialized++
                        changed = true
                    }
                }
            }
        }
        if (changed) {
            if (debug) println("Writing transformed entity \"${ctClass.name}\"")
            ctClass.writeFile(context.outDir.absolutePath)
        }
        return changed
    }

    private fun getInitializedFields(ctClass: CtClass, constructor: CtConstructor): HashSet<String> {
        val initializedFields = hashSetOf<String>()
        val codeIterator = constructor.methodInfo.codeAttribute.iterator()
        codeIterator.begin()
        while (codeIterator.hasNext()) {
            val opIndex = codeIterator.next()
            val op = codeIterator.byteAt(opIndex)
            if (op == Opcode.PUTFIELD) {
                val fieldIndex = codeIterator.u16bitAt(opIndex + 1)
                val constPool = ctClass.classFile.constPool
                val fieldName = constPool.getFieldrefName(fieldIndex)
                if (fieldName != null) {
                    initializedFields += fieldName
                }
            }
        }
        return initializedFields
    }

    private fun transformCursors(context: Context) {
        context.probedClasses.filter { it.isCursor }.forEach { cursorClass ->
            cursorClass.file.inputStream().use {
                val ctClass = context.classPool.makeClass(it)
                try {
                    if (transformCursor(ctClass, context.outDir, context.classPool)) {
                        context.transformedClasses.add(cursorClass)
                    }
                } catch (e: Exception) {
                    throw TransformException("Could not transform Cursor class \"${ctClass.name}\" (${e.message})", e)
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
