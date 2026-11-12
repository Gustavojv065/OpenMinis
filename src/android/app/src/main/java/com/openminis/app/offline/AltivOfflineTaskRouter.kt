package com.openminis.app.offline

import java.text.Normalizer
import java.util.Locale

/**
 * Deterministic, on-device intent router. A catalog entry is not an engine.
 * Image/video generation cannot run in the current CPU-only text GGUF runtime.
 * Routing never performs hidden paid API requests or shell/tool execution.
 */
internal object AltivOfflineTaskRouter {
    enum class Task { TEXT, CODE, IMAGE, VIDEO, AUDIO }

    private fun normalize(value: String): String = Normalizer.normalize(
        value.lowercase(Locale.ROOT), Normalizer.Form.NFD
    ).replace(Regex("\\p{Mn}+"), "")

    fun classify(message: String): Task {
        val s = normalize(message)
        // "Site com fotos" must NOT be treated as image generation.
        // Explicit media generation takes priority even if the asset is for
        // a website (for example: "gere uma imagem para meu site").
        val mediaVerb = "(?:crie|criar|gere|gerar|produza|produzir|desenhe|faca|renderize)"
        if (Regex("\\b$mediaVerb\\b.{0,55}\\b(?:video|clipe|filme)\\b").containsMatchIn(s))
            return Task.VIDEO
        if (Regex("\\b$mediaVerb\\b.{0,55}\\b(?:imagem|foto|ilustracao|desenho)\\b")
                .containsMatchIn(s)) return Task.IMAGE
        val programming = listOf(
            "landing page", "pagina web", "html", "css", "javascript",
            "typescript", "react", "codigo", "program", "frontend", "backend",
            "corrigir bug", "arquivo index"
        ).any { s.contains(it) } || Regex("\\b(?:site|sites|app|aplicativo|api)\\b")
            .containsMatchIn(s) || s.contains("crie um sistema")
        if (programming) return Task.CODE
        val creation = listOf(
            "crie", "criar", "gere", "gerar", "produza", "produzir",
            "desenhe", "faca", "renderize", "monte"
        ).any { s.contains(it) }
        if (!creation) return Task.TEXT
        if (listOf("video", "clipe", "animacao cinematografica", "filme").any { s.contains(it) })
            return Task.VIDEO
        if (listOf("imagem", "foto", "ilustracao", "desenho", "arte visual").any { s.contains(it) })
            return Task.IMAGE
        if (listOf("audio", "narracao", "voz", "musica").any { s.contains(it) })
            return Task.AUDIO
        return Task.TEXT
    }

    fun isRunnable(task: Task): Boolean = task == Task.TEXT || task == Task.CODE

    fun unsupportedMessage(task: Task): String = when (task) {
        Task.IMAGE -> "Identifiquei um pedido de geração de imagem. " +
            "O motor offline de imagens ainda não está instalado nem integrado ao ALTIV. " +
            "A lista de modelos do catálogo é para descoberta. Você pode pesquisar fotos " +
            "reais usando a opção web, ou selecionar manualmente uma IA online com geração " +
            "de imagem. Não foi produzida nenhuma imagem."
        Task.VIDEO -> "Identifiquei um pedido de geração de vídeo. " +
            "O motor offline de vídeo ainda não está integrado neste Android. " +
            "Posso ajudar a preparar o roteiro com o Qwen ou você pode selecionar " +
            "manualmente um provedor online que aceite vídeo. Nenhum vídeo foi gerado."
        Task.AUDIO -> "Identifiquei um pedido de voz ou áudio. " +
            "O motor offline de síntese de áudio ainda não está integrado. " +
            "Use um provedor compatível ou solicite um roteiro em texto. " +
            "Nenhum arquivo de áudio foi gerado."
        else -> ""
    }

    /**
     * Only actual downloaded GGUFs compatible with the current text runtime.
     * The caller must re-check available RAM before inference starts.
     */
    fun chooseTextModel(
        current: AltivOfflineModel,
        available: List<AltivOfflineModel>,
        task: Task,
        availableMemoryBytes: Long,
        totalDeviceRamGb: Int,
    ): AltivOfflineModel {
        val ready = available.filter {
            it.category == AltivOfflineCategory.TEXT &&
                it.codingLevel > 0 &&
                it.minimumDeviceRamGb <= totalDeviceRamGb &&
                it.sizeBytes + 350L * AltivOfflineModel.ONE_MB < availableMemoryBytes
        }
        if (ready.isEmpty()) return current
        return if (task == Task.CODE) {
            ready.maxWithOrNull(compareBy<AltivOfflineModel> { it.codingLevel }
                .thenBy { -it.sizeBytes }) ?: current
        } else if (current in ready) current
        else ready.minByOrNull { it.sizeBytes } ?: current
    }
}
