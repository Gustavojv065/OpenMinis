package com.openminis.app.offline

/**
 * Phase 1, curated local GGUF downloads. No inference is exposed until an
 * Android-compatible runtime is integrated and validated on real devices.
 * Model size is approximate; final file is verified after DownloadManager success.
 */
internal enum class AltivOfflineCategory(val label: String) {
    TEXT("Texto"),
    VISION("Visão"),
    SPEECH("Voz"),
    IMAGES("Imagens"),
    EMBEDDINGS("Embeddings"),
    MUSIC("Música"),
}

internal data class AltivOfflineModel(
    val id: String,
    val name: String,
    val subtitle: String,
    val category: AltivOfflineCategory,
    val quantization: String,
    val sizeBytes: Long,
    val ramGuidance: String,
    val fileName: String,
    val downloadUrl: String,
    val sourceUrl: String,
    val license: String,
) {
    companion object {
        const val ONE_MB = 1_000_000L
    }

    /** No arbitrary URLs from chat or remote catalog are downloaded in phase 1. */
    fun hasTrustedSource(): Boolean =
        downloadUrl.startsWith("https://huggingface.co/") &&
            downloadUrl.contains("/resolve/main/") &&
            downloadUrl.endsWith("/" + fileName) &&
            fileName.matches(Regex("[a-zA-Z0-9_.-]+[.]gguf")) &&
            id.matches(Regex("[a-z0-9-]+"))
}

internal object AltivOfflineModelCatalog {
    val models: List<AltivOfflineModel> = listOf(
        AltivOfflineModel(
            id = "smollm2-360m-q4km",
            name = "SmolLM2 360M Instruct",
            subtitle = "Leve para testes de conversa e geração simples de texto",
            category = AltivOfflineCategory.TEXT,
            quantization = "GGUF Q4_K_M",
            sizeBytes = 271 * AltivOfflineModel.ONE_MB,
            ramGuidance = "Recomendado: 3 GB+ de RAM disponível",
            fileName = "SmolLM2-360M-Instruct-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf",
            sourceUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF",
            license = "Apache-2.0",
        ),
        AltivOfflineModel(
            id = "qwen3-06b-q4km",
            name = "Qwen3 0.6B",
            subtitle = "Mais capacidade que o modelo menor, ainda limitado para projetos complexos",
            category = AltivOfflineCategory.TEXT,
            quantization = "GGUF Q4_K_M",
            sizeBytes = 484 * AltivOfflineModel.ONE_MB,
            ramGuidance = "Recomendado: 4 GB+ de RAM disponível",
            fileName = "Qwen3-0.6B-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/tensorblock/Qwen_Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B-Q4_K_M.gguf",
            sourceUrl = "https://huggingface.co/tensorblock/Qwen_Qwen3-0.6B-GGUF",
            license = "Apache-2.0 (pesos Qwen3; confira a página do modelo)",
        ),
    )

    fun inCategory(category: AltivOfflineCategory): List<AltivOfflineModel> =
        models.filter { it.category == category }

    fun byId(id: String): AltivOfflineModel? = models.firstOrNull { it.id == id }
}
