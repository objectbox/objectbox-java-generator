package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkToManyParent {

    @Id
    Long id;

    @Backlink
    ToMany<BacklinkToManyChild> children = new ToMany<>(this, BacklinkToManyParent_.children);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
