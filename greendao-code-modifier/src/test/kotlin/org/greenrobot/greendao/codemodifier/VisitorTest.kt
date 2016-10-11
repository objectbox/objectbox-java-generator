package org.greenrobot.greendao.codemodifier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.isEmpty
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test

/**
 * Tests if fields, constructors and methods are properly recognized.
 */
class VisitorTest : VisitorTestBase() {

    @Test
    fun fieldDef() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            String name;
            int age;
        }
        """)!!
        assertThat(entity.fields, equalTo(
                listOf(
                        EntityField(Variable(StringType, "name")),
                        EntityField(Variable(IntType, "age"), isNotNull = true)
                )
        ))
    }

    @Test
    fun noDefinitions() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {}
        """)!!
        assertThat(entity.fields, isEmpty)
        assertThat(entity.transientFields, isEmpty)
        assertThat(entity.methods, isEmpty)
        assertThat(entity.constructors, isEmpty)
        assertThat(entity.oneRelations, isEmpty)
        assertThat(entity.manyRelations, isEmpty)
    }

    @Test
    fun transientModifierTest() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            String name;
            transient int age;
        }
        """)!!
        assertThat(entity.fields, hasSize(equalTo(1)))
        assertThat(entity.transientFields, hasSize(equalTo(1)))
        assertThat(entity.fields[0].variable.name, equalTo("name"))
        assertThat(entity.transientFields[0].variable.name, equalTo("age"))
    }

    @Test
    fun transientAnnotationTest() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            String name;
            @Transient int age;
        }
        """)!!
        assertThat(entity.fields, hasSize(equalTo(1)))
        assertThat(entity.transientFields, hasSize(equalTo(1)))
        assertThat(entity.fields[0].variable.name, equalTo("name"))
        assertThat(entity.transientFields[0].variable.name, equalTo("age"))
    }

    @Test
    fun staticFieldsAreTransientTest() {
        val entity = visit(
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

        assertThat(entity.fields, hasSize(equalTo(1)))
        assertThat(entity.fields[0].variable.name, equalTo("name"))

        assertThat(entity.transientFields, hasSize(equalTo(2)))
        assertThat(entity.transientFields[0].variable.name, equalTo("staticField"))
        assertThat(entity.transientFields[1].variable.name, equalTo("CONSTANT_FIELD"))
    }

    @Test
    fun idAnnotation() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @Id
            String name;
        }
        """)!!
        val entityField = entity.fields[0]
        assertNotNull(entityField.id)
        assertFalse(entityField.id!!.autoincrement)
    }

    @Test
    @Ignore("Feature not yet available")
    fun idAutoincrement() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @Id(autoincrement = true)
            String name;
        }
        """)!!
        assertTrue(entity.fields[0].id!!.autoincrement)
    }

    @Test
    fun columnAnnotation() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @Property(nameInDb = "SECOND_NAME")
            String name;
        }
        """)!!
        val field = entity.fields[0]
        assertEquals("SECOND_NAME", field.dbName)
    }

    @Test
    fun propertyIndexAnnotation() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @Index(unique = true)
            String name;
        }
        """)!!
        val field = entity.fields[0]
        val index = field.index!!
        // TODO assertTrue(index.unique)
    }

    @Test
    @Ignore("Feature not yet available")
    fun convertAnnotation() {
        val entity = visit(
                //language=java
                """
        package com.example.myapp;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Convert;
        import com.example.myapp.Converter;

        @Entity
        class Foobar {
            @Convert(converter = Converter.class, dbType = String.class)
            MyType name;
        }
        """, listOf("MyType"))!!
        val field = entity.fields[0]
        assertEquals(
                EntityField(
                        Variable(VariableType("com.example.myapp.MyType", isPrimitive = false, originalName = "MyType"), "name"),
                        customType = CustomType(
                                "com.example.myapp.Converter", StringType
                        )
                ),
                field
        )
    }

    @Test
    @Ignore("Feature not yet available")
    fun convertAnnotation_innerClass() {
        val entity = visit(
                //language=java
                """
        package com.example.myapp;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Convert;
        import org.greenrobot.greendao.converter.PropertyConverter;

        @Entity
        class Foobar {
            @Convert(converter = InnerConverter.class, dbType = String.class)
            MyType name;
            public static class InnerConverter implements PropertyConverter<MyType, String> {
                @Override
                public MyType convertToEntityProperty(String databaseValue) {
                    return null;
                }

                @Override
                public String convertToDatabaseValue(MyType entityProperty) {
                    return null;
                }
            }
        }
        """, listOf("MyType"))!!
        val field = entity.fields[0]

        assertEquals(
                EntityField(
                        Variable(VariableType("com.example.myapp.MyType", isPrimitive = false, originalName = "MyType"), "name"),
                        customType = CustomType(
                                "com.example.myapp.Foobar.InnerConverter", StringType
                        )
                ),
                field
        )
    }

    @Test(expected = IllegalArgumentException::class)
    @Ignore("Feature not yet available")
    fun convertAnnotation_innerClassNotStatic() {
        visit(
                //language=java
                """
        package com.example.myapp;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Convert;
        import org.greenrobot.greendao.converter.PropertyConverter;

        @Entity
        class Foobar {
            @Convert(converter = InnerConverter.class, dbType = String.class)
            MyType name;
            public class InnerConverter implements PropertyConverter<MyType, String> {
                @Override
                public MyType convertToEntityProperty(String databaseValue) {
                    return null;
                }

                @Override
                public String convertToDatabaseValue(MyType entityProperty) {
                    return null;
                }
            }
        }
        """, listOf("MyType"))!!
    }

    @Test(expected = IllegalArgumentException::class)
    @Ignore("Feature not yet available")
    fun convertAnnotation_innerTypeNotStatic() {
        visit(
                //language=java
                """
        package com.example.myapp;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Convert;
        import org.greenrobot.greendao.converter.PropertyConverter;

        @Entity
        class Foobar {
            @Convert(converter = MyConverter.class, dbType = String.class)
            MyType name;

            public class MyType {
            }
        }
        """, listOf("MyConverter"))!!
    }

    @Test
    fun constructors() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            String name;
            int age;

            @Generated(hash = -1)
            Foobar(String name, int age) {
            }

            Foobar() {
            }
        }
        """)!!
        assertThat(entity.constructors, hasSize(equalTo(2)))
        assertThat(entity.constructors[0].parameters, equalTo(
                listOf(Variable(StringType, "name"), Variable(IntType, "age"))
        ))
        assertNull(entity.constructors[1].hint)
        assertThat(entity.constructors[1].parameters, equalTo(emptyList()))
        assertFalse(entity.constructors[1].generated)
    }

    @Test
    fun methods() {
        val entity = visit(
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