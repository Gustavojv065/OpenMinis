package com.openminis.app.mcp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseMcpAccountScopeTest {
    @Test fun uniqueIdsPreventOAuthTokensFromColliding() {
        assertEquals("supabase-account-1", SupabaseMcpAccountScope.nextId(emptySet()))
        assertEquals("supabase-account-3",
            SupabaseMcpAccountScope.nextId(setOf("supabase-account-1", "supabase-account-2")))
    }
    @Test fun discoveryScopeContainsOnlyAccountToolsAndReadOnlyMode() {
        val url = SupabaseMcpAccountScope.DISCOVERY_URL
        assertTrue(url.startsWith("https://mcp.supabase.com/mcp?"))
        assertTrue(url.contains("read_only=true"))
        assertTrue(url.contains("features=account"))
        assertFalse(url.contains("features=database"))
        assertTrue(SupabaseMcpAccountScope.isAccountConnection("supabase-account-2", url, true))
    }
    @Test fun rejectsUnsafeOrUnscopedMcpServers() {
        assertFalse(SupabaseMcpAccountScope.isAccountConnection(
            "supabase-account-1", "https://mcp.supabase.com/mcp", true))
        assertFalse(SupabaseMcpAccountScope.isAccountConnection(
            "supabase-account-a", SupabaseMcpAccountScope.DISCOVERY_URL, true))
        assertFalse(SupabaseMcpAccountScope.isAccountConnection(
            "supabase-account-1", SupabaseMcpAccountScope.DISCOVERY_URL, false))
    }
}
