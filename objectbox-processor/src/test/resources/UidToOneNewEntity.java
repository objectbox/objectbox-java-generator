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
public class UidToOneNewEntity {

    @Id
    Long id;

    @Uid(3843553193211826785L)
    ToOne<UidToOneNewEntity> toOne;

    @Backlink(to = "toOne")
    ToMany<UidToOneNewEntity> toManyBacklink;

    ToMany<UidToOneNewEntity> toManyStandalone;

}
