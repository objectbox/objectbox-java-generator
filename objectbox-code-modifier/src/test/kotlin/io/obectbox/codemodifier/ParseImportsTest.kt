package io.obectbox.codemodifier

import io.objectbox.codemodifier.VariableType
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

/**
 * Tests if types are properly recognized.
 */
class ParseImportsTest : ParseTestBase() {

    @Test(expected = RuntimeException::class)
    fun ambigousImport() {
        parse(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.ToOne;

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
    @Ignore("Feature not yet available")
    fun resolveQualifiedNameInSamePackage() {
        val entity = parse(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.ToOne;

        import net.yetanotherlib.*;

        @Entity
        class Foo {
            long barId;

            @ToOne(mappedBy = "barId")
            Bar bars;
        }
        """, listOf("Bar"))!!

        Assert.assertEquals(BarType, entity.toOneRelations[0].variable.type)
    }

    @Test
    @Ignore("Feature not yet available")
    fun resolveInternalClassInSamePackage() {
        val entity = parse(
                //language=java
                """
        package com.example;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.ToOne;

        import net.yetanotherlib.*;

        @Entity
        class Foo {
            long barId;

            @ToOne(mappedBy = "barId")
            Bar.Item bars;
        }
        """, listOf("Bar"))!!

        Assert.assertEquals(BarItemType, entity.toOneRelations[0].variable.type)
    }

    @Test
    @Ignore("Feature not yet available")
    fun resolveFullyQualifiedNameIternalPackage() {
        val entity = parse(
                //language=java
                """
        package com.example2;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.ToOne;

        import net.yetanotherlib.*;

        @Entity
        class Foo {
            long barId;

            @ToOne(mappedBy = "barId")
            com.example.Bar.Item bars;
        }
        """)!!

        val fullBarItemType = VariableType("com.example.Bar.Item", false, "com.example.Bar.Item")
        Assert.assertEquals(fullBarItemType, entity.toOneRelations[0].variable.type)
    }

    @Test
    @Ignore("Feature not yet available")
    fun resolveFullyQualifiedName() {
        val entity = parse(
                //language=java
                """
        package com.example2;

        import io.objectbox.annotation.Entity;
        import io.objectbox.annotation.ToOne;

        import net.yetanotherlib.*;

        @Entity
        class Foo {
            long barId;

            @ToOne(mappedBy = "barId")
            com.example.Bar bars;
        }
        """)!!

        val fullBarType = VariableType("com.example.Bar", false, "com.example.Bar")
        Assert.assertEquals(fullBarType, entity.toOneRelations[0].variable.type)
    }

}
