package com.openminis.app.offline

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.io.FileInputStream

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
            if (!file.isFile || file.length() < model.sizeBytes * 65L / 100L) return false
            val magic = ByteArray(4)
            FileInputStream(file).use { stream -> stream.read(magic) }
            magic.contentEquals(byteArrayOf(0x47, 0x47, 0x55, 0x46))
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
                when (phase) {
                    DownloadManager.STATUS_PENDING -> State(Phase.QUEUED, received, total)
                    DownloadManager.STATUS_RUNNING -> State(Phase.DOWNLOADING, received, total)
                    DownloadManager.STATUS_PAUSED -> State(
                        Phase.PAUSED, received, total, "Aguardando rede permitida ou Wi-Fi."
                    )
                    DownloadManager.STATUS_SUCCESSFUL ->
                        if (validGguf(model, file)) State(Phase.READY, received, total)
                        else State(Phase.FAILED, received, total,
                            "Arquivo inválido ou incompleto. Exclua e tente novamente.")
                    else -> State(Phase.FAILED, received, total,
                        "Download falhou. Exclua e tente novamente.")
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
            .setDescription("Modelo GGUF — instalação local ainda não ativada")
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

    fun remove(model: AltivOfflineModel) {
        prefs.getLong(prefKey(model), -1L).takeIf { it >= 0 }?.let { id ->
            manager.remove(id)
        }
        // This is an app-private path generated from the curated catalog.
        runCatching { modelFile(model).delete() }
        prefs.edit().remove(prefKey(model)).apply()
    }
}
