/*
 * Copyright (C) 2017-2024 ObjectBox Ltd.
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

import javassist.ClassClassPath
import java.io.InputStream
import java.net.URL

/**
 * Only resolves classes with a given prefix
 */
class PrefixedClassPath(private val prefix: String, clazz: Class<*>) : ClassClassPath(clazz) {
    override fun find(classname: String?): URL? {
        return if (classname!!.startsWith(prefix)) {
            super.find(classname)
        } else null
    }

    override fun openClassfile(classname: String?): InputStream? {
        return if (classname!!.startsWith(prefix)) {
            super.openClassfile(classname)
        } else null
    }
}