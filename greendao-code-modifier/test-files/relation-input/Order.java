package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;

import java.util.List;

@Entity
public class Order {

    @Id
    long id;

    long customerId;

    @Relation()
    Customer customer;

}
