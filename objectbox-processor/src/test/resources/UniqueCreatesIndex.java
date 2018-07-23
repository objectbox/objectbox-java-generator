package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;

import java.util.Date;

@Entity
public class UniqueCreatesIndex {

    @Id long id;

    @Unique String stringProp;

}
