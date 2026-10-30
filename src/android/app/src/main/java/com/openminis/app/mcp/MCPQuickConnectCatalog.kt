package com.openminis.app.mcp

/**
 * Verified, public remote MCP endpoints. These templates only prefill the
 * existing form; selecting one must never save credentials or start OAuth.
 *
 * Figma is deliberately excluded: its remote MCP currently limits new clients.
 * Supabase is deliberately excluded until a project-scoped read-only flow is
 * available, to prevent unintentional access to every project.
 */
internal data class MCPQuickConnectPreset(
    val id: String,
    val label: String,
    val url: String,
)

internal object MCPQuickConnectCatalog {
    val presets: List<MCPQuickConnectPreset> = listOf(
        MCPQuickConnectPreset(
            id = "github",
            label = "GitHub",
            url = "https://api.githubcopilot.com/mcp/",
        ),
        MCPQuickConnectPreset(
            id = "notion",
            label = "Notion",
            url = "https://mcp.notion.com/mcp",
        ),
        MCPQuickConnectPreset(
            id = "vercel",
            label = "Vercel",
            url = "https://mcp.vercel.com",
        ),
    )
}
