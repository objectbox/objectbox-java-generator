package io.objectbox.gradle.transform

import javassist.ClassClassPath
import java.io.InputStream
import java.net.URL

/**
 * Only resolves classes with a given prefix
 */
class PrefixedClassPath(val prefix: String, clazz: Class<*>) : ClassClassPath(clazz) {
    override fun find(classname: String?): URL? {
        if (classname!!.startsWith(prefix)) {
            return super.find(classname)
        } else return null
    }

    override fun openClassfile(classname: String?): InputStream? {
        if (classname!!.startsWith(prefix)) {
            return super.openClassfile(classname)
        } else return null
    }
}