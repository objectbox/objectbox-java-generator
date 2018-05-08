package io.objectbox.processor.test;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.NameInDb;
import io.objectbox.annotation.Uid;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;

@Entity
@Uid(2361091532752425885L)
@NameInDb("UidRelationEmptyEntity")
public class UidToOneEmptyEntity {

    @Id
    Long id;

    @Uid
    ToOne<UidToOneEmptyEntity> toOne;

    @Backlink(to = "toOne")
    ToMany<UidToOneEmptyEntity> toManyBacklink;

    ToMany<UidToOneEmptyEntity> toManyStandalone;

}
