package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.IndexType;
import io.objectbox.annotation.Unique;

import java.util.Date;

@Entity
public class UniqueAndIndex {

    @Id long id;

    @Index
    long notUniqueProp;

    @Unique
    @Index
    int intProp;

    @Unique
    @Index(type = IndexType.VALUE)
    String stringProp;

}
