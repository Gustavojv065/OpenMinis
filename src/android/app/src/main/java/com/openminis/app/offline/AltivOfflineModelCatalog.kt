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
    val minimumDeviceRamGb: Int = 4,
    val codingLevel: Int = 0,
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
            id = "qwen3-06b-q4km",
            name = "Qwen3 0.6B",
            subtitle = "Conversa leve, comandos simples e protótipo de inferência",
            category = AltivOfflineCategory.TEXT,
            quantization = "GGUF Q4_K_M",
            sizeBytes = 484L * AltivOfflineModel.ONE_MB,
            ramGuidance = "RAM total recomendada: 4 GB ou mais",
            fileName = "Qwen3-0.6B-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/tensorblock/Qwen_Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B-Q4_K_M.gguf",
            sourceUrl = "https://huggingface.co/tensorblock/Qwen_Qwen3-0.6B-GGUF",
            license = "Apache-2.0",
            minimumDeviceRamGb = 4,
            codingLevel = 1,
        ),
        AltivOfflineModel(
            id = "qwen25coder-15b-q4km",
            name = "Qwen2.5-Coder 1.5B Instruct",
            subtitle = "Código HTML, CSS e ajustes pequenos com baixo consumo de memória",
            category = AltivOfflineCategory.TEXT,
            quantization = "GGUF Q4_K_M",
            sizeBytes = 986L * AltivOfflineModel.ONE_MB,
            ramGuidance = "RAM total recomendada: 4 GB ou mais",
            fileName = "Qwen2.5-Coder-1.5B-Instruct-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/bartowski/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/Qwen2.5-Coder-1.5B-Instruct-Q4_K_M.gguf",
            sourceUrl = "https://huggingface.co/bartowski/Qwen2.5-Coder-1.5B-Instruct-GGUF",
            license = "Apache-2.0",
            minimumDeviceRamGb = 4,
            codingLevel = 2,
        ),
        AltivOfflineModel(
            id = "qwen25coder-3b-q4km",
            name = "Qwen2.5-Coder 3B Instruct",
            subtitle = "Recomendado para programar com mais contexto no celular",
            category = AltivOfflineCategory.TEXT,
            quantization = "GGUF Q4_K_M",
            sizeBytes = 1930L * AltivOfflineModel.ONE_MB,
            ramGuidance = "RAM total recomendada: 6 GB ou mais",
            fileName = "Qwen2.5-Coder-3B-Instruct-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/bartowski/Qwen2.5-Coder-3B-Instruct-GGUF/resolve/main/Qwen2.5-Coder-3B-Instruct-Q4_K_M.gguf",
            sourceUrl = "https://huggingface.co/bartowski/Qwen2.5-Coder-3B-Instruct-GGUF",
            license = "Qwen Research (confira licença antes do uso comercial)",
            minimumDeviceRamGb = 6,
            codingLevel = 3,
        ),
        AltivOfflineModel(
            id = "qwen3-4b-instruct-q4km",
            name = "Qwen3 4B Instruct 2507",
            subtitle = "Interpretação de pedidos e planejamento; memória intermediária",
            category = AltivOfflineCategory.TEXT,
            quantization = "GGUF Q4_K_M",
            sizeBytes = 2500L * AltivOfflineModel.ONE_MB,
            ramGuidance = "RAM total recomendada: 8 GB ou mais",
            fileName = "Qwen3-4B-Instruct-2507-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/bartowski/Qwen_Qwen3-4B-Instruct-2507-GGUF/resolve/main/Qwen3-4B-Instruct-2507-Q4_K_M.gguf",
            sourceUrl = "https://huggingface.co/bartowski/Qwen_Qwen3-4B-Instruct-2507-GGUF",
            license = "Apache-2.0",
            minimumDeviceRamGb = 8,
            codingLevel = 3,
        ),
        AltivOfflineModel(
            id = "qwen25coder-7b-q4km",
            name = "Qwen2.5-Coder 7B Instruct",
            subtitle = "Programação mais exigente; apenas em celulares com RAM elevada",
            category = AltivOfflineCategory.TEXT,
            quantization = "GGUF Q4_K_M",
            sizeBytes = 4680L * AltivOfflineModel.ONE_MB,
            ramGuidance = "RAM total recomendada: 10 GB ou mais",
            fileName = "Qwen2.5-Coder-7B-Instruct-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/bartowski/Qwen2.5-Coder-7B-Instruct-GGUF/resolve/main/Qwen2.5-Coder-7B-Instruct-Q4_K_M.gguf",
            sourceUrl = "https://huggingface.co/bartowski/Qwen2.5-Coder-7B-Instruct-GGUF",
            license = "Apache-2.0",
            minimumDeviceRamGb = 10,
            codingLevel = 4,
        )
    )

    fun inCategory(category: AltivOfflineCategory): List<AltivOfflineModel> =
        models.filter { it.category == category }

    fun byId(id: String): AltivOfflineModel? = models.firstOrNull { it.id == id }
}
