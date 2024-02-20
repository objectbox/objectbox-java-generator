/*
 * Copyright (C) 2018-2022 ObjectBox Ltd.
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

package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType


abstract class BaseProcessorTest {

    protected fun assertPrimitiveType(prop: Property, type: PropertyType) {
        assertThat(prop.propertyType).isEqualTo(type)
        assertThat(prop.isTypeNotNull).isTrue()
        assertThat(prop.isNotNullFlag).isFalse() // Unused, so should never be set.
        assertThat(prop.isNonPrimitiveFlag).isFalse()
    }

    protected fun assertType(prop: Property, type: PropertyType, hasNonPrimitiveFlag: Boolean = false) {
        assertThat(prop.propertyType).isEqualTo(type)
        assertThat(prop.isTypeNotNull).isFalse()
        assertThat(prop.isNotNullFlag).isFalse() // Unused, so should never be set.
        assertThat(prop.isNonPrimitiveFlag).isEqualTo(hasNonPrimitiveFlag)
    }

}
