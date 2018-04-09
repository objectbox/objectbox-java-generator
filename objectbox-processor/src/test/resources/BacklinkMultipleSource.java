package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class BacklinkMultipleSource {

    @Id
    Long id;

    ToOne<BacklinkMultipleTarget> targetA = new ToOne<>(this, BacklinkMultipleSource_.targetA);

    ToOne<BacklinkMultipleTarget> targetB = new ToOne<>(this, BacklinkMultipleSource_.targetB);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
