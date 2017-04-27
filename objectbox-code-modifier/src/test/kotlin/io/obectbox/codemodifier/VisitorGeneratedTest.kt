package io.obectbox.codemodifier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.objectbox.codemodifier.CodeCompare
import io.objectbox.codemodifier.GeneratorHint
import org.junit.Assert
import org.junit.Test

/**
 * Tests if @Generated and @Keep annotations are properly recognized and if generated code modifications are detected.
 */
class VisitorGeneratedTest : VisitorTestBase() {

    @Test
    fun fieldGeneratorHint() {
        val cityHash = CodeCompare.codeHash("@Transient String city;")
        val entity = visit(
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @Generated
            transient String name;

            @Keep
            transient int age;

            transient String surname;

            @Generated($cityHash)
            @Transient
            String city;
        }
        """.trimIndent())!!
        assertThat(entity.transientFields.map { it.hint }, equalTo(
                listOf(GeneratorHint.Generated(-1), GeneratorHint.Keep, null, GeneratorHint.Generated(cityHash))
        ))
    }

    @Test
    fun constructorGeneratorHint() {
        val constructorHash = CodeCompare.codeHash(
                "Foobar(String name, int age){}"
        )

        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            String name;
            int age;

            @Generated
            Foobar(String name) {
            }

            @Generated($constructorHash)
            Foobar(String name, int age) {
            }

            @Keep
            Foobar(int age) {
            }

            Foobar() {
            }
        }
        """)!!
        Assert.assertEquals(GeneratorHint.Generated(-1), entity.constructors[0].hint)
        Assert.assertEquals(GeneratorHint.Generated(constructorHash), entity.constructors[1].hint)
        Assert.assertEquals(GeneratorHint.Keep, entity.constructors[2].hint)
        Assert.assertNull(entity.constructors[3].hint)
    }

    @Test
    fun methodGeneratorHint() {
        val entity = visit(
                //language=java
                """
        import io.objectbox.annotation.*;

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
        Assert.assertEquals(entity.methods[0].hint, GeneratorHint.Generated(-1))
        Assert.assertEquals(entity.methods[1].hint, GeneratorHint.Keep)
        Assert.assertNull(entity.methods[2].hint)
    }

    @Test(expected = RuntimeException::class)
    fun throwIfGeneratedFieldChanged() {
        visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @Generated(10)
            transient String name;
        }
        """)!!
    }

    @Test(expected = RuntimeException::class)
    fun throwIfGeneratedConstructorChanged() {
        visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @Generated(10)
            Foobar() {
                // body
            }
        }
        """)!!
    }

    @Test(expected = RuntimeException::class)
    fun throwIfGeneratedMethodChanged() {
        visit(
                //language=java
                """
        import io.objectbox.annotation.*;

        @Entity
        class Foobar {
            @Generated(10)
            void hello() {
                // body
            }
        }
        """)!!
    }

}
