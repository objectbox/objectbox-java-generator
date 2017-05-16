package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class RelatedEntity {

    @Id(assignable = true)
    long id;
}
