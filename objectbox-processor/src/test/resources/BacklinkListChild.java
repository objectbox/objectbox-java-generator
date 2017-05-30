package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class BacklinkListChild {

    @Id long id;

    ToOne<BacklinkListParent> parent = new ToOne<>(this, BacklinkListChild_.parent);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
