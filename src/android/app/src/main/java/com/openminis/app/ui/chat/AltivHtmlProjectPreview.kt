package com.openminis.app.ui.chat

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import java.io.File
import java.util.UUID

/**
 * An explicit, text-only workflow to create a REAL index.html from a completed
 * code block. No shell, npm, GitHub, network provider, tools or localhost are
 * invoked automatically. The user must tap "Prévia HTML" before any file write.
 */
internal object AltivHtmlProjectPreview {
    private const val MAX_HTML_BYTES = 128_000

    fun isHtmlCandidate(language: String, source: String): Boolean {
        val lang = language.trim().lowercase()
        val head = source.trimStart().take(160).lowercase()
        return lang == "html" || lang == "htm" ||
            head.startsWith("<!doctype html") || head.startsWith("<html")
    }

    fun isComplete(language: String, source: String): Boolean {
        if (!isHtmlCandidate(language, source)) return false
        if (source.toByteArray(Charsets.UTF_8).size > MAX_HTML_BYTES) return false
        val s = source.lowercase()
        return s.contains("<html") && s.contains("</html>") &&
            s.contains("<head") && s.contains("</head>") &&
            s.contains("<body") && s.contains("</body>")
    }

    fun save(context: Context, language: String, source: String): File {
        require(isComplete(language, source)) {
            "O HTML está incompleto. Peça ao modelo para continuar até </html> antes de visualizar."
        }
        val root = File(context.filesDir, "altiv-projects").canonicalFile
        check(root.isDirectory || root.mkdirs()) { "Não foi possível criar a pasta de projetos." }
        val folder = File(root, "site-" + System.currentTimeMillis() +
            "-" + UUID.randomUUID().toString().take(8))
        check(folder.mkdir()) { "Não foi possível criar a pasta de trabalho." }
        val dest = File(folder, "index.html")
        require(dest.canonicalFile.parentFile == folder.canonicalFile)
        dest.writeText(source, Charsets.UTF_8)
        return dest
    }

    // Add a strict policy to the *preview only*. The original index.html stays
    // byte-for-byte intact in the private project. Images may load over HTTPS,
    // but no script, form, iframe, fetch or external stylesheet will execute.
    fun lockedPreviewHtml(source: String): String {
        val csp = """<meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src https: data:; style-src 'unsafe-inline'; script-src 'none'; connect-src 'none'; object-src 'none'; frame-src 'none'; form-action 'none'; base-uri 'none'">"""
        val match = Regex("<head(?:\\s[^>]*)?>", RegexOption.IGNORE_CASE)
            .find(source) ?: return source
        return source.replaceRange(match.range, match.value + csp)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun AltivHtmlProjectPreviewDialog(
    project: File,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val webView = remember(project.absolutePath) {
        WebView(context).apply {
            settings.javaScriptEnabled = false
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.loadsImagesAutomatically = true
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            settings.allowUniversalAccessFromFileURLs = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: android.webkit.WebResourceRequest?
                ): Boolean = true // block navigation outside the preview
            }
            loadDataWithBaseURL(
                "https://altiv-preview.invalid/",
                AltivHtmlProjectPreview.lockedPreviewHtml(project.readText()),
                "text/html", "UTF-8", null,
            )
        }
    }
    DisposableEffect(webView) {
        onDispose { webView.stopLoading(); webView.destroy() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("ALTIV · Prévia local HTML", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Arquivo criado: " + project.name +
                        " · Pasta ALTIV Projects · Sem localhost ou comandos de terminal",
                    style = MaterialTheme.typography.bodySmall,
                )
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 440.dp)
                        .padding(vertical = 4.dp)
                )
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Fechar prévia")
                }
            }
        }
    }
}
