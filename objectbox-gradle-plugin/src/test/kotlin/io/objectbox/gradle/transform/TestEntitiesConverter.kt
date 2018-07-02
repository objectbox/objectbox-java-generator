@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "unused", "ClassName")

package io.objectbox.gradle.transform

import io.objectbox.EntityInfo
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.converter.PropertyConverter
import io.objectbox.relation.RelationInfo
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import org.greenrobot.essentials.collections.LongHashSet


class EntityEmptyConverter : PropertyConverter<List<EntityEmpty>, String> {
    override fun convertToDatabaseValue(entityProperty: List<EntityEmpty>?): String {
        return "" // dummy
    }

    override fun convertToEntityProperty(databaseValue: String?): List<EntityEmpty> {
        return emptyList() // dummy
    }
}

@Entity
class EntityConverterList {
    @Convert(converter = EntityEmptyConverter::class, dbType = String::class)
    lateinit var convertedList: List<EntityEmpty>
}

@Entity
class EntityConverterListAndList {
    @Convert(converter = EntityEmptyConverter::class, dbType = String::class)
    lateinit var convertedList: List<EntityEmpty>

    lateinit var actualRelation: List<EntityEmpty>
}

object EntityConverterListAndList_ : EntityInfo<EntityConverterListAndList> {
    @JvmField
    val actualRelation = RelationInfo<EntityConverterListAndList, EntityEmpty>(null, null, null, null)
}

class TestConverter : PropertyConverter<String, String> {
    override fun convertToEntityProperty(databaseValue: String?): String? {
        return databaseValue?.substring(0, databaseValue.length - 1)
    }

    override fun convertToDatabaseValue(entityProperty: String?): String? {
        return if (entityProperty == null) null else "$entityProperty!"
    }
}

@Entity
class EntityConverterAndToMany {
    lateinit var entityEmpty: ToMany<EntityEmpty>

    @Convert(converter = TestConverter::class, dbType = String::class)
    lateinit var convertedString: JustCopyMe
}

object EntityConverterAndToMany_ : EntityInfo<EntityConverterAndToMany> {
    @JvmField
    val entityEmpty = RelationInfo<EntityConverterAndToMany, EntityEmpty>(null, null, null, null)
}

@Entity
class EntityConverterAndToOne(val someExternalType: LongHashSet? = LongHashSet(8)) {
    lateinit var entityEmpty: ToOne<EntityEmpty>

    @Convert(converter = TestConverter::class, dbType = String::class)
    lateinit var convertedString: JustCopyMe
}

object EntityConverterAndToOne_ : EntityInfo<EntityConverterAndToOne> {
    @JvmField
    val entityEmpty = RelationInfo<EntityConverterAndToOne, EntityEmpty>(null, null, null, null)
}