package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;

import java.util.List;
import io.objectbox.annotation.Generated;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.exception.DbException;

@Entity
public class Customer {

    @Id
    long id;

    String name;

    @Relation(idProperty = "customerId")
    List<Order> orders;

    /** Used to resolve relations */
    @Generated(hash = 506680373)
    private transient BoxStore __boxStore;

    /** Used for active entity operations. */
    @Generated(hash = 1448445044)
    private transient Box<Customer> __myBox;

    @Generated(hash = 1039711609)
    public Customer(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Generated(hash = 60841032)
    public Customer() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 516183999)
    public List<Order> getOrders() {
        if (orders == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DbDetachedException();
            }
            OrderCursor targetDao = daoSession.getOrderCursor();
            List<Order> ordersNew = targetDao._queryCustomer_Orders(id);
            synchronized (this) {
                if (orders == null) {
                    orders = ordersNew;
                }
            }
        }
        return orders;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 1446109810)
    public synchronized void resetOrders() {
        orders = null;
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
