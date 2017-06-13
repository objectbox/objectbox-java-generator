package io.objectbox.processor.test;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class ToOneAllArgs {

    @Id long id;

    ToOne<ToOneParent> parent = new ToOne<>(this, ToOneAllArgs_.parent);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

    public ToOneAllArgs(long id, long parentId) {
        this.id = id;
        this.parent.setTargetId(parentId);
    }

}
