package com.openminis.app.mcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Only repository names and visibility are retained in memory. Never persist/log the PAT. */
internal data class AltivGitHubRepository(val fullName: String, val isPrivate: Boolean)
internal data class AltivGitHubRepositoryScan(
    val account: String,
    val repositories: List<AltivGitHubRepository>,
    val complete: Boolean,
)
internal class AltivGitHubAccessException(message: String) : Exception(message)

/**
 * A successful GET /user alone does not show which repos a PAT can access.
 * This tests REST authentication and lists the token-accessible repositories.
 * It does not clone a workspace repository or perform an MCP handshake.
 */
internal class AltivGitHubRepositoryAccess(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS).build(),
    private val apiBase: String = "https://api.github.com/",
) {
    private val parser = Json { ignoreUnknownKeys = true }
    private val maxPages = 10
    private val perPage = 100

    suspend fun inspect(token: String): AltivGitHubRepositoryScan = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "Token vazio" }
        try {
            val user = request(apiBase + "user", token)
            val account = parser.parseToJsonElement(user).jsonObject["login"]
                ?.jsonPrimitive?.contentOrNull.orEmpty()
            if (account.isBlank()) {
                throw AltivGitHubAccessException("Não foi possível identificar a conta GitHub.")
            }
            val found = mutableListOf<AltivGitHubRepository>()
            var complete = false
            for (page in 1..maxPages) {
                val response = request(
                    apiBase + "user/repos?per_page=$perPage&type=all&sort=full_name&page=$page",
                    token,
                )
                val entries = parser.parseToJsonElement(response).jsonArray
                for (entry in entries) {
                    val obj = entry.jsonObject
                    val fullName = obj["full_name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (fullName.isNotBlank()) {
                        found += AltivGitHubRepository(
                            fullName = fullName,
                            isPrivate = obj["private"]?.jsonPrimitive?.booleanOrNull ?: false,
                        )
                    }
                }
                if (entries.size < perPage) {
                    complete = true
                    break
                }
            }
            AltivGitHubRepositoryScan(
                account = account,
                repositories = found.distinctBy { it.fullName }.sortedBy { it.fullName.lowercase() },
                complete = complete,
            )
        } catch (e: AltivGitHubAccessException) {
            throw e
        } catch (_: Exception) {
            // Do not expose HTTP diagnostics that might carry the bearer token.
            throw AltivGitHubAccessException(
                "Não foi possível consultar os repositórios do GitHub. Confira a conexão e tente novamente."
            )
        }
    }

    private fun request(url: String, token: String): String {
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("Cache-Control", "no-store")
            .get().build()
        http.newCall(request).execute().use { response ->
            when (response.code) {
                200 -> return response.body?.string()
                    ?: throw AltivGitHubAccessException("O GitHub respondeu sem dados.")
                401 -> throw AltivGitHubAccessException("Token inválido ou revogado.")
                403 -> throw AltivGitHubAccessException(
                    "O GitHub negou acesso. Confira a permissão Metadata (leitura), os repositórios selecionados e a política da organização."
                )
                429 -> throw AltivGitHubAccessException("Limite de consultas do GitHub atingido. Tente novamente.")
                else -> throw AltivGitHubAccessException(
                    "Falha ao consultar repositórios (HTTP " + response.code + ")."
                )
            }
        }
    }
}
