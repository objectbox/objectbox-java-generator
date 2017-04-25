package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.Property;
import io.objectbox.annotation.Uid;

@Entity
@Uid(-1)
public class Note {

    @Id
    private Long id;

    @Index
    @Uid
    private String control;

}
