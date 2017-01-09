package org.greenrobot.greendao.codemodifier

import org.junit.Test

class VisitorIgnoredProblemsTest : VisitorTestBase() {

    @Test
    fun comparable() {
        visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.Entity;

        @Entity
        class Foobar implements Comparable<SomeType> {

                @Override
                public int compareTo(SomeType another) {
                    return 0;
                }

        }
        """
        )
    }

}
