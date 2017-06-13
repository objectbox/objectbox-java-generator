package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class BacklinkMultipleChild {

    @Id long id;

    ToOne<BacklinkMultipleParent> parentA = new ToOne<>(this, BacklinkMultipleChild_.parentA);

    ToOne<BacklinkMultipleParent> parentB = new ToOne<>(this, BacklinkMultipleChild_.parentB);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
