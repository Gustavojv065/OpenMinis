package com.openminis.app.mcp

/**
 * Account discovery only. Never grant a text model unrestricted database tools:
 * project connections must be scoped through SupabaseMcpProjectScope separately.
 * A distinct ID isolates OAuth credentials for each authorized Supabase account.
 */
internal object SupabaseMcpAccountScope {
    private const val prefix = "supabase-account-"
    const val DISCOVERY_URL =
        "https://mcp.supabase.com/mcp?read_only=true&features=account"

    fun nextId(existing: Collection<String>): String {
        var n = 1
        while ("$prefix$n" in existing) n++
        return "$prefix$n"
    }

    fun isAccountConnection(id: String, url: String, isUrlTransport: Boolean): Boolean =
        isUrlTransport && id.startsWith(prefix) &&
            id.removePrefix(prefix).toIntOrNull()?.let { it > 0 } == true &&
            url == DISCOVERY_URL

    /** Display a neutral number; OAuth MCP does not disclose an e-mail address. */
    fun displayName(id: String): String =
        "Conta Supabase " + id.removePrefix(prefix).toIntOrNull().toString()
}
