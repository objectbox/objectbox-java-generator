package io.objectbox.gradle.transform

import io.objectbox.Cursor
import io.objectbox.EntityInfo
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.converter.PropertyConverter
import io.objectbox.relation.RelationInfo
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import org.greenrobot.essentials.collections.LongHashSet
import org.junit.Rule


@Entity
class EntityEmpty

@Entity
class EntityBoxStoreField {
    val __boxStore = Object()
}

@Entity
class EntityToOne {
    val entityEmpty = ToOne<EntityEmpty>(this, null)
}

object EntityToOne_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToOneLateInit {
    lateinit var entityEmpty: ToOne<EntityEmpty>
}

object EntityToOneLateInit_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToOneSuffix {
    lateinit var entityEmptyToOne: ToOne<EntityEmpty>
}

object EntityToOneSuffix_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToMany {
    val entityEmpty = ToMany<EntityEmpty>(this, null)
    val entityEmptyList = listOf<EntityEmpty>()
}

object EntityToMany_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToManyLateInit {
    lateinit var entityEmpty: ToMany<EntityEmpty>
}

object EntityToManyLateInit_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToManyAndConverter {
    lateinit var entityEmpty: ToMany<EntityEmpty>

    @Convert(converter = TestConverter::class, dbType = String::class)
    lateinit var convertedString: JustCopyMe
}

object EntityToManyAndConverter_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToOneAndConverter(val someExternalType: LongHashSet? = LongHashSet(8)) {
    lateinit var entityEmpty: ToOne<EntityEmpty>

    @Convert(converter = TestConverter::class, dbType = String::class)
    lateinit var convertedString: JustCopyMe
}

object EntityToOneAndConverter_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToManySuffix {
    lateinit var entityEmptyToMany: ToMany<EntityEmpty>
}

object EntityToManySuffix_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null, null)
}

@Entity
class EntityToManyListLateInit {
    lateinit var typelessList: List<*>
    lateinit var entityEmpty: List<EntityEmpty>
}

object EntityToManyListLateInit_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null, null)
}

@Entity
class EntityTransientList {
    @Transient
    lateinit var transientList1: List<EntityEmpty>

    @io.objectbox.annotation.Transient
    lateinit var transientList2: List<EntityEmpty>

    lateinit var actualRelation: List<EntityEmpty>

    @Rule
    val dummyWithAlienAnnotation: Boolean = false
}

object EntityTransientList_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val actualRelation = RelationInfo<EntityEmpty>(null, null, null, null)
}

class TestCursor : Cursor<EntityBoxStoreField>() {
    private fun attachEntity(@Suppress("UNUSED_PARAMETER") entity: EntityBoxStoreField) {}
}

class CursorWithExistingImpl : Cursor<EntityBoxStoreField>() {
    private fun attachEntity(entity: EntityBoxStoreField) {
        System.out.println(entity)
    }
}

class JustCopyMe

class TestConverter : PropertyConverter<String, String> {
    override fun convertToEntityProperty(databaseValue: String?): String? {
        return databaseValue?.substring(0, databaseValue.length - 1)
    }

    override fun convertToDatabaseValue(entityProperty: String?): String? {
        return if (entityProperty == null) null else entityProperty + "!"
    }
}
