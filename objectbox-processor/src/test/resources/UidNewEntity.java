package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.NameInDb;
import io.objectbox.annotation.Uid;

@Entity
@Uid(4556510748917406915L)
@NameInDb("UidEntity") // just to re-use json model file
public class UidNewEntity {

    @Id
    Long id;

    String uidProperty;

}
