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
    @Generated(hash = 1318389891)
    transient ToOne<Customer> customerToOne = new ToOne<>(this, Order_.customer);

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
    @Generated(hash = 982764858)
    public Customer getCustomer() {
        customer = customerToOne.getTarget(this.customerId);
        return customer;
    }

    /** Set the to-one relation including its ID property. */
    @Generated(hash = 1896550034)
    public void setCustomer(Customer customer) {
        customerToOne.setTarget(customer);
        this.customer = customer;
    }

}
