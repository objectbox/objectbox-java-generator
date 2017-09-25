package io.objectbox.gradle

import org.gradle.api.Project

import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PluginOptionsTest extends GroovyTestCase {
    void testSchemaOptions() {
        def project = mock(Project)
        when(project.file(any(String))).thenReturn(mock(File))

        def options = new PluginOptions(project)
        options.with {
            debug = true
        }

        assert options.debug
    }
}
