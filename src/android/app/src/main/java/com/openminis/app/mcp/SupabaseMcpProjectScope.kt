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

    /**
     * The user can paste either a Supabase project ref or the URL displayed in
     * the Supabase Dashboard. Never use the provided URL for network requests:
     * extract an allowlisted project ref and build our own read-only URL.
     */
    fun createFromUserInput(input: String): MCPQuickConnectPreset? {
        val value = input.trim()
        if (!value.startsWith("https://")) return create(value)
        val dashboard = try { java.net.URI(value) } catch (_: Exception) { return null }
        if (dashboard.scheme != "https" ||
            dashboard.host != "supabase.com" ||
            dashboard.port != -1 ||
            dashboard.userInfo != null
        ) return null
        val segments = dashboard.path.split("/")
        if (segments.size < 4 || segments[1] != "dashboard" || segments[2] != "project") return null
        return create(segments[3])
    }

    /** Prevent accidental removal of project scope/read-only in guided entries. */
    fun isSafeConfiguration(serverId: String, url: String, isUrlTransport: Boolean): Boolean {
        if (!isUrlTransport || !serverId.startsWith(prefix)) return false
        val ref = serverId.removePrefix(prefix)
        return create(ref)?.url == url
    }
}
