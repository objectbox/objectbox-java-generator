package io.objectbox.processor.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Uid;

@Entity
@Uid(2361091532752425885L)
public class UidEntity {

    @Id
    Long id;

    @Uid(7287685531948841886L)
    String uidProperty;

}
