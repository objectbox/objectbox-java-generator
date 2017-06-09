package io.objectbox.test.kotlin

import io.objectbox.BoxStoreBuilder
import io.objectbox.EntityInfo

import org.junit.Test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

/** Tests some basic functionality without touching BoxStore (which would require native libs).  */
class TestProjectTest {

    @Test
    fun testBasics() {
        assertNotNull(MyObjectBox.builder())
        assertTrue(Customer_().allProperties.size >= 2)
        assertEquals(1, Order_().idProperty.id)
    }


}
