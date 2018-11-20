/*
 * Copyright (C) 2017-2018 ObjectBox Ltd.
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
import com.squareup.moshi.Types
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.PluginContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import java.io.File

class BuildTrackerTest {
    val toolName = "TestTool"

    @Test
    fun testBuildEventData() {
        val project = mock(Project::class.java)
        `when`(project.file(ArgumentMatchers.anyString())).thenReturn(mock(File::class.java))
        val pluginContainer = mock(PluginContainer::class.java)
        // Mocking "find" did not work out... Avoiding the call...
//        `when`(pluginContainer.find<Collection<Plugin<Any>>>(ArgumentMatchers.any())).thenReturn(emptyList<Plugin<Any>>())
//        `when`(pluginContainer.find<Any>(any())).thenReturn(emptyList<Plugin<Any>>())
//        doReturn(emptyList<Plugin<Any>>()).`when`(pluginContainer).find<Any>(any())
        `when`(project.plugins).thenReturn(pluginContainer)
        `when`(project.logger).thenReturn(mock(Logger::class.java))

        val extensionContainer = mock(ExtensionContainer::class.java)
        `when`(project.extensions).thenReturn(extensionContainer)

        val options = PluginOptions(project)
        `when`(extensionContainer.create(ProjectEnv.Const.name, PluginOptions::class.java, project)).thenReturn(options)

        val env = ProjectEnv(project)
        val toolName = "TestTool"
        val analytics = spy(GradleBuildTracker(toolName))
        val aid = "my.test.app"
        doReturn(aid).`when`(analytics).androidAppId(env)

        val eventData = analytics.eventData("Build", analytics.buildEventProperties(env))
        val json = parseJsonAndAssertBasics(eventData, "Build")
        @Suppress("UNCHECKED_CAST")
        val properties = json["properties"] as Map<String, Any>
        val distinctId = properties["distinct_id"] as String
        assertEquals(analytics.hashBase64WithoutPadding(aid), properties["AAID"])
        assertEquals(toolName, properties["Tool"])
        assertEquals(ProjectEnv.Const.pluginVersion, properties["Version"])

        val analytics2 = spy(GradleBuildTracker(toolName))
        assertEquals(distinctId, analytics2.uniqueIdentifier())
    }

    @Test
    fun testErrorEventData() {
        val analytics = spy(GradleBuildTracker(toolName))
        val cause = RuntimeException("Banana")
        val eventData = analytics.eventData("Error", analytics.errorProperties("Boo", Exception("Bad", cause)))

        val json = parseJsonAndAssertBasics(eventData, "Error")
        @Suppress("UNCHECKED_CAST")
        val properties = json["properties"] as Map<String, Any>

        assertEquals(ProjectEnv.Const.pluginVersion, properties["Version"])

        val exStack = properties["ExStack"] as String
        assertTrue(exStack, exStack.contains("Banana"))
        assertTrue(exStack, exStack.contains(javaClass.name))

        assertEquals("Bad", properties["ExMessage1"])
        assertEquals("java.lang.Exception", properties["ExClass1"])

        assertEquals("Banana", properties["ExMessage2"])
        assertEquals("java.lang.RuntimeException", properties["ExClass2"])
    }

    private fun parseJsonAndAssertBasics(eventData: String, expectedEvent:String): Map<String, Any> {
        val parameterizedType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = Moshi.Builder().build().adapter<Map<String, Any>>(parameterizedType)
        val json = adapter.fromJson(eventData)

        assertEquals(expectedEvent, json!!["event"])
        @Suppress("UNCHECKED_CAST")
        val properties = json["properties"] as Map<String, Any>
        assertNotNull(properties["token"])
        assertNotNull(properties["distinct_id"] as String)
        assertEquals(toolName, properties["Tool"])
        return json
    }
}