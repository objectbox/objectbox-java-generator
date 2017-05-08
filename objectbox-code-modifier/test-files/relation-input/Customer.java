package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Backlink;

import java.util.List;

@Entity
public class Customer {

    @Id
    long id;

    String name;

    @Backlink(to = "customerId")
    List<Order> orders;
}
