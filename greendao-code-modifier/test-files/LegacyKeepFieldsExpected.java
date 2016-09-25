package org.greenrobot.greendao.example;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Transient;

@Entity
public class Note {

    @Id
    private Long id;

    // KEEP FIELDS - put your custom fields here

    @Transient
    private String textInside;

    private transient String textTransient;

    // KEEP FIELDS END

    private String textOutside;

    @Generated(hash = 1854957810)
    public Note(Long id, String textOutside) {
        this.id = id;
        this.textOutside = textOutside;
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

    public String getTextOutside() {
        return this.textOutside;
    }

    public void setTextOutside(String textOutside) {
        this.textOutside = textOutside;
    }

}
