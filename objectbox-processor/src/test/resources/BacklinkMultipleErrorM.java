package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkMultipleErrorM {

    @Id long id;

    @Backlink
    ToMany<BacklinkMultipleErrorMRelation> backlink1 = new ToMany<>(this, BacklinkMultipleErrorM_.backlink1);

    @Backlink
    ToMany<BacklinkMultipleErrorMRelation> backlink2 = new ToMany<>(this, BacklinkMultipleErrorM_.backlink2);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
