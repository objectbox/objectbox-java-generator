package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkMissingParent {

    @Id
    Long id;

    ToMany<IdEntity> children = new ToMany<>(this, BacklinkMissingParent_.children);

}
