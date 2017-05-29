package io.objectbox.processor.test;

import java.util.List;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class BacklinkListParentEntity {

    @Id long id;

    @Backlink
    List<BacklinkListChildEntity> children = new ToMany<>(this, BacklinkListParentEntity_.children);

    // need to add manually, as processor can not modify entity
    transient BoxStore __boxStore;

}
