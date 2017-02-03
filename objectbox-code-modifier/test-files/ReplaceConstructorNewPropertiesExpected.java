package io.objectbox.codemodifier.test;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;

@Entity
public class Note {

    @Id
    private Long id;

    private String string;
    private int newInt;

    @Generated(hash = 1777635696)
    public Note(Long id, String string, int newInt) {
        this.id = id;
        this.string = string;
        this.newInt = newInt;
    }

    @Generated(hash = 1272611929)
    public Note() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public int getNewInt() {
        return newInt;
    }

    public void setNewInt(int newInt) {
        this.newInt = newInt;
    }

}
