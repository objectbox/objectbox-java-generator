package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class BacklinkToOneError {

    @Id
    Long id;

    @Backlink
    ToOne<BacklinkToOneError> nonsensicalToOne = new ToOne<>(this, BacklinkToOneError_.nonsensicalToOne);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
