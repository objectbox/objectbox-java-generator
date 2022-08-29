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
