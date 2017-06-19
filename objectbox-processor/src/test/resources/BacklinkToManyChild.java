package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class BacklinkToManyChild {

    @Id
    Long id;

    ToOne<BacklinkToManyParent> parent = new ToOne<>(this, BacklinkToManyChild_.parent);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
