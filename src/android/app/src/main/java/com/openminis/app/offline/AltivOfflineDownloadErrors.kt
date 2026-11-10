package com.openminis.app.offline

import android.app.DownloadManager

/** Human-readable diagnostics from the Android DownloadManager reason column. */
internal object AltivOfflineDownloadErrors {
    fun describe(reason: Int, paused: Boolean): String = when {
        paused && reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK ->
            "Aguardando conexão. Confira Wi-Fi, dados móveis e modo avião."
        paused && reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI ->
            "Download aguardando Wi-Fi. Desative 'Somente Wi-Fi' se quiser usar dados móveis."
        paused && reason == DownloadManager.PAUSED_WAITING_TO_RETRY ->
            "Falha temporária de rede. O Android tentará novamente."
        paused && reason == DownloadManager.PAUSED_UNKNOWN ->
            "Download pausado pelo Android. Confira bateria, rede e armazenamento."
        reason == DownloadManager.ERROR_INSUFFICIENT_SPACE ->
            "Armazenamento insuficiente para receber o modelo."
        reason == DownloadManager.ERROR_CANNOT_RESUME ->
            "O servidor não permitiu retomar o download. Exclua e tente novamente."
        reason == DownloadManager.ERROR_TOO_MANY_REDIRECTS ->
            "O endereço de download teve redirecionamentos demais. Use 'Abrir no navegador'."
        reason == DownloadManager.ERROR_HTTP_DATA_ERROR ->
            "Erro de transferência HTTP. Tente outra rede ou baixe pelo navegador."
        reason == DownloadManager.ERROR_DEVICE_NOT_FOUND ->
            "Armazenamento do aparelho indisponível."
        reason == DownloadManager.ERROR_FILE_ERROR ->
            "Não foi possível gravar o arquivo no armazenamento do aparelho."
        reason in 400..499 ->
            "Servidor recusou o download (HTTP $reason). Verifique o link no navegador."
        reason in 500..599 ->
            "Servidor temporariamente indisponível (HTTP $reason). Tente novamente mais tarde."
        else -> "Falha do gerenciador Android (código $reason). " +
            "Use 'Abrir no navegador' e depois 'Importar GGUF'."
    }
}
