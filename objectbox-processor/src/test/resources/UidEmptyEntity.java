package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Uid;

@Entity
@Uid
public class UidEmptyEntity {

    @Id
    Long id;

    @Uid
    String uidProperty;

}
