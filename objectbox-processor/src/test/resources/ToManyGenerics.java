package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class ToManyGenerics<T> {

    @Id
    public long id;

    public ToMany<T> toMany = new ToMany<>(this, ToManyGenerics_.toMany);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
