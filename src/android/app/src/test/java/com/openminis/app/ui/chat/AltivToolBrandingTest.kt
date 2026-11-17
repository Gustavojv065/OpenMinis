package com.openminis.app.ui.chat

import com.openminis.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression for the screenshot showing "Minis Computer" and
 * "Minis is using Memory" in the commercial ALTIVE APK.
 * Technical package IDs and /var/minis paths stay unchanged.
 */
class AltivToolBrandingTest {
    @Test fun allAgentToolActivityLabelsUseAltiveBrand() {
        assertEquals("altiv", BuildConfig.FLAVOR)
        assertEquals("ALTIVE DEV AI", toolAgentName)
        val tools = listOf(
            "shell_execute", "file_read", "file_write", "file_edit",
            "browser_use", "read_image", "memory_write", "memory_get",
            "web_search", "subagent_task", "delegate_task", "agent_status", "other_tool"
        )
        for (tool in tools) {
            val label = toolTitleLabel(tool)
            assertTrue("$tool: $label", label.startsWith("ALTIVE DEV AI"))
            assertFalse("$tool: $label", label.contains("Minis"))
        }
        assertEquals("ALTIVE DEV AI is using Memory", toolTitleLabel("memory_get"))
        assertEquals("ALTIVE DEV AI", friendlyToolTitleFor(null))
    }

    @Test fun computerSheetAndThinkingIndicatorUseAppBrand() {
        val source = File("src/main/java/com/openminis/app/ui/chat/ChatToolDetailUI.kt").readText()
        assertTrue(source.contains("BuildConfig.FLAVOR == \"altiv\""))
        assertTrue(source.contains("LocalContext.current.getString(R.string.app_name)"))
        assertFalse(source.contains("text = \"Minis Computer\","))
        val thinking = File("src/main/java/com/openminis/app/ui/chat/ChatIndicators.kt").readText()
        assertTrue(thinking.contains("val fallbackName = stringResource(R.string.app_name)"))
        assertFalse(thinking.contains("ifEmpty { \"Minis\" }"))
    }

    @Test fun altiveResourceOverlayKeepsPublicBrand() {
        for (locale in listOf("values", "values-pt-rBR")) {
            val xml = File("src/altiv/res/$locale/strings.xml").readText()
            assertTrue(xml.contains("name=\"app_name\">ALTIV DEV AI</string>"))
        }
    }
}
