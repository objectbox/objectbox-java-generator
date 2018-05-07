package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkMultipleMMSource {

    @Id
    Long id;

    ToMany<BacklinkMultipleMMTarget> targetsA = new ToMany<>(this, BacklinkMultipleMMSource_.targetsA);

    ToMany<BacklinkMultipleMMTarget> targetsB = new ToMany<>(this, BacklinkMultipleMMSource_.targetsB);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
