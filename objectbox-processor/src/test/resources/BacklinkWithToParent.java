package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkWithToParent {

    @Id
    Long id;

    @Backlink(to = "parent")
    ToMany<BacklinkWithToChild> children = new ToMany<>(this, BacklinkWithToParent_.children);

    @Backlink(to = "parentOtherId") // with "Id" postfix for the property
    ToMany<BacklinkWithToChild> childrenOther = new ToMany<>(this, BacklinkWithToParent_.childrenOther);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
