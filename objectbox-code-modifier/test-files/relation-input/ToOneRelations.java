package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;
import io.objectbox.relation.ToOne;

import java.util.List;

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
    ToOne<Customer> customer4;

    ToOne<Customer> customer5;

    long customerCustomId6;
    @Relation(idProperty = "customerCustomId6")
    ToOne<Customer> customer6;

    ToOne<Customer> customer7 = new ToOne<>(this, null);

}
