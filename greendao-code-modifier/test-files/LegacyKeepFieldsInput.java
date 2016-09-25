package org.greenrobot.greendao.example;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;

@Entity
public class Note {

    @Id
    private Long id;

    // KEEP FIELDS - put your custom fields here

    private String textInside;
    private transient String textTransient;

    // KEEP FIELDS END

    private String textOutside;

    @Generated(hash = 1390446558)
    public Note(Long id) {
        this.id = id;
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
