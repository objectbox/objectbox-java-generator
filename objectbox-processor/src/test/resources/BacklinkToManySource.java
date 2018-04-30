package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkToManySource {

    @Id
    Long id;

    ToMany<BacklinkToManyTarget> targets = new ToMany<>(this, BacklinkToManySource_.targets);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
