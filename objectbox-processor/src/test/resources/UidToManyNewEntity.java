package io.objectbox.processor.test;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.NameInDb;
import io.objectbox.annotation.Uid;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;

@Entity
@NameInDb("UidRelationNewEntity")
public class UidToManyNewEntity {

    @Id
    Long id;

    ToOne<UidToManyNewEntity> toOne;

    @Backlink(to = "toOne")
    ToMany<UidToManyNewEntity> toManyBacklink;

    @Uid(3843553193211826785L)
    ToMany<UidToManyNewEntity> toManyStandalone;

}
