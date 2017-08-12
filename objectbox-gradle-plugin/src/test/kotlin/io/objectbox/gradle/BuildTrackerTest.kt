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

        val options = ObjectBoxOptions(project)
        `when`(extensionContainer.create(ProjectEnv.Const.name, ObjectBoxOptions::class.java, project)).thenReturn(options)

        val env = ProjectEnv(project)
        val toolName = "TestTool"
        val analytics = spy(BuildTracker(toolName))
        val aid = "my.test.app"
        doReturn(aid).`when`(analytics).androidAppId(env.project)

        val eventData = analytics.eventData("Build", analytics.buildEventProperties(env))
        val json = parseJsonAndAssertBasics(eventData, "Build")
        val properties = json["properties"] as Map<String, Any>
        val distinctId = properties["distinct_id"] as String
        assertEquals(analytics.hashBase64WithoutPadding(aid), properties["AAID"])
        assertEquals(toolName, properties["Tool"])

        val analytics2 = spy(BuildTracker(toolName))
        assertEquals(distinctId, analytics2.uniqueIdentifier())
    }

    @Test
    fun testErrorEventData() {
        val analytics = spy(BuildTracker(toolName))
        val cause = RuntimeException("Banana")
        val eventData = analytics.eventData("Error", analytics.errorProperties("Boo", Exception("Bad", cause)))

        val json = parseJsonAndAssertBasics(eventData, "Error")
        val properties = json["properties"] as Map<String, Any>

        val exStack = properties["ExStack"] as String
        assertTrue(exStack, exStack.contains("Banana"))
        assertTrue(exStack, exStack.contains(javaClass.name))

        assertEquals("Bad", properties["ExMessage1"])
        assertEquals("java.lang.Exception", properties["ExClass1"])

        assertEquals("Banana", properties["ExMessage2"])
        assertEquals("java.lang.RuntimeException", properties["ExClass2"])
    }

    private fun parseJsonAndAssertBasics(eventData: String, expectedEvent:String): Map<String, Object> {
        val parameterizedType = Types.newParameterizedType(Map::class.java, String::class.java, Object::class.java);
        val adapter = Moshi.Builder().build().adapter<Map<String, Object>>(parameterizedType)
        val json = adapter.fromJson(eventData)

        assertEquals(expectedEvent, json["event"])
        val properties = json["properties"] as Map<String, Any>
        assertNotNull(properties["token"])
        assertNotNull(properties["distinct_id"] as String)
        assertEquals(toolName, properties["Tool"])
        return json
    }
}