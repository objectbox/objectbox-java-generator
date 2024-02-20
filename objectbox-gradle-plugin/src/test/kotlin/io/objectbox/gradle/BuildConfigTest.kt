/*
 * Copyright (C) 2020-2022 ObjectBox Ltd.
 *
 * This file is part of ObjectBox Build Tools.
 *
 * ObjectBox Build Tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ObjectBox Build Tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ObjectBox Build Tools.  If not, see <http://www.gnu.org/licenses/>.
 */

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