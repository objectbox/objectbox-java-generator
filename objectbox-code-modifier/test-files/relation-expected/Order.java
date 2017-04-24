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

    /** @Depreacted Used to resolve relations */
    @Internal
    @Generated(hash = 975972993)
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
    @Generated(hash = 1031210392)
    transient ToOne<Customer> customer__toOne = new ToOne<>(this, Order_.customer);

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 97719339)
    public Customer getCustomer() {
        customer = customer__toOne.getTarget(this.customerId);
        return customer;
    }

    /** Set the to-one relation including its ID property. */
    @Generated(hash = 50954149)
    public void setCustomer(Customer customer) {
        customer__toOne.setTarget(customer);
        this.customer = customer;
    }

}
