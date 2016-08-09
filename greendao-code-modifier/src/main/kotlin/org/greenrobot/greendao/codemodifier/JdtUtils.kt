package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.dom.*
import kotlin.reflect.KClass

/** Tries to find exact import or import on demand (e.g. import java.lang.*) for the qualifiedName */
fun Iterable<ImportDeclaration>.has(qualifiedName : String) : Boolean {
    val packageName = qualifiedName.substringBeforeLast('.', "")
    return any {
        val name = it.name
        name.fullyQualifiedName == qualifiedName
                || (it.isOnDemand && name is QualifiedName && name.fullyQualifiedName == packageName)
    }
}

val Name.simpleName : String
    get() = when (this) {
        is QualifiedName -> this.name.fullyQualifiedName
        else -> this.fullyQualifiedName
    }

val Name.qualifier : String
    get() = when (this) {
        is QualifiedName -> this.qualifier.fullyQualifiedName
        else -> ""
    }

/** no on-demand imports */
fun Iterable<ImportDeclaration>.isStrict() : Boolean = none { it.isOnDemand && !it.isStatic }

private val JavaLangTypes = setOf("Long", "Byte", "Integer", "Boolean", "Short", "Float", "Double", "String")

/**
 * Tries to resolve qualified name if it is not already qualified
 * @return same name if it is qualified,
 *         qualified name if it was simple,
 *         same simple name if qualified name can not be resolved
 **/
fun Name.resolveName(outerClassName: String?, imports : Iterable<ImportDeclaration>, sourcePkg : String?,
                     classesInPackage: List<String>) : String {
    val simpleName = fullyQualifiedName
    return when {
        this is SimpleName -> {
            imports.find { it.name.simpleName == simpleName }?.let { it.name.fullyQualifiedName }
                ?: run {
                // not found name in imports, so...
                // is it one of the classes in this package?
                if (classesInPackage.contains(simpleName)) {
                    if (sourcePkg != null) {
                        "$sourcePkg.$simpleName"
                    } else {
                        simpleName
                    }
                } else if (imports.isStrict() && !JavaLangTypes.contains(simpleName)) {
                    // assume it is defined inline
                    if (outerClassName != null) {
                        if (sourcePkg != null) {
                            "$sourcePkg.$outerClassName.$simpleName"
                        } else {
                            "$outerClassName.$simpleName"
                        }
                    } else {
                        simpleName
                    }
                } else if (JavaLangTypes.contains(simpleName)) {
                    // assume that java.lang.* types are not overwritten
                    simpleName
                } else {
                    throw IllegalArgumentException("Can't resolve qualified name for $simpleName. " +
                            "Try to not use on-demand imports or specify qualified name explicitly (line $lineNumber)")
                }
            }
        }
        // assume nobody starts name of global package with a capital letter
        this is QualifiedName && fullyQualifiedName[0].isUpperCase() ->
            qualifier.resolveName(outerClassName, imports, sourcePkg, classesInPackage) + "." + this.name.identifier

        else -> simpleName
    }
}

fun Iterable<ImportDeclaration>.has(klass : KClass<*>) : Boolean = klass.qualifiedName?.let{has(it)} ?: false

operator fun NormalAnnotation.get(fieldName: String) : Expression? {
    return values().asSequence()
        .map { it as MemberValuePair }
        .find { it.name.identifier == fieldName }
        ?.value
}

fun Type.typeName(containingClassIdentifier : String?, imports : Iterable<ImportDeclaration>, sourcePkg: String?,
                  classesInPackage: List<String>) : String {
    return when (this) {
        is SimpleType -> name.resolveName(containingClassIdentifier, imports, sourcePkg, classesInPackage)
        is ArrayType -> elementType.typeName(null, imports, sourcePkg, classesInPackage) + "[]"
        is QualifiedType -> qualifier.typeName(null, imports, sourcePkg, classesInPackage) + "." + name.identifier
        is ParameterizedType -> type.typeName(null, imports, sourcePkg, classesInPackage)
        else -> toString()
    }
}

val ASTNode.lineNumber : Int?
    get() {
        val root = root
        return if (root is CompilationUnit) {
            root.getLineNumber(startPosition)
        } else {
            null
        }
    }