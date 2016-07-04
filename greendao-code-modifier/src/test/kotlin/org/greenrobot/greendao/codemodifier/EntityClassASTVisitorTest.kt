package org.greenrobot.greendao.codemodifier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.isBlank
import com.natpryce.hamkrest.isEmpty
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test

class EntityClassASTVisitorTest {
    val BarType = VariableType("com.example.Bar", false, "Bar")
    val BarItemType = VariableType("com.example.Bar.Item", false, "Bar.Item")
    val BarListType = VariableType("java.util.List", false, "List<Bar>", listOf(BarType))

    fun visit(code: String, classesInPackage: List<String> = emptyList()) =
            tryParseEntityClass(code, classesInPackage)

    @Test
    fun entityIsRecognised() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.Entity;

        @Entity class Foobar {}
        """)
        assertNotNull(entity)
    }

    @Test
    fun entityIsRecognisedQualifiedName() {
        val entity = visit(
                //language=java
                """
        @org.greenrobot.greendao.annotation.Entity class Foobar {}
        """)
        assertNotNull(entity)
    }

    @Test
    fun entityIsNotRecognized() {
        val entity = visit("class Foobar {}")
        assertNull(entity)
    }

    @Test
    fun entityIsNotRecognizedIfWrongAnnotation() {
        val entity = visit(
                //language=java
                """
        import myapp.Entity;

