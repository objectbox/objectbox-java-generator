package io.objectbox.test;

import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Tests some basic functionality without touching BoxStore (which would require native libs). */
public class TestProjectTest {

    @Test
    public void testBasics() {
        assertNotNull(MyObjectBox.builder());
        assertTrue(Customer_.__ALL_PROPERTIES.length >= 2);
        assertEquals(1, Order_.__ID_PROPERTY.id);
    }

}
