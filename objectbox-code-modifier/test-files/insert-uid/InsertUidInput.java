package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Property;
import io.objectbox.annotation.Uid;

@Entity
@Uid
public class Note {

    @Id
    private Long id;

    @Uid
    private String insert;

    @Uid(1406015203155591783L)
    private String doNotInsert;

    private String control;

}
