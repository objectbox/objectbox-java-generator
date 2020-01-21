package io.objectbox.gradle

import com.squareup.moshi.Moshi
import io.objectbox.reporting.ObjectBoxBuildConfig
import okio.Okio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File


class BuildConfigTest {

    @JvmField
    @Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun write() {
        val folder = temporaryFolder.newFolder("build")
        ObjectBoxBuildConfig("/example/dir", "flavor").writeInto(folder)

        val file = File(folder, ObjectBoxBuildConfig.FILE_NAME)
        assertTrue(file.exists())
        assertTrue(file.isFile)

        val buildConfig = Okio.buffer(Okio.source(file)).use {
            Moshi.Builder().build().adapter<ObjectBoxBuildConfig>(ObjectBoxBuildConfig::class.java).fromJson(it)!!
        }
        assertEquals("/example/dir", buildConfig.projectDir)
        assertEquals("flavor", buildConfig.flavor)
        assertNotEquals(0, buildConfig.timeStarted)
        assertTrue(buildConfig.timeStarted <= System.currentTimeMillis())
    }
}