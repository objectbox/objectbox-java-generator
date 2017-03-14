package org.greenrobot.greendao.codemodifier

import org.junit.Test

/**
 * Problems the AST parse may have when looking at entity source code but which have no impact on the plugin working
 * correctly. See [EntityClassParser.ignoredProblemIds].
 */
class VisitorIgnoredProblemsTest : VisitorTestBase() {

    @Test
    fun comparable() {
        visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.Entity;
        import org.jetbrains.annotations.NotNull;

        @Entity
        class Foobar implements Comparable<SomeType> {

            // Comparable implementation of unresolvable type
            @Override
            public int compareTo(@NotNull SomeType another) {
                return 0;
            }

        }
        """
        )
    }

    @Test
    fun missingTypeInMethod() {
        visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.Entity;

        @Entity
        class Foobar {

            public void doSomething() {
                // unresolvable type a method refers to
                SomeType some = getSomeType();
            }

            public SomeType getSomeType() {
                return null;
            }

        }
        """
        )
    }

    @Test
    fun undefinedName() {
        visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.Entity;
        import android.text.TextUtils;

        @Entity
        class Foobar {

            public void doSomething() {
                // unresolvable external class reference, like TextUtils
                TextUtils.isEmpty("");
            }

        }
        """
        )
    }

    @Test
    fun unsupportedSuppressWarnings() {
        visit(
                //language=java
                """
        import org.greenrobot.greendao.annotation.Entity;

        // SuppressWarnings tokens supported by IntelliJ, but not Eclipse
        @SuppressWarnings("WeakerAccess")
        @Entity
        class Foobar {
        }
        """
        )
    }

}
