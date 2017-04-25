package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class Note {

    @Id
    Long id;

    private String getterSetter;

    String noGetterSetter;

    protected String noGetterSetter2;

    public String noGetterSetter3;

}
