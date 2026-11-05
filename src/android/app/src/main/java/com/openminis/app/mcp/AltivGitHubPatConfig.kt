package com.openminis.app.mcp

import com.openminis.app.data.repository.MCPRepository

/**
 * Single-token GitHub connection for the ALTIV Android flavor.
 * Only GitHub decides which repositories this personal access token can use.
 * Store the credential in EnvVarRepository's encrypted value store, not here,
 * in servers.json, in an export, or in any chat text.
 */
internal object AltivGitHubPatConfig {
    const val ID = "github"
    const val URL = "https://api.githubcopilot.com/mcp/"
    const val ENV_NAME = "ALTIV_GITHUB_MCP_TOKEN"
    const val HEADER_NAME = "Authorization"
    const val HEADER_REFERENCE = "Bearer \$\$ALTIV_GITHUB_MCP_TOKEN"

    fun canReplace(existing: MCPRepository.MCPServerConfig?): Boolean =
        existing == null || (!existing.isStdio &&
            existing.url?.trimEnd('/') == URL.trimEnd('/'))

    fun make(existing: MCPRepository.MCPServerConfig? = null) =
        MCPRepository.MCPServerConfig(
            id = ID,
            url = URL,
            headers = mapOf(HEADER_NAME to HEADER_REFERENCE),
            note = "GitHub MCP: repositórios autorizados pelo token pessoal",
            oauth = null,
            enabled = true,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
}
