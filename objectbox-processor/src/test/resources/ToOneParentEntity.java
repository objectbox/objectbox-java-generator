package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class ToOneParentEntity {

    @Id long id;

    ToOne<ToOneChildEntity> child = new ToOne<>(this, ToOneParentEntity_.child);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
