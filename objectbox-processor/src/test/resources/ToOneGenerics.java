package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class ToOneGenerics<T> {

    @Id
    public long id;

    public ToOne<T> toOne = new ToOne<>(this, ToOneGenerics_.toOne);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
