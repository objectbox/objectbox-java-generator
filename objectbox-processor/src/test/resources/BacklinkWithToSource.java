package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkWithToSource {

    @Id
    Long id;

    ToOne<BacklinkWithToTarget> targetOther = new ToOne<>(this, BacklinkWithToSource_.targetOther);

    ToOne<BacklinkWithToTarget> target = new ToOne<>(this, BacklinkWithToSource_.target);

    ToMany<BacklinkWithToTarget> targets = new ToMany<>(this, BacklinkWithToSource_.targets);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
