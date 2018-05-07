package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkWithToTarget {

    @Id
    Long id;

    @Backlink(to = "target")
    ToMany<BacklinkWithToSource> sources = new ToMany<>(this, BacklinkWithToTarget_.sources);

    @Backlink(to = "targetOtherId") // with "Id" postfix for the property
    ToMany<BacklinkWithToSource> sourcesOther = new ToMany<>(this, BacklinkWithToTarget_.sourcesOther);

    @Backlink(to = "targets")
    ToMany<BacklinkWithToSource> sourcesMany = new ToMany<>(this, BacklinkWithToTarget_.sourcesMany);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
