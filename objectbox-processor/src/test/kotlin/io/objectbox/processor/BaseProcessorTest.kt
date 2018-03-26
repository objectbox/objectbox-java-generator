package io.objectbox.processor

import com.google.common.truth.Truth
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.JavaFileObjectSubject
import com.google.testing.compile.JavaFileObjects
import io.objectbox.generator.model.Property
import io.objectbox.generator.model.PropertyType


abstract class BaseProcessorTest {

    protected fun assertPrimitiveType(prop: Property, type: PropertyType) {
        Truth.assertThat(prop.propertyType).isEqualTo(type)
        Truth.assertThat(prop.isNotNull).isTrue()
        Truth.assertThat(prop.isNonPrimitiveType).isFalse()
    }

    protected fun assertType(prop: Property, type: PropertyType) {
        Truth.assertThat(prop.propertyType).isEqualTo(type)
        Truth.assertThat(prop.isNotNull).isFalse()
        Truth.assertThat(prop.isNonPrimitiveType).isTrue()
    }

    protected fun assertGeneratedSourceMatches(compilation: Compilation, simpleName: String) {
        val generatedFile = getGeneratedJavaFile(compilation, simpleName)
        generatedFile.hasSourceEquivalentTo(JavaFileObjects.forResource("expected-source/$simpleName.java"))
    }

    /** non-null*/
    protected fun getGeneratedJavaFile(compilation: Compilation, simpleName: String): JavaFileObjectSubject {
        val generatedFile = CompilationSubject.assertThat(compilation)
                .generatedSourceFile("io.objectbox.processor.test.$simpleName")
        generatedFile.isNotNull()
        return generatedFile
    }

}