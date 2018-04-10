// no package

import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.NameInDb;
import io.objectbox.annotation.Transient;
import io.objectbox.annotation.Uid;
import io.objectbox.converter.PropertyConverter;
import io.objectbox.relation.ToMany;
import io.objectbox.relation.ToOne;

@Entity
@NameInDb("A")
public class SimpleEntityNoPackage {

    @Id(assignable = true)
    Long id;

    String simpleString;

    @NameInDb("B")
    String namedProperty;

    // Currently broken because TextUtil.getPackageFromFullyQualified() in Entity.init3rdPassAdditionalImports()
    // can not handle sub-types (like Entity.MyType)
    // note: still works fine if custom type and converter are top-level classes
//    @Convert(converter = SimpleEnumConverter.class, dbType = Integer.class)
//    SimpleEnum customType;
//
//    @Convert(converter = SimpleEnumListConverter.class, dbType = Integer.class)
//    List<SimpleEnum> customTypes;

    ToOne<SimpleEntityNoPackage> toOne;

    ToMany<SimpleEntityNoPackage> toMany;

    public enum SimpleEnum {
        DEFAULT(0), A(1), B(2);

        final int id;

        SimpleEnum(int id) {
            this.id = id;
        }
    }

    public static class SimpleEnumConverter implements PropertyConverter<SimpleEnum, Integer> {
        @Override
        public SimpleEnum convertToEntityProperty(Integer databaseValue) {
            return SimpleEnum.DEFAULT;
        }

        @Override
        public Integer convertToDatabaseValue(SimpleEnum entityProperty) {
            return SimpleEnum.DEFAULT.id;
        }
    }

    public static class SimpleEnumListConverter implements PropertyConverter<List<SimpleEnum>, Integer> {
        @Override
        public List<SimpleEnum> convertToEntityProperty(Integer databaseValue) {
            return Collections.singletonList(SimpleEnum.DEFAULT);
        }

        @Override
        public Integer convertToDatabaseValue(List<SimpleEnum> entityProperty) {
            return SimpleEnum.DEFAULT.id;
        }
    }
}
