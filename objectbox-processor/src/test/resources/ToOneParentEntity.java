package io.objectbox.processor.test;

import java.util.Date;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;
import io.objectbox.relation.ToOne;

@Entity
public class ToOneParentEntity {

    @Id long id;

    @Relation
    ToOneChildEntity child;

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;
    transient ToOne<ToOneChildEntity> childToOne = new ToOne<>(this, ToOneParentEntity_.child);

}
