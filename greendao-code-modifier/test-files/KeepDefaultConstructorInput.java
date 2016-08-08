package org.greenrobot.greendao.example;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

@Entity(generateConstructors = false)
public class Note {

    @Id
    private Long id;
    private String text;

    @Generated(hash = 1272611929)
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
