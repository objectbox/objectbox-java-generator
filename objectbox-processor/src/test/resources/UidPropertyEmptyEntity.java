package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.NameInDb;
import io.objectbox.annotation.Uid;

@Entity
@NameInDb("UidEntity") // just to re-use json model file
public class UidPropertyEmptyEntity {

    @Id
    Long id;

    @Uid
    String uidProperty;

}
