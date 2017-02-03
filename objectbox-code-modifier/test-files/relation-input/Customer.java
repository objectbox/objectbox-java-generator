package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;

import java.util.List;

@Entity
public class Customer {

    @Id
    long id;

    String name;

    @Relation(idProperty = "customerId")
    List<Order> orders;
}
