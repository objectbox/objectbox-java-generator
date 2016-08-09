package org.greenrobot.greendao.codemodifier

import org.junit.Assert
import org.junit.Test

/**
 * Tests if types are properly recognized.
 */
class VisitorImportsTest : VisitorTestBase() {

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

        Assert.assertEquals(BarType, entity.oneRelations[0].variable.type)
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

        Assert.assertEquals(BarItemType, entity.oneRelations[0].variable.type)
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
        Assert.assertEquals(fullBarItemType, entity.oneRelations[0].variable.type)
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
        Assert.assertEquals(fullBarType, entity.oneRelations[0].variable.type)
    }

}
