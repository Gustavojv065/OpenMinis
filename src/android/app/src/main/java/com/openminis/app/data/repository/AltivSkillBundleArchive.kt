package com.openminis.app.data.repository

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Recognizes a transport ZIP containing multiple individual skill ZIPs.
 *
 * ALTIV-41-SKILLS-DESCOMPACTAR-PRIMEIRO.zip contains:
 *   00-LEIA-PRIMEIRO.txt
 *   IMPORTAR-UM-POR-VEZ/<skill-name>.zip
 *
 * Never execute the archive contents. Keep bounded in memory; a single-skill
 * archive is handled by the existing importer unchanged.
 */
internal object AltivSkillBundleArchive {
    internal data class Item(val fileName: String, val archive: ByteArray)

    private const val MAX_BUNDLED_SKILLS = 80
    private const val MAX_ENTRY_BYTES = 2 * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 12 * 1024 * 1024

    /** Null means "not a multi-skill package", so callers can use legacy flow. */
    fun collect(input: InputStream): List<Item>? {
        val found = mutableListOf<Item>()
        var totalBytes = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.replace('\\', '/')
                if (!safePath(name)) throw IllegalArgumentException("Arquivo ZIP contém caminho inválido.")
                if (!entry.isDirectory && name.endsWith(".zip", ignoreCase = true)) {
                    if (found.size >= MAX_BUNDLED_SKILLS) {
                        throw IllegalArgumentException("O limite por pacote é de 80 skills.")
                    }
                    val bytes = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    while (true) {
                        val count = zip.read(chunk)
                        if (count < 0) break
                        if (count == 0) continue
                        totalBytes += count
                        if (bytes.size() + count > MAX_ENTRY_BYTES || totalBytes > MAX_TOTAL_BYTES) {
                            throw IllegalArgumentException("Pacote de skills muito grande.")
                        }
                        bytes.write(chunk, 0, count)
                    }
                    found.add(Item(name, bytes.toByteArray()))
                } else {
                    // Skip instruction files without trusting them as commands.
                    val chunk = ByteArray(8192)
                    while (true) {
                        val count = zip.read(chunk)
                        if (count < 0) break
                        if (count == 0) continue
                        totalBytes += count
                        if (totalBytes > MAX_TOTAL_BYTES) {
                            throw IllegalArgumentException("Pacote de skills muito grande.")
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        return found.takeIf { it.size >= 2 }
    }

    /** Also reject unsafe paths before passing each inner archive to the legacy importer. */
    fun safeInnerArchive(bytes: ByteArray): Boolean {
        var files = 0
        var total = 0
        var hasSkillMd = false
        return try {
            ZipInputStream(bytes.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (++files > 100 || !safePath(entry.name)) return false
                    if (entry.name == "SKILL.md" ||
                        (entry.name.endsWith("/SKILL.md") && entry.name.count { it == '/' } == 1)
                    ) hasSkillMd = true
                    val chunk = ByteArray(8192)
                    while (true) {
                        val count = zip.read(chunk)
                        if (count < 0) break
                        if (count == 0) continue
                        total += count
                        if (total > MAX_ENTRY_BYTES) return false
                    }
                    zip.closeEntry()
                }
            }
            hasSkillMd
        } catch (_: Exception) {
            false
        }
    }

    private fun safePath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith("/") &&
            !path.contains(':') && !path.contains('\\') &&
            path.split('/').none { it == ".." || it == "." }
}
