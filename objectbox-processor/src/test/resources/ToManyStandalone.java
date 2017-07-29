package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class ToManyStandalone {

    @Id
    Long id;

    ToMany<IdEntity> children = new ToMany<>(this, ToManyStandalone_.children);

}
