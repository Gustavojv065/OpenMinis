package com.openminis.app.mcp

/**
 * Verified, public remote MCP endpoints. These templates only prefill the
 * existing form; selecting one must never save credentials or start OAuth.
 *
 * Figma is deliberately excluded: its remote MCP accepts only approved clients.
 * Supabase uses its dedicated scoped flow; do not add a second broad preset.
 * MagicPath/Magnific are OPTIONAL user-authorized integrations, not installed
 * ChatGPT plugins or automatically authenticated accounts. Magnific operations
 * may spend the customer's credits and must require clear approval.
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
        MCPQuickConnectPreset(
            id = "magicpath",
            label = "MagicPath",
            url = "https://api.magicpath.ai/mcp",
        ),
        MCPQuickConnectPreset(
            id = "magnific",
            label = "Magnific (usa créditos da conta)",
            url = "https://api.mcp.ai/p_magnific",
        ),
    )
}
