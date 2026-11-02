package com.openminis.app.mcp

/**
 * Creates an explicitly scoped, read-only Supabase MCP configuration.
 *
 * The model must not be granted unscoped access to every Supabase project.
 * This builder is side-effect free: it never starts OAuth, opens a connection
 * or changes the remote database.
 *
 * Supabase docs: https://supabase.com/docs/guides/ai-tools/mcp
 */
internal object SupabaseMcpProjectScope {
    private val projectRefPattern = Regex("^[a-z0-9]{8,32}$")
    private const val prefix = "supabase-"

    fun create(rawProjectRef: String): MCPQuickConnectPreset? {
        val ref = rawProjectRef.trim()
        if (!projectRefPattern.matches(ref)) return null
        return MCPQuickConnectPreset(
            id = "$prefix$ref",
            label = "Supabase ($ref) — somente leitura",
            url = "https://mcp.supabase.com/mcp?project_ref=$ref&read_only=true&features=database%2Cdocs%2Cdebugging",
        )
    }

    /** Prevent accidental removal of project scope/read-only in guided entries. */
    fun isSafeConfiguration(serverId: String, url: String, isUrlTransport: Boolean): Boolean {
        if (!isUrlTransport || !serverId.startsWith(prefix)) return false
        val ref = serverId.removePrefix(prefix)
        return create(ref)?.url == url
    }
}
