package io.objectbox.gradle.transform

import io.objectbox.annotation.Entity
import javassist.bytecode.SignatureAttribute

object ClassConst {
    val entityAnnotationName = Entity::class.qualifiedName

    val toOne = "io/objectbox/relation/ToOne"
    val toOneDescriptor = "L${toOne};"

    val toMany = "io/objectbox/relation/ToMany"
    val toManyDescriptor = "L${toMany};"

    val boxStoreFieldName = "__boxStore"
    val boxStoreClass = "io.objectbox.BoxStore"

    val cursorClass = "io.objectbox.Cursor"
    val cursorAttachEntityMethodName = "attachEntity"

    val listClass = "java/util/List"
    val listDescriptor = "L${listClass};"

    val genericSignatureT =
            SignatureAttribute.ClassSignature(arrayOf(SignatureAttribute.TypeParameter("T"))).encode()!!
}