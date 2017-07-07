package io.objectbox.gradle.transform

import io.objectbox.Cursor
import io.objectbox.EntityInfo
import io.objectbox.annotation.Entity
import io.objectbox.relation.RelationInfo
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.reflect.KClass


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
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null)
}

@Entity
class EntityToOneLateInit {
    lateinit var entityEmpty: ToOne<EntityEmpty>
}

object EntityToOneLateInit_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null)
}

@Entity
class EntityToOneSuffix {
    lateinit var entityEmptyToOne: ToOne<EntityEmpty>
}

object EntityToOneSuffix_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null)
}

@Entity
class EntityToMany {
    val entityEmpty = ToMany<EntityEmpty>(this, null)
    val entityEmptyList = listOf<EntityEmpty>()
}

object EntityToMany_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null)
}

@Entity
class EntityToManyLateInit {
    lateinit var entityEmpty: ToMany<EntityEmpty>
}

object EntityToManyLateInit_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null)
}

@Entity
class EntityToManySuffix {
    lateinit var entityEmptyToMany: ToMany<EntityEmpty>
}

object EntityToManySuffix_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null)
}

@Entity
class EntityToManyListLateInit {
    lateinit var typelessList: List<*>
    lateinit var entityEmpty: List<EntityEmpty>
}

object EntityToManyListLateInit_ : EntityInfo<EntityToOneLateInit> {
    @JvmField
    val entityEmpty = RelationInfo<EntityEmpty>(null, null, null)
}

@Entity
class EntityTransientList {
    @Transient
    lateinit var transientList1: List<EntityEmpty>

//    @io.objectbox.annotation.Transient
//    lateinit var transientList2: List<EntityEmpty>
//
//    @Rule
//    val dummyWithAlienAnnotation: Boolean = false
}

object EntityTransientList_ : EntityInfo<EntityToOneLateInit>

class TestCursor : Cursor<EntityBoxStoreField>() {
    private fun attachEntity(@Suppress("UNUSED_PARAMETER") entity: EntityBoxStoreField) {}
}

class CursorWithExistingImpl : Cursor<EntityBoxStoreField>() {
    private fun attachEntity(entity: EntityBoxStoreField) {
        System.out.println(entity)
    }
}

class JustCopyMe

abstract class AbstractTransformTest {
    private val classDir1 = File("build/classes/test")
    private val classDir2 = File("objectbox-gradle-plugin/${classDir1.path}")
    val classDir = if (classDir1.exists()) classDir1 else classDir2

    val prober = ClassProber(true)

    @Test
    fun testClassDir() {
        assertTrue(classDir.exists())
    }

    protected fun probeClass(kclass: KClass<*>): ProbedClass {
        val file = File(classDir, kclass.qualifiedName!!.replace('.', '/') + ".class")
        assertTrue(file.exists())
        return prober.probeClass(file)
    }

}