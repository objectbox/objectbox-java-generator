package org.greenrobot.greendao.example;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Generated;

@Entity
public class Note {

    @Id
    private String id;

    @Generated(hash = 617073424)
    public Note(String id) {
        this.id = id;
    }

    @Generated(hash = 1272611929)
    public Note() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
