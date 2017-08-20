package io.objectbox.gradle.transform

import io.objectbox.BoxStoreBuilder
import io.objectbox.build.BasicBuildTracker
import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor
import javassist.CtField
import javassist.Modifier
import javassist.NotFoundException
import javassist.bytecode.Descriptor
import javassist.bytecode.Opcode
import javassist.bytecode.SignatureAttribute
import java.io.File

class ClassTransformer(val debug: Boolean = false) {

    // Use internal once fixed (Kotlin 1.1.4?)
    class Context(val probedClasses: List<ProbedClass>, val outDir: File) {
        val classPool = ClassPool()
        val transformedClasses = mutableSetOf<ProbedClass>()
        val entityTypes: Set<String> = probedClasses.filter { it.isEntity }.map { it.name }.toHashSet()
        val stats = ClassTransformerStats()

        init {
            // Notes:
            // 1) class pool should not use any ClassClassPath (problem: would infer with test Fakes)
            // 2) class pool cannot find ObjectBox classes on system path when run as Gradle plugin (OK in iJ)
            // 3) ObjectBox is separated for mainly for tests (we make this configurable, treat tests as special)
            // 4) Don't fake java.lang.Object, it may cause stack overflows because superclass != null
            val objectBoxPath = BoxStoreBuilder::class.java.protectionDomain.codeSource.location.path
            classPool.appendClassPath(objectBoxPath)
            classPool.appendClassPath(PrefixedClassPath("java.", java.lang.Object::class.java))
        }

        fun wasTransformed(probedClass: ProbedClass) = transformedClasses.contains(probedClass)
    }

    private class RelationField(val ctField: CtField,
                                val relationName: String,
                                val relationType: String,
                                val targetTypeSignature: SignatureAttribute.ClassType?
    )

