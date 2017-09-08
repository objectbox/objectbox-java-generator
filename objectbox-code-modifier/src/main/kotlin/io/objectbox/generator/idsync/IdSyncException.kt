package io.objectbox.generator.idsync

open class IdSyncException(message: String? = null, cause: Throwable?=null) : RuntimeException(message, cause) {
}

class IdSyncPrintUidException(uidTarget:String, currentUid: Long, randomNewUid: Long):
        IdSyncException("UID operations for $uidTarget:\n" +
                "[Rename] apply the current UID using @Uid(${currentUid}L)\n" +
                "[Change/reset] apply a new UID using @Uid(${randomNewUid}L)")