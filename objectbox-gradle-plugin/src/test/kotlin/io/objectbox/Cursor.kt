package io.objectbox

/** FAKE just for tests */
class BoxStore {

}

/** FAKE just for tests */
open class Cursor<T> {
    @JvmField
    protected var boxStoreForEntities: BoxStore? = null
}

