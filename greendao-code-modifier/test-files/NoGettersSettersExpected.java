package org.greenrobot.greendao.example;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

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
