package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;

import java.util.List;

@Entity
public class Customer {

    @Id
    long id;

    String name;

    List<Order> orders;
}
