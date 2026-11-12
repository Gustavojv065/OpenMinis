package com.openminis.app.offline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

/**
 * Opt-in, source-limited web research for the on-device GGUF.
 *
 * No API keys or cloud model calls. Only the user's search words reach
 * public Wikipedia/Openverse endpoints. The service never executes tools,
 * visits user-supplied URLs, downloads binaries or edits project files.
 * References are treated as untrusted data, not instructions to the model.
 */
internal object AltivOfflineWebReferences {
    internal data class Reference(
        val label: String,
        val pageUrl: String,
        val detail: String,
        val mediaUrl: String? = null,
    )

    internal data class Result(
        val references: List<Reference>,
        val attempted: Boolean = true,
    ) {
        val promptContext: String get() = references.take(4).joinToString("\n") { ref ->
            "- " + plain(ref.label, 72) + ": " + plain(ref.detail, 110)
        }.take(600)

        val markdownSources: String get() = if (references.isEmpty()) {
            "\n\n_Pesquisa web ativada, mas não foram encontradas referências nesta consulta._"
        } else {
            "\n\n**Referências públicas consultadas (confira autoria e licença):**\n" +
                references.take(5).joinToString("\n") { ref ->
                    "- [" + markdown(ref.label, 65) + "](" + ref.pageUrl + ")" +
                        " — " + markdown(ref.detail, 110) +
                        (ref.mediaUrl?.let { " · [Abrir imagem](" + it + ")" } ?: "")
                }
        }
    }

    private val triggers = listOf(
        "pesquis", "buscar", "busque", "procure", "internet", "online",
        "foto", "imagem", "imagens", "fotografia", "referencia", "inspiracao",
        "exemplo de site", "sites parecidos", "landing page", "pet shop"
    )
    private val imageTerms = listOf(
        "foto", "imagem", "fotografia", "cachorro", "gato",
        "animal", "pet shop", "ilustracao", "banner"
    )

    internal fun normalized(value: String): String = Normalizer.normalize(
        value.lowercase(Locale.ROOT), Normalizer.Form.NFD,
    ).replace(Regex("\\p{Mn}+"), "")

    fun shouldResearch(message: String): Boolean {
        val clean = normalized(message)
        return triggers.any { clean.contains(it) }
    }

    private fun wantsImages(message: String): Boolean {
        val clean = normalized(message)
        return imageTerms.any { clean.contains(it) }
    }

    internal fun searchTerm(message: String): String {
        val ignores = setOf(
            "crie", "criar", "quero", "para", "faca", "com", "uma", "umas", "uns",
            "das", "dos", "que", "onde", "pode", "tambem", "pesquise", "busque",
            "buscar", "foto", "fotos", "imagem", "imagens", "site", "sites",
            "reais", "referencia", "referencias", "exemplos", "meu", "minha",
            "preciso", "pagina", "landing", "page", "html", "css", "javascript"
        )
        val words = Regex("[a-z0-9]{3,}").findAll(normalized(message))
            .map { it.value }.filter { it !in ignores }.take(7).toList()
        return words.joinToString(" ").ifBlank { "design fotografia" }.take(85)
    }

    suspend fun search(message: String): Result = withContext(Dispatchers.IO) {
        val term = searchTerm(message)
        val references = mutableListOf<Reference>()
        if (wantsImages(message)) {
            runCatching { imageSearch(term) }.getOrDefault(emptyList())
                .let { references.addAll(it) }
        }
        runCatching { encyclopediaSearch(term) }.getOrDefault(emptyList())
            .let { references.addAll(it) }
        Result(references.take(5))
    }

    // Exact hosts; no arbitrary website fetch, redirects or in-app web scraping.
    private fun getJson(uri: String, expectedHost: String): JSONObject {
        val url = URL(uri)
        require(url.protocol == "https" && url.host == expectedHost)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 6_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "ALTIV-DEV-AI/1.0 (educational references)")
        connection.setRequestProperty("Accept", "application/json")
        try {
            check(connection.responseCode == 200) { "Pesquisa web indisponível." }
            val body = StringBuilder()
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(4096)
                while (body.length < 96_000) {
                    val count = reader.read(buffer, 0, minOf(buffer.size, 96_000 - body.length))
                    if (count < 0) break
                    body.append(buffer, 0, count)
                }
            }
            return JSONObject(body.toString())
        } finally {
            connection.disconnect()
        }
    }

    private fun imageSearch(term: String): List<Reference> {
        // Openverse's anonymous image endpoint has rate limits and provides
        // attribution/license. A link is not a commercial use guarantee.
        val q = URLEncoder.encode(term, "UTF-8")
        val json = getJson(
            "https://api.openverse.org/v1/images/?q=$q&page_size=3&mature=false",
            "api.openverse.org",
        )
        val items = json.optJSONArray("results") ?: return emptyList()
        return (0 until minOf(items.length(), 3)).mapNotNull { i ->
            val item = items.optJSONObject(i) ?: return@mapNotNull null
            val id = item.optString("id")
            val page = item.optString("foreign_landing_url")
                .takeIf(::safeHttpUrl) ?: id.takeIf { it.matches(Regex("[a-fA-F0-9-]{36}")) }
                    ?.let { "https://openverse.org/image/$it" } ?: return@mapNotNull null
            val license = item.optString("license").ifBlank { "licença não informada" }
            val creator = item.optString("creator").ifBlank { "autor não identificado" }
            Reference(
                label = item.optString("title").ifBlank { "Fotografia $i" },
                pageUrl = page,
                detail = "Foto; autor: $creator; licença: $license",
                mediaUrl = item.optString("url").takeIf(::safeHttpUrl),
            )
        }
    }

    private fun encyclopediaSearch(term: String): List<Reference> {
        val q = URLEncoder.encode(term, "UTF-8")
        val json = getJson(
            "https://pt.wikipedia.org/w/rest.php/v1/search/page?q=$q&limit=2",
            "pt.wikipedia.org",
        )
        val pages = json.optJSONArray("pages") ?: return emptyList()
        return (0 until minOf(pages.length(), 2)).mapNotNull { i ->
            val item = pages.optJSONObject(i) ?: return@mapNotNull null
            val key = item.optString("key").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val url = "https://pt.wikipedia.org/wiki/" +
                URLEncoder.encode(key, "UTF-8").replace("+", "%20")
            Reference(
                label = item.optString("title").ifBlank { key },
                pageUrl = url,
                detail = plain(item.optString("excerpt"), 160),
            )
        }
    }

    private fun safeHttpUrl(value: String): Boolean =
        runCatching {
            val url = URL(value)
            url.protocol == "https" && url.host.isNotBlank() &&
                !url.host.equals("localhost", true) &&
                !url.host.endsWith(".local", true) &&
                !url.host.matches(Regex("[0-9.]+"))
        }.getOrDefault(false)

    private fun plain(value: String, max: Int): String = value
        .replace(Regex("<[^>]*>"), " ")
        .replace(Regex("[\\r\\n\\t\\p{Cc}]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim().take(max)

    private fun markdown(value: String, max: Int): String = plain(value, max)
        .replace("[", "\\[").replace("]", "\\]")
        .replace("(", "\\(").replace(")", "\\)")
}
