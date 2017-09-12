package io.objectbox.test.entityannotation;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.NameInDb;
import io.objectbox.relation.ToOne;

@Entity
@NameInDb("ORDERS")
public class Order {

    @Id(assignable = true)
    long id;
    java.util.Date date;
    long customerId;
    private String text;

    private ToOne<Customer> customer;

    private ToOne<Customer> customerWithoutIdProperty;

    ToOne<Customer> customerToOneWithoutIdProperty;

    public Order() {
    }

    public Order(Long id) {
        this.id = id;
    }

    public Order(long id, java.util.Date date, long customerId, String text,
            long customerWithoutIdPropertyId, long customerToOneWithoutIdPropertyId) {
        this.id = id;
        this.date = date;
        this.customerId = customerId;
        this.text = text;
        this.customerWithoutIdProperty.setTargetId(customerWithoutIdPropertyId);
        this.customerToOneWithoutIdProperty.setTargetId(customerToOneWithoutIdPropertyId);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ToOne<Customer> getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer.setTarget(customer);
    }

    public ToOne<Customer> getCustomerWithoutIdProperty() {
        return customerWithoutIdProperty;
    }

    public void setCustomerWithoutIdProperty(Customer customerWithoutIdProperty) {
        this.customerWithoutIdProperty.setTarget(customerWithoutIdProperty);
    }
    
}
