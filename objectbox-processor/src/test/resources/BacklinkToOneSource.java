package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class BacklinkToOneSource {

    @Id
    Long id;

    ToOne<BacklinkToOneTarget> target = new ToOne<>(this, BacklinkToOneSource_.target);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
