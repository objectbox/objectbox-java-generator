package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkMultipleErrorMRelation {

    @Id long id;

    ToMany<BacklinkMultipleErrorM> relation = new ToMany<>(this, BacklinkMultipleErrorMRelation_.relation);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
