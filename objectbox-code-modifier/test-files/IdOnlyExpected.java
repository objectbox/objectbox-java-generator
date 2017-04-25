package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;

@Entity
public class IdOnly {

    @Id
    long id;

    @Generated(hash = 1303514080)
    public IdOnly(long id) {
        this.id = id;
    }

    @Generated(hash = 203147900)
    public IdOnly() {
    }

}
