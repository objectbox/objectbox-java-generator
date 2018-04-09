package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class BacklinkWrongToSource {

    @Id
    Long id;

    ToOne<BacklinkWrongToTarget> target = new ToOne<>(this, BacklinkWrongToSource_.target);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
