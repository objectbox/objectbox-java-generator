package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class BacklinkMultipleOOSource {

    @Id
    Long id;

    ToOne<BacklinkMultipleOOTarget> targetA = new ToOne<>(this, BacklinkMultipleOOSource_.targetA);

    ToOne<BacklinkMultipleOOTarget> targetB = new ToOne<>(this, BacklinkMultipleOOSource_.targetB);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
