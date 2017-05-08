package io.objectbox.codemodifier.test.one;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

import java.util.List;

@Entity
public class OnePackage {

    @Id
    long id;

    ToOne<OtherPackage> other;

}
