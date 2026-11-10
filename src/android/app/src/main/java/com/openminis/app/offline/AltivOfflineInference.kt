package com.openminis.app.offline

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A small, manual, ON-DEVICE smoke test for downloaded GGUF models.
 * No MCP/Shell/Git calls. It never edits project files.
 * The CPU-only free AAR has no tool-calling or token streaming.
 */
internal class AltivOfflineInference(context: Context) {
    private val app = context.applicationContext
    private val downloads = AltivOfflineModelDownloads(app)

    data class Output(
        val text: String,
        val elapsedMs: Long,
        val tokensPerSecond: Double,
    )

    companion object {
        private val inferenceMutex = Mutex()
        private const val GB = 1_073_741_824L

        fun totalRamGb(context: Context): Int {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            return ((info.totalMem + GB - 1) / GB).toInt()
        }
    }

    suspend fun complete(model: AltivOfflineModel, prompt: String): Output =
        withContext(Dispatchers.Default) {
            require(prompt.isNotBlank() && prompt.length <= AltivOfflineConversationPrompt.MAX_PROMPT_CHARS) {
                "O histórico da conversa excede o limite de contexto local."
            }
            check(downloads.state(model).phase == AltivOfflineModelDownloads.Phase.READY) {
                "Baixe e valide o arquivo GGUF primeiro."
            }
            check(model.codingLevel >= 1) { "Este modelo não é indicado para gerar texto." }
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            check(totalRamGb(app) >= model.minimumDeviceRamGb) {
                "Este celular tem pouca RAM para o modelo selecionado. " +
                    "Escolha outro menor."
            }
            // Conservative free-memory check; model weights + KV cache/compute.
            val required = model.sizeBytes + 350L * AltivOfflineModel.ONE_MB
            check(info.availMem > required) {
                "Memória livre insuficiente para iniciar. Feche outros apps " +
                    "ou baixe um modelo menor."
            }
            inferenceMutex.withLock {
                val started = SystemClock.elapsedRealtime()
                val modelPath = downloads.modelFile(model).absolutePath
                var modelHandle: LlamaModel? = null
                try {
                    val handle = Llama.loadModel(
                        modelPath = modelPath,
                        config = LlamaConfig(contextSize = 1536, threads = 3),
                    )
                    modelHandle = handle
                    val result = Llama.complete(
                        model = handle,
                        prompt = prompt,
                        systemPrompt = "Você é o ALTIV DEV AI offline. Responda em português " +
                            "brasileiro com código correto e objetivo quando solicitado. " +
                            "Não afirme que executou testes, Shell, Git ou modificou arquivos. " +
                            "Se o pedido exigir isso, explique que é preciso o agente com ferramentas.",
                        maxTokens = 384,
                    )
                    Output(
                        text = result.text,
                        elapsedMs = SystemClock.elapsedRealtime() - started,
                        tokensPerSecond = result.tokensPerSecond.toDouble(),
                    )
                } finally {
                    modelHandle?.let { Llama.releaseModel(it) }
                }
            }
        }
}
