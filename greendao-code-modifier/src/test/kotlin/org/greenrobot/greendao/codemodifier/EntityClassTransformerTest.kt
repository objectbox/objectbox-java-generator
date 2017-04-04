package org.greenrobot.greendao.codemodifier

import org.greenrobot.jdt.jdt.core.JavaCore
import org.greenrobot.jdt.jdt.internal.compiler.impl.CompilerOptions
import org.junit.Assert.*
import org.junit.Test

class EntityClassTransformerTest {
    val jdtOptions = JavaCore.getOptions()
    init {
        jdtOptions.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_7)
    }
    private val formattingOptions = FormattingOptions(Tabulation(' ', 4))

    @Test
    fun addMethod() {
        val entityClass = parseEntity(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;

            @Entity
            class Foobar {
                int age;

                class Bar {
                }
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
                int age;

                public void hello(String name, int age) {
                    System.out.println("Hello, " + name);
                }

                class Bar {
                }
            }
            """.trimIndent(),
            result
        )
    }

    @Test
    fun addConstructor() {
        val entityClass = parseEntity(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;

            @Entity
            class Foobar {
                String name;

                void someMethod() {
                    // do nothing
                }
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
                String name;

                public Foobar(String name, int age) {
                    System.out.println("Hello, " + name);
                }

                void someMethod() {
                    // do nothing
                }
            }
            """.trimIndent(),
            result
        )
    }

    @Test
    fun addField() {
        val entityClass = parseEntity(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;

            @Entity
            class Foobar {
                int age;

                Foobar() {
                }

                void someMethod() {
                }
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
                int age;
                /** Name of the Foobar */
                @Generated(hash = 802063436)
                private transient String name;

                Foobar() {
                }

                void someMethod() {
                }
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
        val entityClass = parseEntity(javaCode)

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
                    @Generated(hash = 977232244)
                    private transient int age;

                        private Foobar(){
                name = "MyName";
                        }
            }
            """.trimIndent()

        assertEquals(expectedChangedCode, result)
    }

    @Test
    fun replaceGeneratedMethod() {
        val entityClass = parseEntity(
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
        val methodCode = """
                @Generated(hash = GENERATED_HASH_STUB)
                public void hello(String name, int age) {
                    System.out.println("Hi, " + name);
                }
                """.trimIndent()
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defMethod("hello", "String", "int") { methodCode }
        }.writeToString()

        assertEquals(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                @Generated(hash = ${CodeCompare.codeHash(methodCode)})
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
        val entityClass = parseEntity(
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

        val constructorCode = """
                @Generated(hash = GENERATED_HASH_STUB)
                public Foobar(String name, int age) {
                    throw new RuntimeException("Error");
                }
                """.trimIndent()


        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defConstructor(listOf("String", "int")) { constructorCode }
        }.writeToString()

        assertEquals(
            //language=java
            """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                @Generated(hash = ${CodeCompare.codeHash(constructorCode)})
                public Foobar(String name, int age) {
                    throw new RuntimeException("Error");
                }
            }
            """.trimIndent(),
            result
        )
    }

    @Test
    fun doNotReplaceEmptyGeneratedConstructor() {
        val entityClass = parseEntity(
                //language=java
                """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                @Generated(hash = 400782353)
                public Foobar() {
                }
            }
            """.trimIndent()
        )

        val constructorCode = """
                @Generated(hash = GENERATED_HASH_STUB)
                public Foobar(String name, int age) {
                    throw new RuntimeException("Error");
                }
                """.trimIndent()


        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defConstructor(listOf("String", "int")) { constructorCode }
        }.writeToString()

        assertEquals(
                //language=java
                """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                @Generated(hash = 400782353)
                public Foobar() {
                }

                @Generated(hash = ${CodeCompare.codeHash(constructorCode)})
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
        val entityClass = parseEntity(
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
                @Generated(hash = 863598746)
                private transient String name;
            }
            """.trimIndent(),
            result
        )
    }

    @Test
    fun removeUnrequiredGeneratedCode() {
        val entityClass = parseEntity(
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
                @Generated(hash = -1)
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
                @Generated(hash = 863598746)
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
        val entityClass = parseEntity(originalCode)
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defMethod("hello", "String", "int") {
                """
                @Generated(hash = -1)
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
        val entityClass = parseEntity(originalCode)
        val result = EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defConstructor(listOf("String", "int")) {
                """
                @Generated(hash = -1)
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
        val entityClass = parseEntity(originalCode)
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
        val entityClass = parseEntity(originalCode)
        EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defMethod("hello", "String", "int") {
                """
                @Generated(hash = -1)
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
        val entityClass = parseEntity(originalCode)
        EntityClassTransformer(entityClass, jdtOptions, formattingOptions).apply {
            defConstructor(listOf("String", "int")) {
                """
                @Generated(hash = -1)
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
        val entityClass = parseEntity(originalCode)
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
        val entityClass = parseEntity(originalCode)
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
    fun replaceGeneratedMethodEvenIfOnlyJavaDocChanged() {
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
        val entityClass = parseEntity(originalCode)
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

        assertEquals(
            // language=java
            """
            import org.greenrobot.greendao.annotation.Entity;
            import org.greenrobot.greendao.annotation.Generated;

            @Entity
            class Foobar {
                /** Note is changed */
                @Generated
                public void hello(String name, int age) {
                    System.out.println("Hello, " + name);
                }
            }
            """.trimIndent(),
            result)
    }
}