package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class BacklinkListChildEntity {

    @Id long id;

    ToOne<BacklinkListParentEntity> parent = new ToOne<>(this, BacklinkListChildEntity_.parent);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
