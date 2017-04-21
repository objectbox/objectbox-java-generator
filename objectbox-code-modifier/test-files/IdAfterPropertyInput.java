package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Property;

@Entity
public class Note {

    @Property
    private String text;

    @Id
    private Long id;

}
