package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkToManyParentEntity {

    @Id long id;

    @Backlink
    ToMany<BacklinkToManyChildEntity> children = new ToMany<>(this, BacklinkToManyParentEntity_.children);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
