package io.objectbox.gradle

import com.squareup.moshi.Moshi
import io.objectbox.reporting.ObjectBoxBuildConfig
import io.objectbox.reporting.ObjectBoxBuildConfigJsonAdapter
import okio.buffer
import okio.source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder


class BuildConfigTest {

    @JvmField
    @Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun write() {
        val folder = temporaryFolder.newFolder("build")
        val file = ObjectBoxBuildConfig.buildFile(folder)
        ObjectBoxBuildConfig("/example/dir", "flavor").writeInto(file)

        assertTrue(file.exists())
        assertTrue(file.isFile)

        val buildConfig = file.source().buffer().use {
            ObjectBoxBuildConfigJsonAdapter(Moshi.Builder().build()).fromJson(it)!!
        }
        assertEquals("/example/dir", buildConfig.projectDir)
        assertEquals("flavor", buildConfig.flavor)
        assertNotEquals(0, buildConfig.timeStarted)
        assertTrue(buildConfig.timeStarted <= System.currentTimeMillis())
    }
}