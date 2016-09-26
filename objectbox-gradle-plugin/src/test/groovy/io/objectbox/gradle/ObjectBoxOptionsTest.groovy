package io.objectbox.gradle

import org.gradle.api.Project

import static org.mockito.Matchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ObjectBoxOptionsTest extends GroovyTestCase {
    void testSchemaOptions() {
        def project = mock(Project)
        when(project.file(any(String))).thenReturn(mock(File))

        def options = new ObjectBoxOptions(project)
        options.with {
            schemas {
                notes
                orders {
                    version 2
                }
            }
        }

        assert options.schemas.schemasMap.keySet() == ["notes", "orders"].toSet()
        assert options.schemas.orders.version == 2
        assert options.schemas.notes.version == null
    }
}
