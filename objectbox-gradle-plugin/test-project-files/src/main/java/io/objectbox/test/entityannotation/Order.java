package io.objectbox.test.entityannotation;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.relation.ToOne;
import io.objectbox.exception.DbException;

/**
 * Entity mapped to table "ORDERS".
 */
@Entity(nameInDb = "ORDERS")
public class Order {

    @Id(assignable = true)
    long id;
    java.util.Date date;
    long customerId;
    private String text;

    @Relation
    private Customer customer;

    /** @Depreacted Used to resolve relations */
    @Internal
    @Generated(hash = 975972993)
    transient BoxStore __boxStore;
    @Internal
    @Generated(hash = 35245611)
    private transient ToOne<Customer> customer__toOne;

    @Generated(hash = 1105174599)
    public Order() {
    }

    public Order(Long id) {
        this.id = id;
    }

    @Generated(hash = 10986505)
    public Order(long id, java.util.Date date, long customerId, String text) {
        this.id = id;
        this.date = date;
        this.customerId = customerId;
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public java.util.Date getDate() {
        return date;
    }

    public void setDate(java.util.Date date) {
        this.date = date;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Customer peekCustomer() {
        return customer;
    }

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 424372732)
    public Customer getCustomer() {
        customer = getCustomer__toOne().getTarget(this.customerId);
        return customer;
    }

    /** Set the to-one relation including its ID property. */
    @Generated(hash = 410684144)
    public void setCustomer(Customer customer) {
        getCustomer__toOne().setTarget(customer);
        this.customer = customer;
    }

    /** See {@link io.objectbox.relation.ToOne} for details. */
    @Generated(hash = 310001897)
    public synchronized ToOne<Customer> getCustomer__toOne() {
        if (customer__toOne == null) {
            customer__toOne = new ToOne<>(this, Order_.customerId, Customer.class);
        }
        return customer__toOne;
    }

}