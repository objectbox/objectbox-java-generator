package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;

import java.util.List;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.exception.DbException;
import io.objectbox.relation.ToOne;

@Entity
public class Order {

    @Id
    long id;

    long customerId;

    @Relation
    Customer customer;

    /** Used to resolve relations */
    @Internal
    @Generated(hash = 1307364262)
    transient BoxStore __boxStore;

    @Generated(hash = 789963847)
    public Order(long id, long customerId) {
        this.id = id;
        this.customerId = customerId;
    }

    @Generated(hash = 1105174599)
    public Order() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    @Internal
    @Generated(hash = 489682569)
    private transient ToOne<Order, Customer> customer__toOne;

    /** See {@link io.objectbox.relation.ToOne} for details. */
    @Generated(hash = 41876359)
    public synchronized ToOne<Order, Customer> getCustomer__toOne() {
        if (customer__toOne == null) {
            customer__toOne = new ToOne<>(this, Order_.customerId, Customer.class);
        }
        return customer__toOne;
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

    /**
     * Removes entity from its object box. Entity must attached to an entity context.
     */
    @Generated(hash = 100425113)
    public void remove() {
        if (__boxStore == null) {
            throw new DbDetachedException();
        }
        __boxStore.boxFor(Order.class).remove(this);
    }

    /**
     * Puts the entity in its object box.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 2065119853)
    public void put() {
        if (__boxStore == null) {
            throw new DbDetachedException();
        }
        __boxStore.boxFor(Order.class).put(this);
    }

}
