package io.obectbox.codemodifier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import io.objectbox.codemodifier.Variable
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test

/**
 * Tests if fields, constructors and methods are properly recognized.
 */
class ParseBasicsTest : ParseTestBase() {

    @Test
    fun fieldDef() {
        val entity = parse(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            String name;
            private int age;
        }
        """)!!
        assertEquals(Variable(StringType, "name"), entity.properties[0].variable)
        assertTrue(entity.properties[0].fieldAccessible)

        assertEquals(Variable(IntType, "age"), entity.properties[1].variable)
        assertTrue(entity.properties[1].isNotNull)
    }

    @Test
    fun noDefinitions() {
        val entity = parse(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {}
        """)!!
        assertThat(entity.properties, isEmpty)
        assertThat(entity.transientFields, isEmpty)
        assertThat(entity.methods, isEmpty)
        assertThat(entity.constructors, isEmpty)
        assertThat(entity.toOneRelations, isEmpty)
        assertThat(entity.toManyRelations, isEmpty)
    }

    @Test
    fun transientModifierTest() {
        val entity = parse(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            String name;
            transient int age;
        }
        """)!!
        assertThat(entity.properties.size, equalTo(1))
        assertThat(entity.transientFields.size, equalTo(1))
        assertThat(entity.properties[0].variable.name, equalTo("name"))
        assertThat(entity.transientFields[0].variable.name, equalTo("age"))
    }

    @Test
    fun transientAnnotationTest() {
        val entity = parse(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            String name;
            @Transient int age;
        }
        """)!!
        assertThat(entity.properties.size, equalTo(1))
        assertThat(entity.transientFields.size, equalTo(1))
        assertThat(entity.properties[0].variable.name, equalTo("name"))
        assertThat(entity.transientFields[0].variable.name, equalTo("age"))
    }

    @Test
    fun staticFieldsAreTransientTest() {
        val entity = parse(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            static String staticField;
            static final String CONSTANT_FIELD;
            String name;
        }
        """)!!

        assertThat(entity.properties.size, equalTo(1))
        assertThat(entity.properties[0].variable.name, equalTo("name"))

        assertThat(entity.transientFields.size, equalTo(2))
        assertThat(entity.transientFields[0].variable.name, equalTo("staticField"))
        assertThat(entity.transientFields[1].variable.name, equalTo("CONSTANT_FIELD"))
    }

    @Test
    fun idAnnotation() {
        val entity = parse(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @Id
            String name;
        }
        """)!!
        val entityField = entity.properties[0]
        assertNotNull(entityField.idParams)
        assertFalse(entityField.idParams!!.autoincrement)
    }

    @Test
    @Ignore("Feature not yet available")
    fun idAutoincrement() {
        val entity = parse(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @Id(autoincrement = true)
            String name;
        }
        """)!!
        assertTrue(entity.properties[0].idParams!!.autoincrement)
    }

    @Test
    fun nameInDbAnnotation() {
        val entity = parse(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @NameInDb("SECOND_NAME")
            String name;
        }
        """)!!
        val field = entity.properties[0]
        assertEquals("SECOND_NAME", field.dbName)
    }

    @Test
    fun propertyIndexAnnotation() {
        val entity = parse(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @Index(unique = true)
            String name;
        }
        """)!!
        val field = entity.properties[0]
        assertNotNull(field.index)
        // TODO assertTrue(field.index!!.unique)
    }

    @Test
    fun propertyUniqueAnnotation() {
        val entity = parse(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @Unique
            String name;
        }
        """)!!
        assertNotNull(entity)
        // TODO assertTrue(entity.properties[0].unique)
    }

    @Test
    fun constructors() {
        val entity = parse(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            String name;
            int age;

            @Generated(-1)
            Foobar(String name, int age) {
            }

            Foobar() {
            }
        }
        """)!!
        assertThat(entity.constructors.size, equalTo(2))
        assertThat(entity.constructors[0].parameters, equalTo(
                listOf(Variable(StringType, "name"), Variable(IntType, "age"))
        ))
        assertNull(entity.constructors[1].hint)
        assertThat(entity.constructors[1].parameters, equalTo(emptyList()))
        assertFalse(entity.constructors[1].generated)
    }

    @Test
    fun methods() {
        val entity = parse(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            String name;

            public void setName(String name) {
                this.name = name;
            }

            public String getName() {
                return this.name;
            }
        }
        """)!!

        assertEquals(2, entity.methods.size)
        assertEquals("setName", entity.methods[0].name)
        assertThat(entity.methods[0].parameters, equalTo(listOf(Variable(StringType, "name"))))
        assertEquals("getName", entity.methods[1].name)
        assertThat(entity.methods[1].parameters, equalTo(emptyList()))
    }

}