package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class BacklinkWrongToChild {

    @Id
    Long id;

    ToOne<BacklinkWrongToParent> parent = new ToOne<>(this, BacklinkWrongToChild_.parent);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
