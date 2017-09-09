package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Uid;
import io.objectbox.relation.ToOne;

@Entity
@Uid(2361091532752425885L)
public class UidRelationEmptyEntity {

    @Id
    Long id;

    @Uid
    ToOne<UidRelationEmptyEntity> toOne;

}
