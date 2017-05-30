package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;
import io.objectbox.relation.ToOne;

@Entity
public class RelationChild {

    @Id long id;

    long parentId;

    @Relation
    RelationParent parent;

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;
    transient ToOne<RelationParent> parentToOne = new ToOne<>(this, RelationChild_.parent);

}
