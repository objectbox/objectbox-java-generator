package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;
import io.objectbox.relation.ToOne;

import java.util.List;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.BoxStore;

@Entity
public class ToOneRelations {

    @Id
    long id;

    long customer1Id;

    @Relation
    Customer customer1;

    @Relation
    Customer customer2;

    long customerCustomId3;
    @Relation(idProperty = "customerCustomId3")
    Customer customer3;

    long customer4Id;
    ToOne<Customer> customer4 = new ToOne<>(this, ToOneRelations_.customer4);

    ToOne<Customer> customer5 = new ToOne<>(this, ToOneRelations_.customer5);

    long customerCustomId6;
    @Relation(idProperty = "customerCustomId6")
    ToOne<Customer> customer6 = new ToOne<>(this, ToOneRelations_.customer6);

    ToOne<Customer> customer7 = new ToOne<>(this, null);

    /** Used to resolve relations */
    @Internal
    @Generated(1307364262)
    transient BoxStore __boxStore;

    @Generated(104792396)
    @Internal
    /** This constructor was generated by ObjectBox and may change any time. */
    public ToOneRelations(long id, long customer1Id, long customerCustomId3, long customer4Id, long customerCustomId6,
            long customer2Id, long customer5Id, long customer7Id) {
        this.id = id;
        this.customer1Id = customer1Id;
        this.customerCustomId3 = customerCustomId3;
        this.customer4Id = customer4Id;
        this.customerCustomId6 = customerCustomId6;
        this.customer2ToOne.setTargetId(customer2Id);
        this.customer5.setTargetId(customer5Id);
        this.customer7.setTargetId(customer7Id);
    }

    @Generated(709257305)
    public ToOneRelations() {
    }

    @Internal
    @Generated(2061582599)
    transient ToOne<Customer> customer1ToOne = new ToOne<>(this, ToOneRelations_.customer1);

    @Internal
    @Generated(601683709)
    transient ToOne<Customer> customer2ToOne = new ToOne<>(this, ToOneRelations_.customer2);

    @Internal
    @Generated(239006051)
    transient ToOne<Customer> customer3ToOne = new ToOne<>(this, ToOneRelations_.customer3);

    /** To-one relationship, resolved on first access. */
    @Generated(547953048)
    public Customer getCustomer1() {
        customer1 = customer1ToOne.getTarget(this.customer1Id);
        return customer1;
    }

    /** Set the to-one relation including its ID property. */
    @Generated(784267751)
    public void setCustomer1(Customer customer1) {
        customer1ToOne.setTarget(customer1);
        this.customer1 = customer1;
    }

    /** To-one relationship, resolved on first access. */
    @Generated(1437778746)
    public Customer getCustomer2() {
        customer2 = customer2ToOne.getTarget(this.customer2Id);
        return customer2;
    }

    /** Set the to-one relation including its ID property. */
    @Generated(1488668235)
    public void setCustomer2(Customer customer2) {
        customer2ToOne.setTarget(customer2);
        this.customer2 = customer2;
    }

    /** To-one relationship, resolved on first access. */
    @Generated(1033235195)
    public Customer getCustomer3() {
        customer3 = customer3ToOne.getTarget(this.customerCustomId3);
        return customer3;
    }

    /** Set the to-one relation including its ID property. */
    @Generated(178707021)
    public void setCustomer3(Customer customer3) {
        customer3ToOne.setTarget(customer3);
        this.customer3 = customer3;
    }

}
