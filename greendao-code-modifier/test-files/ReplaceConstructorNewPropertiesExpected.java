package org.greenrobot.greendao.example;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

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
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getNewInt() {
        return this.newInt;
    }

    public void setNewInt(int newInt) {
        this.newInt = newInt;
    }

    public String getString() {
        return this.string;
    }

    public void setString(String string) {
        this.string = string;
    }

}
