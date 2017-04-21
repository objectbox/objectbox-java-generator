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

    @Generated(hash = 911239829)
    public Note(String text, Long id) {
        this.text = text;
        this.id = id;
    }

    @Generated(hash = 1272611929)
    public Note() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
