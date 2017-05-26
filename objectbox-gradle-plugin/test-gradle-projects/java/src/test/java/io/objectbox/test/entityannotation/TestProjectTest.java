package io.objectbox.test.entityannotation;

import io.objectbox.BoxStoreBuilder;
import io.objectbox.EntityInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Tests some basic functionality without touching BoxStore (which would require native libs). */
public class TestProjectTest {

    @Test
    public void testBuilder() {
        BoxStoreBuilder builder = MyObjectBox.builder();
        assertNotNull(builder);
    }

    @Test
    public void testProperties() {
        EntityInfo[] propertiesArray = {new Customer_(), new Order_(), new TypesInInnerClass_()};
        for (EntityInfo properties : propertiesArray) {
            assertTrue(properties.getAllProperties().length >= 2);
            assertEquals(1, properties.getIdProperty().id);
        }
    }

    @Test
    public void testIdGetter() {
        Customer customer = new Customer();
        customer.setId(42);
        assertEquals(42, Customer_.__ID_GETTER.getId(customer));
    }

    @Test
    public void testIdGetterNull() {
        TypesInInnerClass nullableIdEntity = new TypesInInnerClass();
        nullableIdEntity.setId(null);
        assertEquals(0, TypesInInnerClass_.__ID_GETTER.getId(nullableIdEntity));
    }

}
