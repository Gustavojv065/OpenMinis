package com.openminis.app.ui.chat

import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * Temporary loopback-only HTTP preview for a user-approved, single-file
 * index.html. Does NOT execute shell/npm, and never exposes files or the
 * embedded HTTP server to the LAN. Stops with its preview dialog.
 */
internal class AltivOfflinePreviewServer(private val project: File) : AutoCloseable {
    private val server = ServerSocket(0, 4, InetAddress.getByName("127.0.0.1"))
    @Volatile private var closed = false
    val url: String = "http://127.0.0.1:" + server.localPort + "/"

    private val worker = Thread({
        while (!closed) {
            try {
                val client = server.accept()
                client.use { respond(it) }
            } catch (_: SocketException) {
                if (closed) break
            } catch (_: Exception) {
                if (closed) break
            }
        }
    }, "altiv-html-preview").apply {
        isDaemon = true
        start()
    }

    private fun respond(client: Socket) {
        client.soTimeout = 2000
        val reader = client.getInputStream().bufferedReader(Charsets.ISO_8859_1)
        val firstLine = reader.readLine()?.take(512).orEmpty()
        var count = 0
        while (count++ < 30) {
            val header = reader.readLine() ?: break
            if (header.isEmpty()) break
        }
        val valid = firstLine.startsWith("GET / HTTP/1.") ||
            firstLine.startsWith("GET /index.html HTTP/1.")
        val page = if (valid) {
            AltivHtmlProjectPreview.lockedPreviewHtml(project.readText(Charsets.UTF_8))
        } else "Not found"
        val bytes = page.toByteArray(Charsets.UTF_8)
        val status = if (valid) "200 OK" else "404 Not Found"
        val contentType = if (valid) "text/html" else "text/plain"
        val headers = "HTTP/1.1 " + status + "\r\n" +
            "Content-Type: " + contentType + "; charset=utf-8\r\n" +
            "Content-Length: " + bytes.size + "\r\n" +
            "Content-Security-Policy: default-src 'none'; img-src https: data:; " +
            "style-src 'unsafe-inline'; script-src 'none'; connect-src 'none'; " +
            "object-src 'none'; frame-src 'none'; form-action 'none'\r\n" +
            "X-Content-Type-Options: nosniff\r\n" +
            "Cache-Control: no-store\r\n" +
            "Connection: close\r\n\r\n"
        client.getOutputStream().apply {
            write(headers.toByteArray(Charsets.ISO_8859_1))
            write(bytes)
            flush()
        }
    }

    override fun close() {
        closed = true
        runCatching { server.close() }
    }
}
