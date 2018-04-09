package io.objectbox.processor.test;

import java.util.List;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkToOneListTarget {

    @Id
    Long id;

    @Backlink
    List<BacklinkToOneListSource> sources = new ToMany<>(this, BacklinkToOneListTarget_.sources);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
