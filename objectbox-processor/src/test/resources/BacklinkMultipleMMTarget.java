package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkMultipleMMTarget {

    @Id
    Long id;

    @Backlink
    ToMany<BacklinkMultipleMMSource> sources = new ToMany<>(this, BacklinkMultipleMMTarget_.sources);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
