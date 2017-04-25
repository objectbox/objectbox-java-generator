package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.Property;
import io.objectbox.annotation.Uid;

@Entity
@Uid
public class Note {

    @Id
    private Long id;

    @Uid(-1)
    private String generateNew;

    @Index
    @Uid(-1)
    private String generateNewWithIndex;

    private String control;

}
