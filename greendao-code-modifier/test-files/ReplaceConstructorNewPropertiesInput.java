package org.greenrobot.greendao.example;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;

@Entity
public class Note {

    @Id
    private Long id;

    private String string;
    private int newInt;

    @Generated(hash = 1884512397)
    public Note(Long id, String string, String string2) {
        this.id = id;
        this.string = string;
        this.string2 = string2;
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

}
