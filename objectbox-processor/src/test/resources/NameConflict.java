package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class NameConflict {

    @Id
    Long id;

    ToMany<Property> children = new ToMany<>(this, NameConflict_.children);

}
