package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class RelationChild {

    @Id
    Long id;

    long parentId;

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

    ToOne<RelationParent> parent = new ToOne<>(this, RelationChild_.parent);

}
