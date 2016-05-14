package org.greenrobot.greendao.codemodifier

import org.junit.Assert.*
import org.junit.Test

class FormattingTest {
    @Test
    fun detect4Spaces() {
        val formatting = Formatting.detect(
//language=java
"""
/**
 *
 * Long jdoc
 *
 */
class Foobar {
    /**
     * Long
     * jdoc
     */
    int age;
}
"""
        )
        assertEquals(Tabulation(' ', 4), formatting.tabulation)
    }

    @Test
    fun detect2Spaces() {
        val formatting = Formatting.detect(
            //language=java
            """
/**
 *
 * Long jdoc
 *
 */
class Foobar {
  int age;

  void run() {
    age++;
  }
}
"""
        )
        assertEquals(Tabulation(' ', 2), formatting.tabulation)
    }

    @Test
    fun detectTab() {
        val formatting = Formatting.detect(
            """
/**
 *
 * Long jdoc
 *
 */
class Foobar {
\tint age;
\tvoid run() {
\t\tage++;
\t}
}
"""
        )
        assertEquals(Tabulation('\t', 1), formatting.tabulation)
    }
}
