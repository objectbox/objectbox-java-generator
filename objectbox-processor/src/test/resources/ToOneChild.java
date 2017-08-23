package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.TargetIdProperty;
import io.objectbox.relation.ToOne;

@Entity
public class ToOneChild {

    @Id
    Long id;

    ToOne<ToOneParent> parent = new ToOne<>(this, ToOneChild_.parent);

    long aParentId;

    @TargetIdProperty("aParentId")
    ToOne<ToOneParent> parentWithIdProperty = new ToOne<>(this, ToOneChild_.parentWithIdProperty);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
