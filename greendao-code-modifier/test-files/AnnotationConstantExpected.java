package org.greenrobot.greendao.example;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;

@Entity
public class Note {

    static final String CONSTANT_COLUMN = "example-column";
    static final boolean CONSTANT_TRUE = true;

    @Id(autoincrement = CONSTANT_TRUE)
    private Long id;

    @Property(nameInDb = CONSTANT_COLUMN)
    private String text;

    @Generated(hash = 1816070532)
    public Note(Long id, String text) {
        this.id = id;
        this.text = text;
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

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