    fun transformOrCopyClasses(probedClasses: List<ProbedClass>, outDir: File): ClassTransformerStats {
        val context = Context(probedClasses, outDir)

        probedClasses.forEach { if (it.isEntityInfo) makeCtClass(context, it) }

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
            val ctClass = makeCtClass(context, entityClass)
            try {
                if (transformEntity(context, ctClass, entityClass)) {
                    context.transformedClasses.add(entityClass)
                }
            } catch (e: Exception) {
                throw TransformException("Could not transform entity class \"${ctClass.name}\" (${e.message})", e)
            }
        }
    }

    private fun makeCtClass(context: Context, entityClass: ProbedClass): CtClass {
        entityClass.file.inputStream().use {
            return context.classPool.makeClass(it)
        }
    }

    private fun transformEntity(context: Context, ctClass: CtClass, entityClass: ProbedClass): Boolean {
        val hasRelations = entityClass.hasRelation(context.entityTypes)
        if (debug) println("Checking to transform entity \"${ctClass.name}\" (has relations: $hasRelations)")
        var changed = checkBoxStoreField(ctClass, context, hasRelations)
        if (hasRelations) {
            val toOneFields = findRelationFields(context, ctClass, ClassConst.toOneDescriptor, ClassConst.toOne)
            context.stats.toOnesFound += toOneFields.size
            val toManyFields = findRelationFields(context, ctClass, ClassConst.toManyDescriptor, ClassConst.toMany)
            val listToEntityFields = findRelationFields(context, ctClass, ClassConst.listDescriptor, ClassConst.toMany)
            toManyFields += listToEntityFields
            context.stats.toManyFound += toManyFields.size
            if (transformConstructors(context, ctClass, toOneFields + toManyFields)) changed = true
        }
        if (changed) {
            if (debug) println("Writing transformed entity \"${ctClass.name}\"")
            ctClass.writeFile(context.outDir.absolutePath)
        }
        return changed
    }

    private fun checkBoxStoreField(ctClass: CtClass, context: Context, hasRelations: Boolean): Boolean {
        var changed = false
        var boxStoreField = ctClass.declaredFields.find { it.name == ClassConst.boxStoreFieldName }
        if (boxStoreField != null && Modifier.isPrivate(boxStoreField.modifiers)) {
            boxStoreField.modifiers = boxStoreField.modifiers.xor(Modifier.PRIVATE)
            context.stats.boxStoreFieldsMadeVisible++
            changed = true
        } else if (boxStoreField == null && hasRelations) {
            val code = "transient ${ClassConst.boxStoreClass} ${ClassConst.boxStoreFieldName};"
            boxStoreField = CtField.make(code, ctClass)
            ctClass.addField(boxStoreField)
            context.stats.boxStoreFieldsAdded++
            changed = true
        }
        return changed
    }

    private fun findRelationFields(context: Context, ctClass: CtClass, fieldTypeDescriptor: String,
                                   relationType: String)
            : MutableList<RelationField> {
        val fields = mutableListOf<RelationField>()
        ctClass.declaredFields.filter { it.fieldInfo.descriptor == fieldTypeDescriptor }.forEach { field ->
            val targetClassType = field.fieldInfo.exGetSingleGenericTypeArgumentOrNull()
            if (ClassConst.listDescriptor == fieldTypeDescriptor) {
                if (targetClassType == null || !context.entityTypes.contains(targetClassType.name)
                        || Modifier.isTransient(field.modifiers)
                        || field.fieldInfo.exGetAnnotation(ClassConst.transientAnnotationName) != null) {
                    return@forEach
                }
            }
            val name = findRelationNameInEntityInfo(context, ctClass, field, relationType)
            fields += RelationField(field, name, relationType, targetClassType)
        }
        return fields
    }

    private fun findRelationNameInEntityInfo(context: Context, ctClass: CtClass, field: CtField, relationType: String)
            : String {
        val entityInfoClassName = ctClass.name + '_'
        val entityInfoCtClass = try {
            context.classPool.get(entityInfoClassName)
        } catch (e: NotFoundException) {
            throw TransformException("Could not find generated class \"$entityInfoClassName\", " +
                    "please ensure that ObjectBox class generation runs properly before")
        }
        var name = field.name
        if (!entityInfoCtClass.fields.any { it.name == name }) {
            val suffix = when (relationType) {
                ClassConst.toOne -> "ToOne"
                ClassConst.toMany -> "ToMany"
                else -> throw TransformException("Unexpected $relationType")
            }
            if (name.endsWith(suffix)) {
                val name2 = name.dropLast(suffix.length)
                if (!entityInfoCtClass.fields.any { it.name == name2 }) {
                    throw TransformException("Could not find RelationInfo element for relation field " +
                            "\"${ctClass.name}.$name\" in generated class \"$entityInfoClassName\"")
                }
                name = name2
            }
        }
        return name
    }

    private fun transformConstructors(context: Context, ctClass: CtClass, relationFields: List<RelationField>)
            : Boolean {
        var changed = false
        for (constructor in ctClass.constructors) {
            checkMakeParamCtClasses(context, constructor)
            context.stats.constructorsCheckedForTransform++
            val initializedFields = getInitializedFields(ctClass, constructor)
            for (field in relationFields) {
                val fieldName = field.ctField.name
                if (!initializedFields.contains(fieldName)) {
                    val code = "\$0.$fieldName = new ${field.relationType}" +
                            "(\$0, ${ctClass.name}_#${field.relationName});"
                    try {
                        constructor.insertBeforeBody(code)
                    } catch (e: Exception) {
                        throw TransformException("Could not insert init code for field $fieldName in constructor", e)
                    }
                    if (field.relationType == ClassConst.toOne) context.stats.toOnesInitializerAdded++
                    else if (field.relationType == ClassConst.toMany) context.stats.toManyInitializerAdded++
                    changed = true
                }
            }
        }
        return changed
    }

    private fun checkMakeParamCtClasses(context: Context, constructor: CtConstructor) {
        // Plan A
        try {
            val count = Descriptor.numOfParameters(constructor.signature)
            // Start at 1, because 0 is '('
            var charIndex = 1
            for (i in 0 until count) {
                val paramPair = getParamType(constructor.signature, charIndex)

                if (paramPair.first != null) {
                    if (context.classPool.getOrNull(paramPair.first) == null) {
                        context.classPool.makeClass(paramPair.first)
                    }
                }

                check(charIndex != paramPair.second)
                charIndex = paramPair.second
            }
        } catch (e: Exception) {
            BasicBuildTracker("Transformer").trackError("Could not define class for params: ${constructor.signature}")
        }

        // Plan B in case previous code failed to define all missing types (plan B could be remove if plan A is stable)
        try {
            var lastExMsg = ""
            while (true) {
                try {
                    constructor.parameterTypes
                    break
                } catch (e: NotFoundException) {
                    val message = e.message
                    if (message != null && message != lastExMsg && !message.contains(' ')) {
                        context.classPool.makeClass(message)
                        lastExMsg = message
                    } else break
                }
            }
        } catch (e: Exception) {
            BasicBuildTracker("Transformer")
                    .trackError("Could not define class for params (2): ${constructor.signature}")
        }
    }

    private fun getParamType(descriptor: String, charIndex: Int): Pair<String?, Int> {
        var charIndex = charIndex
        var c = descriptor[charIndex]
        while (c == '[') {
            c = descriptor[++charIndex]
        }

        return if (c == 'L') {
            charIndex++
            val endIndex = descriptor.indexOf(';', charIndex)
            val name = descriptor.substring(charIndex, endIndex).replace('/', '.')
            Pair(name, endIndex + 1)
        } else Pair(null, charIndex + 1)
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
            val ctClass = makeCtClass(context, cursorClass)
            try {
                if (transformCursor(ctClass, context.outDir, context.classPool)) {
                    context.transformedClasses.add(cursorClass)
                }
            } catch (e: Exception) {
                throw TransformException("Could not transform Cursor class \"${ctClass.name}\" (${e.message})", e)
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
