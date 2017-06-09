package io.objectbox.test.kotlin

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Generated
import io.objectbox.annotation.Id
import io.objectbox.annotation.Transient
import io.objectbox.converter.PropertyConverter
import io.objectbox.annotation.apihint.Internal

@Entity
class TypesInInnerClass(
        @Id
        var id: Long = 0,

        @Convert(converter = MyInnerTypeConverter::class, dbType = Long::class)
        var type: MyInnerType,

        var dummy: String
) {
    class MyInnerType(var value: String)

    class MyInnerTypeConverter : PropertyConverter<MyInnerType, Long> {

        override fun convertToEntityProperty(databaseValue: Long?): MyInnerType? {
            return if (databaseValue != null) MyInnerType(java.lang.Long.toHexString(databaseValue)) else null
        }

        @Override
        override fun convertToDatabaseValue(entityProperty: MyInnerType?): Long? {
            return if (entityProperty != null) java.lang.Long.parseLong(entityProperty.value, 16) else null
        }
    }

}
