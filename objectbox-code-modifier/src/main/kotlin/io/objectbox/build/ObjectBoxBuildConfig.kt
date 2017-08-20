package io.objectbox.build

class ObjectBoxBuildConfig (val projectDir: String, val flavor: String? = null) {
    companion object {
        const val FILE_NAME = "objectbox-build-config.json"
    }

    val timeStarted = System.currentTimeMillis()
}