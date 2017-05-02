package io.objectbox;

import io.objectbox.test.entityannotation.Order;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MockableTest {
    @Test
    public void testMocks() {
        BoxStore store = Mockito.mock(BoxStore.class);
        assertNull(store.boxFor(Order.class));

        Box<Order> box = Mockito.mock(Box.class);
        assertEquals(0, box.getAll().size());
    }
}
