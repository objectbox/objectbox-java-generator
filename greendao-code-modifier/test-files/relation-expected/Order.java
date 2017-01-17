package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;

import java.util.List;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.exception.DbException;
import io.objectbox.annotation.NotNull;

@Entity
public class Order {

    @Id
    long id;

    long customerId;

    @Relation()
    Customer customer;

    /** Used to resolve relations */
    @Generated(hash = 506680373)
    private transient BoxStore __boxStore;

    /** Used for active entity operations. */
    @Generated(hash = 1538225439)
    private transient Box<Order> __myBox;

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

    @Generated(hash = 8592637)
    private transient Long customer__resolvedKey;

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 2043538556)
    public Customer getCustomer() {
        long __key = this.customerId;
        if (customer__resolvedKey == null || !customer__resolvedKey.equals(__key)) {
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

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 445223802)
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
    @Generated(hash = 366573901)
    public void remove() {
        if (__myBox == null) {
            throw new DbDetachedException();
        }
        __myBox.remove(this);
    }

    /**
     * Puts the entity in its object box.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1902731265)
    public void put() {
        if (__myBox == null) {
            throw new DbDetachedException();
        }
        __myBox.put(this);
    }

}
