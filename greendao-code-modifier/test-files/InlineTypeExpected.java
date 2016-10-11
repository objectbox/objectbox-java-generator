package org.greenrobot.greendao.example;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import org.greenrobot.greendao.converter.PropertyConverter;
import io.objectbox.annotation.Generated;

@Entity
public class Note {

    @Id
    private Long id;

    @Convert(converter = NoteTypeConverter.class, dbType = String.class)
    private NoteType type;

    @Generated(hash = 1415525089)
    public Note(Long id, NoteType type) {
        this.id = id;
        this.type = type;
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

    public NoteType getType() {
        return this.type;
    }

    public void setType(NoteType type) {
        this.type = type;
    }

    enum NoteType {
        TEXT, LIST, PICTURE
    }

    static class NoteTypeConverter implements PropertyConverter<NoteType, String> {
        @Override
        public NoteType convertToEntityProperty(String databaseValue) {
            return NoteType.valueOf(databaseValue);
        }

        @Override
        public String convertToDatabaseValue(NoteType entityProperty) {
            return entityProperty.name();
        }
    }

}
