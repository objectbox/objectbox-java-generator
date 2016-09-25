package org.greenrobot.greendao.example;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Generated;

@Entity(generateGettersSetters = false)
public class Note {

    @Id
    private Long id;

    private String name;

    @Generated(hash = 654067880)
    public Note(Long id, String name) {
        this.id = id;
        this.name = name;
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
