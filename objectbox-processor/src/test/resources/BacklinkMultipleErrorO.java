package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkMultipleErrorO {

    @Id long id;

    @Backlink
    ToMany<BacklinkMultipleErrorORelation> backlink1 = new ToMany<>(this, BacklinkMultipleErrorO_.backlink1);

    @Backlink
    ToMany<BacklinkMultipleErrorORelation> backlink2 = new ToMany<>(this, BacklinkMultipleErrorO_.backlink2);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
