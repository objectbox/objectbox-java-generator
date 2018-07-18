package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class BacklinkMultipleErrorORelation {

    @Id long id;

    ToOne<BacklinkMultipleErrorO> relation = new ToOne<>(this, BacklinkMultipleErrorORelation_.relation);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
