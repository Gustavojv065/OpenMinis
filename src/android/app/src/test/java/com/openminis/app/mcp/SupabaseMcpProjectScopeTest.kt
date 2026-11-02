package com.openminis.app.mcp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseMcpProjectScopeTest {
    @Test fun `creates project scoped read-only endpoint`() {
        val preset = requireNotNull(SupabaseMcpProjectScope.create("abcdefghijklmnopqrst"))
        assertEquals("supabase-abcdefghijklmnopqrst", preset.id)
        assertTrue(preset.url.contains("project_ref=abcdefghijklmnopqrst"))
        assertTrue(preset.url.contains("read_only=true"))
        assertTrue(preset.url.startsWith("https://mcp.supabase.com/mcp?"))
        assertTrue(SupabaseMcpProjectScope.isSafeConfiguration(preset.id, preset.url, true))
    }

    @Test fun `rejects empty or injected project references`() {
        listOf("", "  ", "abc", "ABC1234567", "project/name", "id123456?read_only=false",
            "id123456&read_only=false", "../secret", "id123456%26foo", "id123456;DROP",
            "id123456.foo", "a".repeat(33))
            .forEach { assertNull(SupabaseMcpProjectScope.create(it)) }
    }

    @Test fun `rejects removing read only or replacing project`() {
        val preset = requireNotNull(SupabaseMcpProjectScope.create("abcdefghijklmnopqrst"))
        assertFalse(SupabaseMcpProjectScope.isSafeConfiguration(preset.id, preset.url.replace("true", "false"), true))
        assertFalse(SupabaseMcpProjectScope.isSafeConfiguration(preset.id, preset.url.replace("abcdefghijklmnopqrst", "zyxwvutsrqponmlkjihg"), true))
        assertFalse(SupabaseMcpProjectScope.isSafeConfiguration(preset.id, preset.url, false))
        assertFalse(SupabaseMcpProjectScope.isSafeConfiguration("supabase-", preset.url, true))
    }
}
