package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Generated;

@Entity(generateGettersSetters = false)
public class Note {

    @Id
    private Long id;

    private String name;

    @Generated(654067880)
    public Note(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Generated(1272611929)
    public Note() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