        @Entity class Foobar {}
        """)
        assertNull(entity)
    }

    @Test
    fun entityIsNotRecognizedWithoutAnnotationImport() {
        val entity = visit(
                //language=java
                """
        @Entity class Foobar {}
        """)
        assertNull(entity)
    }

    @Test(expected = RuntimeException::class)
    fun entityIsNotRecognizedAmbigousImport() {
        visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;
        import org.redrobot.reddao.annotations.*;

        @Entity class Foobar {}
        """)
    }

    @Test
    fun activeEntity() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.Entity;

        @Entity(active = true) class Foobar {}
        """)!!
        assertTrue(entity.active)
    }

    @Test
    fun entityName() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity class Foobar {}
        """)!!
        assertEquals("Foobar", entity.name)
    }

    @Test
    fun packageName() {
        val entity = visit(
                //language=java
                """
        package com.user.myapp;

        import org.greenrobot.greendao.annotation.*;

        @Entity class Foobar {}
        """)!!
        assertEquals("com.user.myapp", entity.packageName)
    }

    @Test
    fun noPackageName() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity class Foobar {}
        """)!!
        assertThat(entity.packageName, isBlank)
    }

    @Test
    fun noCustomTableName() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity class Foobar {}
        """)!!
        assertNull(entity.tableName)
    }

    @Test
    fun customTableName() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity(nameInDb = "BAR")
        class Foobar {}
        """)!!
        assertEquals("BAR", entity.tableName)
    }

    @Test
    fun defaultSchemaName() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {}
        """)!!
        assertEquals("default", entity.schema)
    }

    @Test
    fun customSchemaName() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity(schema="custom")
        class Foobar {}
        """)!!
        assertEquals("custom", entity.schema)
    }

    @Test
    fun noKeepAnnotation() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {}
        """)!!
        assertFalse(entity.keepSource)
    }

    @Test
    fun keepAnnotation() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity
        @Keep
        class Foobar {}
        """)!!
        assertTrue(entity.keepSource)
    }

    @Test
    fun fieldDef() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

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
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {}
        """)!!
        assertThat(entity.fields, isEmpty)
        assertThat(entity.transientFields, isEmpty)
        assertThat(entity.methods, isEmpty)
        assertThat(entity.constructors, isEmpty)
        assertThat(entity.oneRelations, isEmpty)
        assertThat(entity.manyRelations, isEmpty)
        assertThat(entity.indexes, isEmpty)
    }

    @Test
    fun transientModifierTest() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

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
        import org.greenrobot.greendao.annotation.*;

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
    fun idAnnotation() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

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
    fun idAutoincrement() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

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
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {
            @Property(nameInDb = "SECOND_NAME")
            String name;
        }
        """)!!
        val field = entity.fields[0]
        assertEquals("SECOND_NAME", field.columnName)
    }

    @Test
    fun fieldGeneratorHint() {
        val cityHash = CodeCompare.codeHash("@Transient String city;")
        val entity = visit(
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {
            @Generated
            transient String name;

            @Keep
            transient int age;

            transient String surname;

            @Generated(hash = $cityHash)
            @Transient
            String city;
        }
        """.trimIndent())!!
        assertThat(entity.transientFields.map { it.hint }, equalTo(
                listOf(GeneratorHint.Generated(-1), GeneratorHint.Keep, null, GeneratorHint.Generated(cityHash))
        ))
    }

    @Test(expected = RuntimeException::class)
    fun throwIfGeneratedFieldChanged() {
        visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {
            @Generated(hash = 10)
            transient String name;
        }
        """)!!
    }

    @Test(expected = RuntimeException::class)
    fun throwIfGeneratedMethodChanged() {
        visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {
            @Generated(hash = 10)
            void hello() {
                // body
            }
        }
        """)!!
    }

    @Test(expected = RuntimeException::class)
    fun throwIfGeneratedConstructorChanged() {
        visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {
            @Generated(hash = 10)
            Foobar() {
                // body
            }
        }
        """)!!
    }

    @Test
    fun propertyIndexAnnotation() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {
            @Index(name = "NAME_INDEX", unique = true)
            String name;
        }
        """)!!
        val field = entity.fields[0]
        val index = field.index!!
        assertEquals("NAME_INDEX", index.name)
        assertTrue(index.unique)
    }

    @Test(expected = RuntimeException::class)
    fun propertyIndexAnnotationDoesNotAcceptIndexSpec() {
        visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {
            @Index(value = "name desc")
            String name;
        }
        """)!!
    }

    @Test
    fun propertyUniqueAnnotation() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {
            @Unique
            String name;
        }
        """)!!
        assertTrue(entity.fields[0].unique)
    }

    @Test
    fun convertAnnotation() {
        val entity = visit(
                //language=java
                """
        package com.example.myapp;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.Convert;
        import com.example.myapp.Converter;

        @Entity
        class Foobar {
            @Convert(converter = Converter.class, columnType = String.class)
            MyType name;
        }
        """)!!
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
    @Ignore("Not yet working")
    fun convertAnnotation_innerClass() {
        val entity = visit(
                //language=java
                """
        package com.example.myapp;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.Convert;
        import org.greenrobot.greendao.converter.PropertyConverter;

        @Entity
        class Foobar {
            @Convert(converter = InnerConverter.class, columnType = String.class)
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
        """)!!
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

    @Test
    fun multiIndexes() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity(indexes = {
            @Index(value = "name DESC, age", unique = true, name = "NAME_AGE_INDEX"),
            @Index("age, name")
        })
        class Foobar {
            String name;
            int age;
        }
        """)!!

        assertThat(entity.indexes, equalTo(listOf(
                TableIndex("NAME_AGE_INDEX", listOf(
                        OrderProperty("name", Order.DESC),
                        OrderProperty("age", Order.ASC)
                ), unique = true),
                TableIndex(null, listOf(
                        OrderProperty("age", Order.ASC),
                        OrderProperty("name", Order.ASC)
                ), unique = false)
        )))
    }

    @Test
    fun constructors() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

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
    fun constructorGeneratorHint() {
        val constructorHash = CodeCompare.codeHash(
                "Foobar(String name, int age){}"
        )

        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {
            String name;
            int age;

            @Generated
            Foobar(String name) {
            }

            @Generated(hash = $constructorHash)
            Foobar(String name, int age) {
            }

            @Keep
            Foobar(int age) {
            }

            Foobar() {
            }
        }
        """)!!
        assertEquals(entity.constructors[0].hint, GeneratorHint.Generated(-1))
        assertEquals(entity.constructors[1].hint, GeneratorHint.Generated(constructorHash))
        assertEquals(entity.constructors[2].hint, GeneratorHint.Keep)
        assertNull(entity.constructors[3].hint)
    }

    @Test
    fun methods() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

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

    @Test
    fun methodGeneratorHint() {
        val entity = visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.*;

        @Entity
        class Foobar {
            String name;
            int age;

            @Generated
            void update() {
            }

            @Keep
            void remove() {
            }

            void refresh() {
            }
        }
        """)!!
        assertEquals(entity.methods[0].hint, GeneratorHint.Generated(-1))
        assertEquals(entity.methods[1].hint, GeneratorHint.Keep)
        assertNull(entity.methods[2].hint)
    }

    @Test
    fun toOne() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.ToOne;

        @Entity
        class Foobar {
            String name;
            long barId;

            @ToOne(joinProperty = "barId")
            Bar bar;
        }
        """)!!
        assertThat(entity.oneRelations, equalTo(
                listOf(OneRelation(Variable(BarType, "bar"), foreignKeyField = "barId"))
        ))
    }

    @Test
    fun toOneWithoutProperty() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Column;
        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.ToOne;
        import org.jetbrains.annotations.NotNull;

        @Entity
        class Foobar {
            String name;

            @ToOne
            Bar bar;
        }
        """)!!
        assertThat(entity.oneRelations, equalTo(
                listOf(OneRelation(Variable(BarType, "bar")))
        ))
    }

    @Test
    fun toOneWithoutPropertyMore() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Column;
        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.Property;import org.greenrobot.greendao.annotation.ToOne;
        import org.greenrobot.greendao.annotation.Unique;
        import org.jetbrains.annotations.NotNull;

        @Entity
        class Foobar {
            String name;

            @ToOne
            @Unique
            @NotNull
            @Property(nameInDb = "BAR_ID")
            Bar bar;
        }
        """)!!
        assertThat(entity.oneRelations, equalTo(
                listOf(OneRelation(Variable(BarType, "bar"), columnName = "BAR_ID", isNotNull = true, unique = true))
        ))
    }

    @Test
    fun toMany() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.ToMany;

        import java.util.List;

        @Entity
        class Foobar {
            String name;

            @ToMany(referencedJoinProperty = "barId")
            List<Bar> bars;
        }
        """)!!
        assertThat(entity.manyRelations, equalTo(
                listOf(ManyRelation(Variable(BarListType, "bars"), mappedBy = "barId"))
        ))
    }

    @Test
    fun toManyWithMulticolumnJoin() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.JoinOn;
        import org.greenrobot.greendao.annotation.JoinProperty;import org.greenrobot.greendao.annotation.ToMany;

        import java.util.List;

        @Entity
        class Foobar {
            String name;
            long barId;
            long barSubId;

            @ToMany(joinProperties = {
                @JoinProperty(name = "barId", referencedName = "id"),
                @JoinProperty(name = "barSubId", referencedName = "subId")
            })
            List<Bar> bars;
        }
        """)!!
        assertThat(entity.manyRelations, equalTo(
                listOf(ManyRelation(Variable(BarListType, "bars"), joinOnProperties = listOf(
                        JoinOnProperty("barId", "id"),
                        JoinOnProperty("barSubId", "subId")
                )))
        ))
    }

    @Test
    fun toManyWithJoinEntity() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.JoinEntity;
        import org.greenrobot.greendao.annotation.ToMany;

        import java.util.List;

        @Entity
        class Foo {
            String name;

            @ToMany
            @JoinEntity(entity = Foobar.class, sourceProperty = "fooId", targetProperty = "barId")
            List<Bar> bars;
        }
        """)!!
        assertThat(entity.manyRelations, equalTo(
                listOf(ManyRelation(Variable(BarListType, "bars"), joinEntitySpec =
                JoinEntitySpec("com.example.Foobar", "fooId", "barId")
                ))
        ))
    }

    @Test
    fun toManyOrderBy() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.OrderBy;
        import org.greenrobot.greendao.annotation.ToMany;

        import java.util.List;

        @Entity
        class Foo {
            String name;
            long barId;

            @ToMany(referencedJoinProperty = "barId")
            @OrderBy("date, likes DESC")
            List<Bar> bars;
        }
        """)!!
        assertThat(entity.manyRelations, equalTo(
                listOf(ManyRelation(Variable(BarListType, "bars"), mappedBy = "barId",
                        order = listOf(OrderProperty("date", Order.ASC), OrderProperty("likes", Order.DESC))
                ))
        ))
    }

    @Test(expected = RuntimeException::class)
    fun ambigousImport() {
        visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.ToOne;

        import net.yetanotherlib.*;

        @Entity
        class Foo {
            long barId;

            @ToOne(mappedBy = "barId")
            Bar bars;
        }
        """)
    }

    @Test
    fun resolveQualifiedNameInSamePackage() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.ToOne;

        import net.yetanotherlib.*;

        @Entity
        class Foo {
            long barId;

            @ToOne(mappedBy = "barId")
            Bar bars;
        }
        """, listOf("Bar"))!!

        assertEquals(BarType, entity.oneRelations[0].variable.type)
    }

    @Test
    fun resolveInternalClassInSamePackage() {
        val entity = visit(
                //language=java
                """
        package com.example;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.ToOne;

        import net.yetanotherlib.*;

        @Entity
        class Foo {
            long barId;

            @ToOne(mappedBy = "barId")
            Bar.Item bars;
        }
        """, listOf("Bar"))!!

        assertEquals(BarItemType, entity.oneRelations[0].variable.type)
    }

    @Test
    fun resolveFullyQualifiedNameIternalPackage() {
        val entity = visit(
                //language=java
                """
        package com.example2;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.ToOne;

        import net.yetanotherlib.*;

        @Entity
        class Foo {
            long barId;

            @ToOne(mappedBy = "barId")
            com.example.Bar.Item bars;
        }
        """)!!

        val fullBarItemType = VariableType("com.example.Bar.Item", false, "com.example.Bar.Item")
        assertEquals(fullBarItemType, entity.oneRelations[0].variable.type)
    }

    @Test
    fun resolveFullyQualifiedName() {
        val entity = visit(
                //language=java
                """
        package com.example2;

        import org.greenrobot.greendao.annotation.Entity;
        import org.greenrobot.greendao.annotation.ToOne;

        import net.yetanotherlib.*;

        @Entity
        class Foo {
            long barId;

            @ToOne(mappedBy = "barId")
            com.example.Bar bars;
        }
        """)!!

        val fullBarType = VariableType("com.example.Bar", false, "com.example.Bar")
        assertEquals(fullBarType, entity.oneRelations[0].variable.type)
    }

}