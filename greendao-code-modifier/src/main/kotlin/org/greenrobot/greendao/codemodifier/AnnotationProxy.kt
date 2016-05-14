package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.dom.*
import org.eclipse.jdt.core.dom.Annotation
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Proxies JDT's {@link org.eclipse.jdt.core.dom.Annotation} to the actual annotation object,
 * allows to read annotation's data in the regular java's reflection way
 *
 * <p>
 * E.g. given the annotation:
 * '@interface Entity {
 *      String name() default "nonaname";
 * }
 *
 * End using class:
 * '@Entity("NOTES")
 * class Note { ... }
 *
 * Use AnnotationProxy like so:
 * val proxy = AnnotationProxy<Entity>(noteClassJdtAnnotationNode)
 * assert(proxy.name == "NOTES")
 * </p>
 *
 * <p>
 * Supported features:
 * - return default values, if no value is provided
 * - raw string literals
 * - raw boolean literals
 * - annotation as values, e.g. Table annotation is proxied automatically in "@Entity(table = @Table(name = "Hello"))"
 * - arrays of all above
 * </p>
 * <p>
 * Unsupported, but easy to implement:
 * - other primitive literals (int, long, byte...)
 * - references to enum values, which are available in classpath
 * </p>
 * <p>
 * Unsupported and hard to implement:
 * - class references (usually referenced classes are in user code, so they are not available in the current classpath)
 * - references to any primitive or String values from user code (e.g. "@Entity(name = MyConstants.ENTITY_NAME)")
 * - references to enum values from user code
 * </p>
 */
object AnnotationProxy {
    class Handler(val jdtAnnotation: Annotation) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            return when (jdtAnnotation) {
                is SingleMemberAnnotation -> if (method.name == "value") {
                    jdtAnnotation.value.javaValue(method.returnType)
                } else {
                    method.defaultValue
                }
                is NormalAnnotation -> {
                    jdtAnnotation[method.name]
                        ?.let { it.javaValue(method.returnType) }
                        ?: method.defaultValue
                }
                else -> method.defaultValue
            }
        }
    }

    operator fun invoke(jdtAnnotation: Annotation, type: Class<*>) : Any =
        Proxy.newProxyInstance(type.classLoader, arrayOf(type), Handler(jdtAnnotation))

    inline operator fun <reified T : kotlin.Annotation> invoke(jdtAnnotation: Annotation) : T =
        this(jdtAnnotation, T::class.java) as T

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any, C : Class<out T>> Expression.javaValue(expected: C) : T {
        when {
            expected.isArray -> if (this is ArrayInitializer) {
                val componentType = expected.componentType
                val expressions = this.expressions()
                val result = java.lang.reflect.Array.newInstance(componentType, expressions.size)
                expressions.forEachIndexed { i, exp ->
                    java.lang.reflect.Array.set(result, i, (exp as Expression).javaValue(componentType))
                }
                return result as T
            }
            expected == Boolean::class.javaPrimitiveType -> if (this is BooleanLiteral) return booleanValue() as T
            expected == String::class.java -> if (this is StringLiteral) return literalValue as T
            expected.isAnnotation -> if (this is Annotation) return AnnotationProxy(this, expected) as T
        }

        throw RuntimeException("Can't get ${expected.simpleName} value from ${this.javaClass}")
    }
}
