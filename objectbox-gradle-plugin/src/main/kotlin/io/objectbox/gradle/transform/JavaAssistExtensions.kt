/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.objectbox.gradle.transform

import javassist.Modifier
import javassist.bytecode.AccessFlag
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.ClassFile
import javassist.bytecode.FieldInfo
import javassist.bytecode.SignatureAttribute
import javassist.bytecode.annotation.Annotation

fun FieldInfo.exGetGenericTypeArguments(): Array<out SignatureAttribute.TypeArgument>? {
    val signatureAttr = getAttribute(SignatureAttribute.tag) as SignatureAttribute
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

fun ClassFile.exGetAnnotation(name: String): Annotation? {
    var annotationsAttribute = getAttribute(AnnotationsAttribute.visibleTag) as AnnotationsAttribute?
    var annotation = annotationsAttribute?.getAnnotation(name)
    if (annotation == null) {
        annotationsAttribute = getAttribute(AnnotationsAttribute.invisibleTag) as AnnotationsAttribute?
        annotation = annotationsAttribute?.getAnnotation(name)
    }
    return annotation
}
