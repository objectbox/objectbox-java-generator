/*
 * ObjectBox Build Tools
 * Copyright (C) 2017-2024 ObjectBox Ltd.
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

import javassist.CtMethod
import javassist.Modifier
import javassist.bytecode.AccessFlag
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.FieldInfo
import javassist.bytecode.MethodInfo
import javassist.bytecode.Opcode
import javassist.bytecode.SignatureAttribute
import javassist.bytecode.annotation.Annotation
import javassist.expr.ExprEditor
import javassist.expr.FieldAccess

fun FieldInfo.exGetGenericTypeArguments(): Array<out SignatureAttribute.TypeArgument>? {
    val signatureAttr = getAttribute(SignatureAttribute.tag) as? SignatureAttribute ?: return null
    val objectType = SignatureAttribute.toFieldSignature(signatureAttr.signature)
    return (objectType as? SignatureAttribute.ClassType)?.typeArguments
}

fun FieldInfo.exGetSingleGenericTypeArgumentOrNull(): SignatureAttribute.ClassType? {
    return exGetGenericTypeArguments()?.singleOrNull()?.type as? SignatureAttribute.ClassType
}

fun FieldInfo.exGetModifiers(): Int {
    return AccessFlag.toModifier(accessFlags)
}

fun FieldInfo.exIsTransient(): Boolean {
    return Modifier.isTransient(exGetModifiers())
}

fun FieldInfo.exGetAnnotation(name: String): Annotation? {
    var annotationsAttribute = getAttribute(AnnotationsAttribute.visibleTag) as AnnotationsAttribute?
    var annotation = annotationsAttribute?.getAnnotation(name)
    if (annotation == null) {
        annotationsAttribute = getAttribute(AnnotationsAttribute.invisibleTag) as AnnotationsAttribute?
        annotation = annotationsAttribute?.getAnnotation(name)
    }
    return annotation
}

/**
 * Returns true if the method contains at least one statement that writes [ClassConst.boxStoreFieldName].
 */
fun CtMethod.assignsBoxStoreField(): Boolean {
    var assignsBoxStoreField = false
    instrument(object : ExprEditor() {
        override fun edit(f: FieldAccess?) {
            // Java: BoxStore field write access
            if (f?.fieldName == ClassConst.boxStoreFieldName && f.isWriter) {
                assignsBoxStoreField = true
            }
        }
    })
    return assignsBoxStoreField
}

fun ClassFile.exGetAnnotation(name: String): Annotation? {
    var annotationsAttribute = getAttribute(AnnotationsAttribute.visibleTag) as AnnotationsAttribute?
    var annotation = annotationsAttribute?.getAnnotation(name)
    if (annotation == null) {
        annotationsAttribute = getAttribute(AnnotationsAttribute.invisibleTag) as AnnotationsAttribute?
        annotation = annotationsAttribute?.getAnnotation(name)
    }
    return annotation
}

fun ClassFile.getInitializedFields(methodInfo: MethodInfo): HashSet<String> {
    val initializedFields = hashSetOf<String>()
    val codeIterator = methodInfo.codeAttribute.iterator()
    codeIterator.begin()
    while (codeIterator.hasNext()) {
        val opIndex = codeIterator.next()
        val op = codeIterator.byteAt(opIndex)
        if (op == Opcode.PUTFIELD) {
            val fieldIndex = codeIterator.u16bitAt(opIndex + 1)
            val fieldName = constPool.getFieldrefName(fieldIndex)
            if (fieldName != null) {
                initializedFields += fieldName
            }
        }
    }
    return initializedFields
}
