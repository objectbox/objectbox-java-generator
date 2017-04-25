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

    /** @Depreacted Used to resolve relations */
    @Internal
    @Generated(hash = 975972993)
    transient BoxStore __boxStore;

    @Generated(hash = 1440476873)
    public OtherPackage(long id, long customerId) {
        this.id = id;
        this.customerId = customerId;
    }

    @Generated(hash = 1677191657)
    public OtherPackage() {
    }

    @Internal
    @Generated(hash = 519009093)
    transient ToOne<Customer> customerToOne = new ToOne<>(this, OtherPackage_.customer);

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

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 1958088637)
    public List<Order> getOrders() {
        if (orders == null) {
            final BoxStore boxStore = this.__boxStore;
            if (boxStore == null) {
                throw new DbDetachedException();
            }
            Box<Order> box = boxStore.boxFor(Order.class);
            int targetTypeId = boxStore.getEntityTypeIdOrThrow(Order.class);
            List<Order> ordersNew = box.getBacklinkEntities(targetTypeId, Order_.customerId, id);
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

}
