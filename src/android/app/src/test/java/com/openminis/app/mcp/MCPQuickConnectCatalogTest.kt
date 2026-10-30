package com.openminis.app.mcp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MCPQuickConnectCatalogTest {
    @Test
    fun `remote presets use unique, expected IDs`() {
        val ids = MCPQuickConnectCatalog.presets.map { it.id }
        assertEquals(listOf("github", "notion", "vercel"), ids)
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `presets contain only public HTTPS endpoints without secrets`() {
        MCPQuickConnectCatalog.presets.forEach { preset ->
            assertTrue(preset.url.startsWith("https://"))
            assertFalse(preset.url.contains("@"))
            assertFalse(preset.url.contains("token=", ignoreCase = true))
            assertFalse(preset.url.contains("key=", ignoreCase = true))
            assertTrue(preset.label.isNotBlank())
        }
    }

    @Test
    fun `official endpoints are kept intact`() {
        val endpoints = MCPQuickConnectCatalog.presets.associate { it.id to it.url }
        assertEquals("https://api.githubcopilot.com/mcp/", endpoints["github"])
        assertEquals("https://mcp.notion.com/mcp", endpoints["notion"])
        assertEquals("https://mcp.vercel.com", endpoints["vercel"])
    }
}
