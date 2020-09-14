package io.objectbox.processor

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.JavaFileObjectSubject
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType


abstract class BaseProcessorTest {

    protected fun assertPrimitiveType(prop: Property, type: PropertyType) {
        assertThat(prop.propertyType).isEqualTo(type)
        assertThat(prop.isNotNull).isTrue()
        assertThat(prop.isNonPrimitiveType).isFalse()
    }

    protected fun assertType(prop: Property, type: PropertyType) {
        assertThat(prop.propertyType).isEqualTo(type)
        assertThat(prop.isNotNull).isFalse()
        assertThat(prop.isNonPrimitiveType).isTrue()
    }

    protected fun Compilation.generatedSourceFileOrFail(qualifiedName: String): JavaFileObjectSubject {
        val generatedFile = CompilationSubject
            .assertThat(this)
            .generatedSourceFile(qualifiedName)
        generatedFile.isNotNull()
        return generatedFile
    }

    protected fun Compilation.assertGeneratedSourceMatches(qualifiedName: String, fileName: String) {
        generatedSourceFileOrFail(qualifiedName)
            .hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/$fileName"))
    }

    /**
     * Assumes type is in "io.objectbox.processor.test" package and file is named "$simpleName.java".
     */
    protected fun Compilation.assertGeneratedSourceMatches(simpleName: String) {
        assertGeneratedSourceMatches(
            "io.objectbox.processor.test.$simpleName",
            "$simpleName.java"
        )
    }

}