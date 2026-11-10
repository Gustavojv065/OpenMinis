package com.openminis.app.offline

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android DownloadManager owns downloads (notification, OS-managed Wi-Fi policy).
 * Files remain private to the app's external-files area and are deleted when
 * the app is uninstalled. Nothing is copied into the embedded Linux rootfs.
 */
internal class AltivOfflineModelDownloads(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = appContext.getSharedPreferences("altiv_offline_downloads", Context.MODE_PRIVATE)

    internal enum class Phase { NOT_DOWNLOADED, QUEUED, DOWNLOADING, PAUSED, READY, FAILED }
    internal data class State(
        val phase: Phase,
        val downloadedBytes: Long = 0,
        val totalBytes: Long = -1,
        val message: String? = null,
    )

    private fun downloadsRoot(): File =
        requireNotNull(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)) {
            "Armazenamento externo do aplicativo indisponível."
        }.resolve("altiv-models")

    fun modelFile(model: AltivOfflineModel): File {
        require(model.hasTrustedSource()) { "Modelo não autorizado no catálogo." }
        return downloadsRoot().resolve(model.fileName)
    }

    fun freeSpaceBytes(): Long = runCatching {
        val folder = downloadsRoot().apply { mkdirs() }
        StatFs(folder.absolutePath).availableBytes
    }.getOrDefault(0)

    private fun prefKey(model: AltivOfflineModel): String = "download_" + model.id

    /** Validate at least the actual GGUF signature and reasonable byte size. */
    private fun validGguf(model: AltivOfflineModel, file: File): Boolean =
        runCatching {
            if (!file.isFile || file.length() < model.sizeBytes * 65L / 100L) {
                false
            } else {
                val magic = ByteArray(4)
                val read = FileInputStream(file).use { stream -> stream.read(magic) }
                read == 4 && magic.contentEquals(byteArrayOf(0x47, 0x47, 0x55, 0x46))
            }
        }.getOrDefault(false)

    fun state(model: AltivOfflineModel): State {
        val file = modelFile(model)
        val id = prefs.getLong(prefKey(model), -1L)
        if (id < 0) {
            return if (validGguf(model, file)) State(Phase.READY) else State(Phase.NOT_DOWNLOADED)
        }
        val query = DownloadManager.Query().setFilterById(id)
        val snapshot = runCatching {
            manager.query(query)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val phase = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val received = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                )
                val total = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                )
                val reason = cursor.getInt(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                )
                when (phase) {
                    DownloadManager.STATUS_PENDING -> State(Phase.QUEUED, received, total)
                    DownloadManager.STATUS_RUNNING -> State(Phase.DOWNLOADING, received, total)
                    DownloadManager.STATUS_PAUSED -> State(
                        Phase.PAUSED, received, total,
                        AltivOfflineDownloadErrors.describe(reason, paused = true)
                    )
                    DownloadManager.STATUS_SUCCESSFUL ->
                        if (validGguf(model, file)) State(Phase.READY, received, total)
                        else State(Phase.FAILED, received, total,
                            "Arquivo inválido ou incompleto. Exclua e tente novamente.")
                    DownloadManager.STATUS_FAILED -> State(
                        Phase.FAILED, received, total,
                        AltivOfflineDownloadErrors.describe(reason, paused = false)
                    )
                    else -> State(Phase.FAILED, received, total,
                        "Estado de download desconhecido (código $phase).")
                }
            }
        }.getOrNull()
        return snapshot ?: if (validGguf(model, file)) State(Phase.READY)
            else State(Phase.FAILED, message = "Download não encontrado. Exclua e tente novamente.")
    }

    fun start(model: AltivOfflineModel, wifiOnly: Boolean): Result<Long> = runCatching {
        require(model.hasTrustedSource()) { "Fonte do modelo não permitida." }
        require(state(model).phase == Phase.NOT_DOWNLOADED) {
            "Exclua o download anterior antes de tentar novamente."
        }
        val root = downloadsRoot()
        check(root.exists() || root.mkdirs()) { "Não foi possível criar pasta de modelos." }
        val reserve = 150L * AltivOfflineModel.ONE_MB
        check(freeSpaceBytes() >= model.sizeBytes + reserve) {
            "Espaço insuficiente. Libere armazenamento antes de baixar."
        }
        val file = modelFile(model)
        check(!file.exists()) { "Há um arquivo incompleto. Exclua antes de reiniciar." }
        val request = DownloadManager.Request(Uri.parse(model.downloadUrl))
            .setTitle("ALTIV DEV AI · " + model.name)
            .setDescription("Modelo GGUF — após baixar, use Testar offline")
            .setMimeType("application/octet-stream")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setAllowedOverMetered(!wifiOnly)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(
                appContext, Environment.DIRECTORY_DOWNLOADS,
                "altiv-models/" + model.fileName,
            )
        val id = manager.enqueue(request)
        prefs.edit().putLong(prefKey(model), id).apply()
        id
    }

    /**
     * Import a GGUF chosen via the Android file picker, without executing any code.
     * Stream to a .part file so a failed import never looks like an installed model.
     * Limit disk usage and verify the GGUF magic/expected size before final rename.
     */
    suspend fun importFromUri(model: AltivOfflineModel, uri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(uri.scheme == "content") {
                    "Selecione um arquivo pelo gerenciador de documentos do Android."
                }
                require(model.hasTrustedSource()) { "Modelo não permitido." }
                check(state(model).phase !in setOf(Phase.QUEUED, Phase.DOWNLOADING, Phase.PAUSED)) {
                    "Cancele o download em andamento antes de importar."
                }
                val sizeLimit = model.sizeBytes * 115L / 100L + 20_000_000L
                val minimum = model.sizeBytes * 80L / 100L
                check(freeSpaceBytes() >= sizeLimit + 100_000_000L) {
                    "Espaço insuficiente para copiar o GGUF. Libere armazenamento."
                }
                val root = downloadsRoot().apply { check(exists() || mkdirs()) }
                val part = root.resolve(model.fileName + ".part")
                check(!part.exists()) { "Importação anterior incompleta. Exclua e tente novamente." }
                try {
                    val source = appContext.contentResolver.openInputStream(uri)
                        ?: error("Não foi possível abrir o arquivo selecionado.")
                    source.use { input ->
                        FileOutputStream(part).use { output ->
                            val buffer = ByteArray(128 * 1024)
                            var count = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                count += read
                                check(count <= sizeLimit) {
                                    "Arquivo maior que o esperado para o modelo escolhido."
                                }
                                output.write(buffer, 0, read)
                            }
                            output.fd.sync()
                            check(count >= minimum) {
                                "Arquivo menor que o esperado. Escolha o GGUF completo."
                            }
                        }
                    }
                    check(validGguf(model, part)) {
                        "Arquivo não reconhecido como GGUF válido para este modelo."
                    }
                    // A previous failed managed download must not overwrite the import.
                    remove(model)
                    val destination = modelFile(model)
                    check(part.renameTo(destination)) {
                        "Falha ao concluir a importação no armazenamento do aplicativo."
                    }
                } finally {
                    if (part.exists()) part.delete()
                }
            }
        }

    fun remove(model: AltivOfflineModel) {
        prefs.getLong(prefKey(model), -1L).takeIf { it >= 0 }?.let { id ->
            manager.remove(id)
        }
        // This is an app-private path generated from the curated catalog.
        runCatching { modelFile(model).delete() }
        prefs.edit().remove(prefKey(model)).apply()
    }
}
