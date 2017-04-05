package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;

@Entity
public class IdOnly {

    @Id
    long id;

}
