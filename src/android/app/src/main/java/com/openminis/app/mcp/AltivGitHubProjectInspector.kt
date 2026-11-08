package com.openminis.app.mcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Read-only repository signals; no claim of an active deployment or account login. */
internal data class AltivGitHubProjectSignals(
    val repository: String,
    val lovable: Boolean,
    val vercel: Boolean,
    val supabase: Boolean,
) {
    fun labels(): List<String> = buildList {
        if (lovable) add("Lovable (.lovable/project.json)")
        if (vercel) add("Vercel (arquivo de configuração)")
        if (supabase) add("Supabase (supabase/config.toml)")
    }
}

/**
 * Checks presence of repository configuration files through the authorized
 * GitHub Contents API. Does not clone the repository, log into Vercel/Lovable,
 * read file bodies, or initiate a deployment. Requires Contents: Read.
 */
internal class AltivGitHubProjectInspector(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS).build(),
    private val apiBase: String = "https://api.github.com/",
) {
    private val repoName = Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")

    suspend fun inspect(token: String, fullName: String): AltivGitHubProjectSignals =
        withContext(Dispatchers.IO) {
            if (token.isBlank()) throw AltivGitHubAccessException("Token GitHub ausente.")
            if (!repoName.matches(fullName) || fullName.contains("..")) {
                throw AltivGitHubAccessException("Nome de repositório inválido.")
            }
            val base = apiBase.trimEnd('/') + "/repos/" + fullName + "/contents/"
            val lovable = fileExists(base + ".lovable/project.json", token)
            val vercel = fileExists(base + "vercel.json", token) ||
                fileExists(base + ".vercel/project.json", token)
            val supabase = fileExists(base + "supabase/config.toml", token)
            AltivGitHubProjectSignals(fullName, lovable, vercel, supabase)
        }

    private fun fileExists(url: String, token: String): Boolean {
        val req = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("Cache-Control", "no-store")
            .get().build()
        try {
            http.newCall(req).execute().use { response ->
                return when (response.code) {
                    200 -> true
                    404 -> false
                    401 -> throw AltivGitHubAccessException("Token inválido ou revogado.")
                    403 -> throw AltivGitHubAccessException(
                        "Sem permissão para inspecionar arquivos. " +
                            "Ative Contents: Read para o repositório selecionado."
                    )
                    else -> throw AltivGitHubAccessException(
                        "Falha na verificação dos serviços (HTTP " + response.code + ")."
                    )
                }
            }
        } catch (e: AltivGitHubAccessException) {
            throw e
        } catch (_: Exception) {
            // Do not report user token, URL parameters or HTTP body.
            throw AltivGitHubAccessException("Falha de conexão ao inspecionar o projeto.")
        }
    }
}
