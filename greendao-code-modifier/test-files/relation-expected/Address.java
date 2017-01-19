package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;

import java.util.List;
import io.objectbox.annotation.Internal;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.exception.DbException;

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
    @Generated(hash = 1698848862)
    private transient Long customer__resolvedKey;

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 910495430)
    public Customer getCustomer() {
        long __key = this.customerId;
        if (customer__resolvedKey == null || customer__resolvedKey != __key) {
            final BoxStore boxStore = this.__boxStore;
            if (boxStore == null) {
                throw new DbDetachedException();
            }
            Box<Customer> box = boxStore.boxFor(Customer.class);
            Customer customerNew = box.get(__key);
            synchronized (this) {
                customer = customerNew;
                customer__resolvedKey = __key;
            }
        }
        return customer;
    }

    /** Set the to-one relation including its ID property. */
    @Generated(hash = 1322376583)
    public void setCustomer(Customer customer) {
        synchronized (this) {
            this.customer = customer;
            customerId = customer == null ? 0 : customer.getId();
            customer__resolvedKey = customerId;
        }
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
