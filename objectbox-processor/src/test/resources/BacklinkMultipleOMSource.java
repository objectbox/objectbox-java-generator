package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkMultipleOMSource {

    @Id
    Long id;

    ToOne<BacklinkMultipleOMTarget> target = new ToOne<>(this, BacklinkMultipleOMSource_.target);

    ToMany<BacklinkMultipleOMTarget> targets = new ToMany<>(this, BacklinkMultipleOMSource_.targets);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
