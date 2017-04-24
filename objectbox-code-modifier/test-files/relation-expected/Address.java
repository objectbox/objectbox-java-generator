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

    /** @Depreacted Used to resolve relations */
    @Internal
    @Generated(hash = 975972993)
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
    @Generated(hash = 969081769)
    transient ToOne<Customer> customer__toOne = new ToOne<>(this, Address_._Relations.customer);

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
