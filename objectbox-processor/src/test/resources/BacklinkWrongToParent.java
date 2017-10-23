package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkWrongToParent {

    @Id
    Long id;

    @Backlink(to = "wrongParent")
    ToMany<BacklinkWrongToChild> children = new ToMany<>(this, BacklinkWrongToParent_.children);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
