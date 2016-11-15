package org.greenrobot.greendao.example;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class Note {

    @Id
    private Long id;

    /** Switch on string: ensure that parser source + compliance level is at least 1.7. */
    public void doSwitch(String sandra) {
        switch (sandra) {
            default:
                break;
        }
    }

}
