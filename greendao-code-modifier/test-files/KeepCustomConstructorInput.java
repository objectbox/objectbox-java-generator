package org.greenrobot.greendao.example;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;

@Entity
public class Note {

    @Id
    private Long id;
    private String text;

    @Generated
    public Note() {
    }

    public Note(Long id) {
        // custom constructor
        this.id = id + 1;
    }

    @Generated
    public Note(Long id, String text) {
        this.id = id;
        this.text = text;
    }

}
