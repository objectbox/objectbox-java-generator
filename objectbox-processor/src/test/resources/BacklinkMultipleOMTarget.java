package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkMultipleOMTarget {

    @Id
    Long id;

    @Backlink
    ToMany<BacklinkMultipleOMSource> sources = new ToMany<>(this, BacklinkMultipleOMTarget_.sources);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
