package io.objectbox.codemodifier.test.other;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;
import java.util.List;

import io.objectbox.codemodifier.test.Customer;
import io.objectbox.codemodifier.test.Order;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbDetachedException;
import io.objectbox.exception.DbException;
import io.objectbox.codemodifier.test.Order_;
import io.objectbox.relation.ToOne;

@Entity
public class OtherPackage {

    @Id
    long id;

    long customerId;

    @Relation()
    Customer customer;

    // idProperty is totally wrong here, but OK for this test
    @Relation(idProperty="customerId")
    List<Order> orders;

    /** Used to resolve relations */
    @Internal
    @Generated(hash = 1307364262)
    transient BoxStore __boxStore;

    @Generated(hash = 1440476873)
    public OtherPackage(long id, long customerId) {
        this.id = id;
        this.customerId = customerId;
    }

    @Generated(hash = 1677191657)
    public OtherPackage() {
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
    @Generated(hash = 1021441002)
    private transient ToOne<OtherPackage, Customer> customer__toOne;

    /** See {@link io.objectbox.relation.ToOne} for details. */
    @Generated(hash = 1644448081)
    public synchronized ToOne<OtherPackage, Customer> getCustomer__toOne() {
        if (customer__toOne == null) {
            customer__toOne = new ToOne<>(this, OtherPackage_.customerId, Customer.class);
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
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 954185799)
    public List<Order> getOrders() {
        if (orders == null) {
            final BoxStore boxStore = this.__boxStore;
            if (boxStore == null) {
                throw new DbDetachedException();
            }
            Box<Order> box = boxStore.boxFor(Order.class);
            int targetEntityId = boxStore.getEntityIdOrThrow(Order.class);
            List<Order> ordersNew = box.getBacklinkEntities(targetEntityId, Order_.customerId, id);
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
    @Generated(hash = 926656567)
    public void remove() {
        if (__boxStore == null) {
            throw new DbDetachedException();
        }
        __boxStore.boxFor(OtherPackage.class).remove(this);
    }

    /**
     * Puts the entity in its object box.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 364321733)
    public void put() {
        if (__boxStore == null) {
            throw new DbDetachedException();
        }
        __boxStore.boxFor(OtherPackage.class).put(this);
    }

}
