package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class BacklinkWithToChild {

    @Id
    Long id;

    ToOne<BacklinkWithToParent> parentOther = new ToOne<>(this, BacklinkWithToChild_.parentOther);

    ToOne<BacklinkWithToParent> parent = new ToOne<>(this, BacklinkWithToChild_.parent);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}