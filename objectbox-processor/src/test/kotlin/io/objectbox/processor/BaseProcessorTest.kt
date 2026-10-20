/*
 * ObjectBox Build Tools
 * Copyright (C) 2018-2026 ObjectBox Ltd.
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

package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Base class for processor tests: tests run for both the annotation processor and the KSP
 * processor (which should behave identical), create a test environment for the current one
 * with [env].
 */
@RunWith(Parameterized::class)
abstract class BaseProcessorTest(protected val processorType: ProcessorType) {

    enum class ProcessorType {
        JAVAC,
        KSP
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun processorTypes(): List<ProcessorType> = ProcessorType.entries
    }

    /**
     * Creates a [TestEnvironment] running the processor variant of the current test run.
     */
    protected fun env(
        modelFile: String,
        myObjectBoxPackage: String? = null,
        optionDisableTransform: Boolean = false,
        useTemporaryModelFile: Boolean = false
    ): TestEnvironment = when (processorType) {
        ProcessorType.JAVAC -> JavacTestEnvironment(
            modelFile, myObjectBoxPackage, optionDisableTransform, useTemporaryModelFile
        )

        ProcessorType.KSP -> KspTestEnvironment(
            modelFile, myObjectBoxPackage, optionDisableTransform, useTemporaryModelFile
        )
    }

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
