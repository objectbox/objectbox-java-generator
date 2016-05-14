package org.greenrobot.greendao.codemodifier

import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import org.junit.Assert.*
import org.junit.Test

class EntityClassTransformerTest {
    val jdtOptions = JavaCore.getOptions()
    init {
        jdtOptions.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_7)
        jdtOptions.put(CompilerOptions.OPTION_Encoding, "UTF-8")
    }
    private val formattingOptions = FormattingOptions(Tabulation(' ', 4))

    @Test
    fun addMethod() {
        val entityClass = parseEntityClass(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;

            @Entity
            class Foobar {
            }
            """.trimIndent()
        )
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defMethod("hello", "String", "int") {
                """
                public void hello(String name, int age) {
                    System.out.println("Hello, " + name);
                }
                """.trimIndent()
            }
        }.writeToString()

        assertEquals(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;

            @Entity
            class Foobar {

                public void hello(String name, int age) {
                    System.out.println("Hello, " + name);
                }
            }
            """.trimIndent(),
            result
        )
    }

    @Test
    fun addConstructor() {
        val entityClass = parseEntityClass(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;

            @Entity
            class Foobar {
            }
            """.trimIndent()
        )
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defConstructor(listOf("String", "int")) {
                """
                public Foobar(String name, int age) {
                    System.out.println("Hello, " + name);
                }
                """.trimIndent()
            }
        }.writeToString()

        assertNotNull(result)

        assertEquals(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;

            @Entity
            class Foobar {

                public Foobar(String name, int age) {
                    System.out.println("Hello, " + name);
                }
            }
            """.trimIndent(),
            result
        )
    }

    @Test
    fun addField() {
        val entityClass = parseEntityClass(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;

            @Entity
            class Foobar {
            }
            """.trimIndent()
        )
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defField("name", StringType, "Name of the Foobar")
        }.writeToString()

        assertNotNull(result)

        assertEquals(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;

            @Entity
            class Foobar {

                /** Name of the Foobar */
                @Generated
                private transient String name;
            }
            """.trimIndent(),
            result
        )
    }

    @Test
    fun doNotTouchUserFormatting() {
        //language=java
        val javaCode = """
            import org.greenrobot.greendao.annotation.Entity;

            @Entity
            class Foobar {
                private String name;

                public void hello10() {
                        // here comes bad formatted code
                        for(i=0;i<10;i++){System.out.println(   "Hello!"   );}
                }

                    public    int age=10000;

                        private Foobar(){
                name = "MyName";
                        }
            }
            """.trimIndent()
        val entityClass = parseEntityClass(javaCode)

        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defField("age", IntType)
        }.writeToString()

        //language=java
        val expectedChangedCode = """
            import org.greenrobot.greendao.annotation.Entity;

            @Entity
            class Foobar {
                private String name;

                public void hello10() {
                        // here comes bad formatted code
                        for(i=0;i<10;i++){System.out.println(   "Hello!"   );}
                }

                    public    int age=10000;

                        private Foobar(){
                name = "MyName";
                        }

                        @Generated
                        private transient int age;
            }
            """.trimIndent()

        assertEquals(expectedChangedCode, result)
    }

    @Test
    fun replaceGeneratedMethod() {
        val entityClass = parseEntityClass(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                @Generated
                public void hello(String name, int age) {
                    System.out.println("Hello, " + name);
                }
            }
            """.trimIndent()
        )
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defMethod("hello", "String", "int") {
                """
                @Generated
                public void hello(String name, int age) {
                    System.out.println("Hi, " + name);
                }
                """.trimIndent()
            }
        }.writeToString()

        assertEquals(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                @Generated
                public void hello(String name, int age) {
                    System.out.println("Hi, " + name);
                }
            }
            """.trimIndent(),
            result
        )
    }

    @Test
    fun replaceGeneratedConstructor() {
        val entityClass = parseEntityClass(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                @Generated
                public Foobar(String name, int age) {
                    this.name = name;
                    this.age = age;
                }
            }
            """.trimIndent()
        )
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defConstructor(listOf("String", "int")) {
                """
                @Generated
                public Foobar(String name, int age) {
                    throw new RuntimeException("Error");
                }
                """.trimIndent()
            }
        }.writeToString()

        assertEquals(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                @Generated
                public Foobar(String name, int age) {
                    throw new RuntimeException("Error");
                }
            }
            """.trimIndent(),
            result
        )
    }

    @Test
    fun replaceGeneratedField() {
        val entityClass = parseEntityClass(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                @Generated
                private transient String name = "John Lennon";
            }
            """.trimIndent()
        )
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defField("name", StringType)
        }.writeToString()

        assertEquals(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                @Generated
                private transient String name;
            }
            """.trimIndent(),
            result
        )
    }

    @Test
    fun removeUnrequiredGeneratedCode() {
        val entityClass = parseEntityClass(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                @Generated
                private transient String name;

                /** will be removed */
                @Generated
                private transient int age;

                /** will be removed */
                @Generated
                public Foobar() {
                }

                /** will be removed */
                @Generated
                public void hello() {
                }
            }
            """.trimIndent()
        )
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defField("name", StringType)
        }.writeToString()

        assertEquals(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                @Generated
                private transient String name;
            }
            """.trimIndent(),
            result
        )
    }

    @Test
    fun doNotReplaceKeepMarkedMethod() {
        //language=java
        val originalCode = """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Keep;

            @Entity
            class Foobar {
                @Keep
                public void hello(String name, int age) {
                    System.out.println("Hello, " + name);
                }
            }
            """.trimIndent()
        val entityClass = parseEntityClass(originalCode)
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defMethod("hello", "String", "int") {
                """
                @Generated
                public void hello(String name, int age) {
                    System.out.println("Hi, " + name);
                }
                """.trimIndent()
            }
        }.writeToString()

        assertNull(result)
    }

    @Test
    fun doNotReplaceKeepMarkedConstructor() {
        //language=java
        val originalCode = """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Keep;

            @Entity
            class Foobar {
                @Keep
                public Foobar(String name, int age) {
                    this.name = name;
                    this.age = age;
                }
            }
            """.trimIndent()
        val entityClass = parseEntityClass(originalCode)
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defConstructor(listOf("String", "int")) {
                """
                @Generated
                public Foobar(String name, int age) {
                    throw new RuntimeException("Error");
                }
                """.trimIndent()
            }
        }.writeToString()

        assertNull(result)
    }

    @Test
    fun doNotReplaceKeepMarkedField() {
        //language=java
        val originalCode = """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Keep;

            @Entity
            class Foobar {
                @Keep
                private transient String name = "John Lennon";
            }
            """.trimIndent()
        val entityClass = parseEntityClass(originalCode)
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defField("name", StringType)
        }.writeToString()

        assertNull(result)
    }

    @Test(expected = RuntimeException::class)
    fun failOnMethodConflict() {
        //language=java
        val originalCode = """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Keep;

            @Entity
            class Foobar {
                public void hello(String name, int age) {
                    System.out.println("Hello, " + name);
                }
            }
            """.trimIndent()
        val entityClass = parseEntityClass(originalCode)
        EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defMethod("hello", "String", "int") {
                """
                @Generated
                public void hello(String name, int age) {
                    System.out.println("Hi, " + name);
                }
                """.trimIndent()
            }
        }
    }

    @Test(expected = RuntimeException::class)
    fun failOnConstructorConflict() {
        //language=java
        val originalCode = """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Keep;

            @Entity
            class Foobar {
                public Foobar(String name, int age) {
                    this.name = name;
                    this.age = age;
                }
            }
            """.trimIndent()
        val entityClass = parseEntityClass(originalCode)
        EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defConstructor(listOf("String", "int")) {
                """
                @Generated
                public Foobar(String name, int age) {
                    throw new RuntimeException("Error");
                }
                """.trimIndent()
            }
        }
    }

    @Test(expected = RuntimeException::class)
    fun failOnFieldConflict() {
        //language=java
        val originalCode = """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Keep;

            @Entity
            class Foobar {
                private transient String name = "John Lennon";
            }
            """.trimIndent()
        val entityClass = parseEntityClass(originalCode)
        EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defField("name", StringType)
        }
    }

    @Test
    fun doNotReplaceGeneratedMethodIfOnlyFormattingChanged() {
        // use 2-space formatting and less spaces for for-statement
        //language=java
        val originalCode = """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
              @Generated
              public void hello(String name, int age) {
                for(int i=0;i<10;i++){
                  System.out.println("Hello, " + name);
                }
              }
            }
            """.trimIndent()
        val entityClass = parseEntityClass(originalCode)
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defMethod("hello", "String", "int") {
                """
                @Generated
                public void hello(String name, int age) {
                    for(int i = 0; i < 10; i++){
                        System.out.println("Hello, " + name);
                    }
                }
                """.trimIndent()
            }
        }.writeToString()

        assertNull(result)
    }

    @Test
    fun doNotReplaceGeneratedMethodIfOnlyJavaDocChanged() {
        //language=java
        val originalCode = """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                /** Note: age is not used */
                @Generated
                public void hello(String name, int age) {
                    System.out.println("Hello, " + name);
                }
            }
            """.trimIndent()
        val entityClass = parseEntityClass(originalCode)
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defMethod("hello", "String", "int") {
                """
                /** Note is changed */
                @Generated
                public void hello(String name, int age) {
                    System.out.println("Hello, " + name);
                }
                """.trimIndent()
            }
        }.writeToString()

        assertNull(result)
    }
}