package org.greenrobot.greendao.example;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

@Entity
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

    @Generated(hash = 1816070532)
    public Note(Long id, String text) {
        this.id = id;
        this.text = text;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
