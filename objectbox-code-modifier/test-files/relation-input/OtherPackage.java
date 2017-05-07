package io.objectbox.codemodifier.test.other;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Relation;
import java.util.List;

import io.objectbox.codemodifier.test.Customer;
import io.objectbox.codemodifier.test.Order;

@Entity
public class OtherPackage {

    @Id
    long id;

    long customerId;

    @Relation
    Customer customer;

    @Backlink(to = "other")
    List<OnePackage> ones;

}
