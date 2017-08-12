package io.objectbox.gradle

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.PluginContainer
import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import java.io.File

class AnalyticsTest {
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
        val analytics = spy(Analytics(env, toolName))
        val aid = "my.test.app"
        doReturn(aid).`when`(analytics).androidAppId()

        val eventData = analytics.buildEventData()

        val parameterizedType = Types.newParameterizedType(Map::class.java, String::class.java, Object::class.java);
        val adapter = Moshi.Builder().build().adapter<Map<String, Object>>(parameterizedType)
        val json = adapter.fromJson(eventData)
        Assert.assertEquals("Build", json["event"])
        val properties = json["properties"] as Map<String, Any>
        Assert.assertNotNull(properties["token"])
        val distinctId = properties["distinct_id"] as String
        Assert.assertNotNull(distinctId)
        Assert.assertEquals(analytics.hashBase64WithoutPadding(aid), properties["AAID"])
        Assert.assertEquals(toolName, properties["Tool"])

        val analytics2 = spy(Analytics(env, toolName))
        Assert.assertEquals(distinctId, analytics2.uniqueIdentifier())
    }
}