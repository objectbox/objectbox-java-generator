package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;

import java.util.List;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.relation.ToOne;

@Entity
public class Address {

    @Id
    long id;

    @Relation
    Customer customer;

    /** Used to resolve relations */
    @Internal
    @Generated(1307364262)
    transient BoxStore __boxStore;

    @Generated(2007049824)
    @Internal
    /** This constructor was generated by ObjectBox and may change any time. */
    public Address(long id, long customerId) {
        this.id = id;
        this.customerToOne.setTargetId(customerId);
    }

    @Generated(388317431)
    public Address() {
    }

    @Internal
    @Generated(1429753430)
    transient ToOne<Customer> customerToOne = new ToOne<>(this, Address_.customer);

    /** To-one relationship, resolved on first access. */
    @Generated(982764858)
    public Customer getCustomer() {
        customer = customerToOne.getTarget(this.customerId);
        return customer;
    }

    /** Set the to-one relation including its ID property. */
    @Generated(1896550034)
    public void setCustomer(Customer customer) {
        customerToOne.setTarget(customer);
        this.customer = customer;
    }

}
