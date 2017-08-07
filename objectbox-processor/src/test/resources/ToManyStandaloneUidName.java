package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.NameInDb;
import io.objectbox.annotation.Uid;
import io.objectbox.relation.ToMany;

@Entity
public class ToManyStandaloneUidName {

    @Id
    Long id;

    @Uid(420000000L)
    @NameInDb("Hoolaloop")
    ToMany<IdEntity> children = new ToMany<>(this, ToManyStandaloneUidName_.children);

}
