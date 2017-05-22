package io.objectbox.processor.test;

import java.util.Date;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;
import io.objectbox.relation.ToOne;

@Entity
public class RelationParentEntity {

    @Id long id;

    @Relation
    RelationChildEntity child;

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;
    transient ToOne<RelationChildEntity> childToOne = new ToOne<>(this, RelationParentEntity_.child);

}
