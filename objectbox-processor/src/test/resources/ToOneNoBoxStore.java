package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class ToOneNoBoxStore {

    @Id
    Long id;

    ToOne<ToOneParent> toOne = new ToOne<>(this, ToOneNoBoxStore_.toOne);

}
