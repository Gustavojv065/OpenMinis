package com.openminis.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ApiProviderTemplateTest {
    @Test
    fun `all shortcuts are unique, HTTPS, and versioned`() {
        val templates = ApiProviderTemplate.all
        assertEquals(13, templates.size)
        assertEquals(templates.size, templates.map { it.id }.toSet().size)
        assertEquals(templates.size, templates.map { it.name }.toSet().size)
        assertTrue(templates.all { it.baseURL.startsWith("https://") })
        assertTrue(templates.all { it.baseURL.substringAfter("https://").contains("/") })
        assertTrue(templates.all { !it.appendV1 })
        assertFalse(templates.any { it.baseURL.contains("API_KEY") })
    }

    @Test
    fun `custom endpoints do not get duplicate v1 suffixes`() {
        assertEquals("https://integrate.api.nvidia.com/v1",
            ApiProviderTemplate.all.first { it.id == "nvidia" }.baseURL)
        assertEquals("https://api.minimax.io/v1",
            ApiProviderTemplate.all.first { it.id == "minimax" }.baseURL)
        assertEquals("https://api.xiaomimimo.com/v1",
            ApiProviderTemplate.all.first { it.id == "mimo" }.baseURL)
        assertEquals("https://api.groq.com/openai/v1",
            ApiProviderTemplate.all.first { it.id == "groq" }.baseURL)
        assertEquals("https://opencode.ai/zen/v1",
            ApiProviderTemplate.all.first { it.id == "opencode" }.baseURL)
        assertEquals("https://api.deepinfra.com/v1/openai",
            ApiProviderTemplate.all.first { it.id == "deepinfra" }.baseURL)
    }

    @Test
    fun `presets remain connected to Add Provider flow and voice flow remains`() {
        val source = File("src/main/java/com/openminis/app/ui/settings/AddProviderScreen.kt").readText()
        assertTrue(source.contains("ApiProviderTemplate.all"))
        assertTrue(source.contains("onSelectApiTemplate(template)"))
        assertTrue(source.contains("apiTemplate?.baseURL"))
        assertTrue(source.contains("apiTemplate?.appendV1"))
        assertTrue(source.contains("VoiceProviderTemplate.all"))
        assertTrue(source.contains("onSelectVoiceTemplate(template)"))
    }
}
