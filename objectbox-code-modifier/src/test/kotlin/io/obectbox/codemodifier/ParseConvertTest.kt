package io.obectbox.codemodifier

import io.objectbox.codemodifier.CustomType
import io.objectbox.codemodifier.ParseException
import io.objectbox.codemodifier.Variable
import io.objectbox.codemodifier.VariableType
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests @Convert annotation.
 */
class ParseConvertTest : ParseTestBase() {
    val myType = VariableType("com.example.myapp.MyType", isPrimitive = false, originalName = "MyType")

    @Test
    fun convertAnnotation() {
        val entity = parse(
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
        val field = entity.properties[0]
        assertEquals(Variable(myType, "name"), field.variable)
        assertEquals(CustomType("com.example.myapp.Converter", StringType), field.customType)
        assertTrue(field.fieldAccessible)
    }

    @Test
    fun convertAnnotation_missingAttributes() {
        try {
            parse(
                    //language=java
                    """
        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Convert;

        @Entity
        class Foobar {
            @Convert
            MyType name;
        }
        """, listOf("MyType"))!!
            fail("Should have failed")
        } catch (e: ParseException) {
            assertTrue(e.message, e.message!!.contains("@Convert attributes absent "))
        }
    }

    @Test
    fun convertAnnotation_list() {
        val entity = parse(
                //language=java
                """
        package com.example.myapp;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.Convert;
        import com.example.myapp.Converter;
        import java.util.List;

        @Entity
        class Foobar {
            @Convert(converter = Converter.class, dbType = String.class)
            List<MyType> name;
        }
        """, listOf("MyType"))!!
        val field = entity.properties[0]
        assertEquals(myType, field.variable.type.typeArguments!![0])
        assertEquals(CustomType("com.example.myapp.Converter", StringType), field.customType)
        assertTrue(field.fieldAccessible)
    }

    @Test
    fun convertAnnotation_innerClass() {
        val entity = parse(
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
        val field = entity.properties[0]
        assertEquals(Variable(myType, "name"), field.variable)
        assertEquals(CustomType("com.example.myapp.Foobar.InnerConverter", StringType), field.customType)
        assertTrue(field.fieldAccessible)
    }

    @Test(expected = IllegalArgumentException::class)
    fun convertAnnotation_innerClassNotStatic() {
        parse(
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
    fun convertAnnotation_innerTypeNotStatic() {
        parse(
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

}