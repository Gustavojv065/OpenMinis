package com.openminis.app.mcp

import com.openminis.app.data.repository.MCPRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AltivGitHubPatConfigTest {
    @Test fun githubUsesOfficialServerAndOnlyEncryptedTokenReference() {
        val config = AltivGitHubPatConfig.make()
        assertEquals("github", config.id)
        assertEquals("https://api.githubcopilot.com/mcp/", config.url)
        assertTrue(config.enabled)
        assertNull(config.oauth)
        assertEquals(mapOf(
            "Authorization" to "Bearer \$\$ALTIV_GITHUB_MCP_TOKEN"
        ), config.headers)
        assertFalse(config.headers.toString().contains("github_pat_"))
        assertFalse(config.headers.toString().contains("ghp_"))
    }

    @Test fun cannotOverwriteUnrelatedMcpServerNamedGithub() {
        val wrongServer = MCPRepository.MCPServerConfig(
            id = "github", url = "https://example.com/mcp"
        )
        assertFalse(AltivGitHubPatConfig.canReplace(wrongServer))
        assertTrue(AltivGitHubPatConfig.canReplace(null))
        assertTrue(AltivGitHubPatConfig.canReplace(AltivGitHubPatConfig.make()))
    }

    @Test fun preservesServerIdentityWhileSwitchingAwayFromOauth() {
        val old = MCPRepository.MCPServerConfig(
            id = "github", url = AltivGitHubPatConfig.URL, createdAt = 12345L,
        )
        val next = AltivGitHubPatConfig.make(old)
        assertEquals(12345L, next.createdAt)
        assertEquals("github", next.id)
        assertNull(next.oauth)
    }

    @Test fun interfaceDoesNotRequestClientSecretsOrWritePlaintextToMcpJson() {
        val ui = File("src/main/java/com/openminis/app/ui/settings/AltivGitHubPatSheet.kt").readText()
        assertTrue(ui.contains("PasswordVisualTransformation()"))
        assertTrue(ui.contains("vault.add(AltivGitHubPatConfig.ENV_NAME, chosen"))
        assertTrue(ui.contains("vault.update(entry.id, AltivGitHubPatConfig.ENV_NAME"))
        assertTrue(ui.contains("AltivGitHubPatVerifier.check(chosen)"))
        assertTrue(ui.contains("MCPOAuthController(context).purge"))
        assertFalse(ui.contains("headers = mapOf(\"Authorization\" to \"Bearer \$token\")"))
        val screen = File("src/main/java/com/openminis/app/ui/settings/MCPIntegrationsScreen.kt").readText()
        assertTrue(screen.contains("AltivGitHubPatSheet("))
        assertTrue(screen.contains("BuildConfig.FLAVOR == \"altiv\""))
    }

    @Test fun altivResourcesDoNotPresentMinisAsBrand() {
        for (locale in listOf("values", "values-pt-rBR")) {
            val xml = File("src/altiv/res/$locale/strings.xml").readText()
            val texts = Regex("""<string\s+name="[^"]+"[^>]*>([\s\S]*?)</string>""")
                .findAll(xml).map { it.groupValues[1] }.toList()
            assertFalse(locale, texts.any { it.contains("minis-model-use") })
            assertFalse(locale, texts.any { it.contains("Boas-vindas ao Minis") })
            assertFalse(locale, texts.any { it.contains("Welcome to Minis") })
        }
    }
}
