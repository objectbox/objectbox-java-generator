package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Property;

@Entity
public class Note {

    static final String CONSTANT_COLUMN = "example-column";
    static final boolean CONSTANT_TRUE = true;

    @Id(autoincrement = CONSTANT_TRUE)
    private Long id;

    @Property(nameInDb = CONSTANT_COLUMN)
    private String text;

    @Generated(1816070532)
    public Note(Long id, String text) {
        this.id = id;
        this.text = text;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
