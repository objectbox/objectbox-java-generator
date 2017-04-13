package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Property;

@Entity
public class Note {

    @Id
    private Long id;

    @Uid
    private String insert;

    @Uid(21L)
    private String doNotInsert;

    private String control;

}
