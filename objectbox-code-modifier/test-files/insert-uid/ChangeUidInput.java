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

    private String control;

    @Uid(-1)
    private String generateNew;

}
