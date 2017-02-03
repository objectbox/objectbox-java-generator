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
public class Address {

    @Id
    long id;

    @Relation
    Customer customer;

    long customerId;

    /** Used to resolve relations */
    @Internal
    @Generated(hash = 1307364262)
    transient BoxStore __boxStore;

    @Generated(hash = 1157172385)
    public Address(long id, long customerId) {
        this.id = id;
        this.customerId = customerId;
    }

    @Generated(hash = 388317431)
    public Address() {
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
    @Generated(hash = 255983826)
    private transient ToOne<Address, Customer> customer__toOne;

    /** See {@link io.objectbox.relation.ToOne} for details. */
    @Generated(hash = 1338197451)
    public synchronized ToOne<Address, Customer> getCustomer__toOne() {
        if (customer__toOne == null) {
            customer__toOne = new ToOne<>(this, Address_.customerId, Customer.class);
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
    @Generated(hash = 1389894795)
    public void remove() {
        if (__boxStore == null) {
            throw new DbDetachedException();
        }
        __boxStore.boxFor(Address.class).remove(this);
    }

    /**
     * Puts the entity in its object box.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 490623117)
    public void put() {
        if (__boxStore == null) {
            throw new DbDetachedException();
        }
        __boxStore.boxFor(Address.class).put(this);
    }

}
