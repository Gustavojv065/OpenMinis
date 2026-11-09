package com.openminis.app.data.model

/**
 * ALTIV DEV AI — shortcuts for third-party OpenAI-compatible endpoints.
 * These are NOT new ProviderType values; they reuse the existing encrypted
 * API-key storage, provider configuration, and model selection.
 *
 * URLs contain the complete versioned API base path. The setup flow MUST
 * disable automatic "/v1" appending, otherwise /v1/v1 would break requests.
 * A preset never embeds credentials and never implies that a plan is free.
 */
data class ApiProviderTemplate(
    val id: String,
    val name: String,
    val baseURL: String,
    val appendV1: Boolean = false,
) {
    companion object {
        val all: List<ApiProviderTemplate> = listOf(
            ApiProviderTemplate("nvidia", "NVIDIA NIM", "https://integrate.api.nvidia.com/v1"),
            ApiProviderTemplate("minimax", "MiniMax", "https://api.minimax.io/v1"),
            ApiProviderTemplate("mimo", "Xiaomi MiMo", "https://api.xiaomimimo.com/v1"),
            ApiProviderTemplate("groq", "GroqCloud", "https://api.groq.com/openai/v1"),
            ApiProviderTemplate("cerebras", "Cerebras", "https://api.cerebras.ai/v1"),
            ApiProviderTemplate("mistral", "Mistral AI", "https://api.mistral.ai/v1"),
            ApiProviderTemplate("huggingface", "Hugging Face", "https://router.huggingface.co/v1"),
            ApiProviderTemplate("opencode", "OpenCode Zen", "https://opencode.ai/zen/v1"),
            ApiProviderTemplate("deepseek", "DeepSeek", "https://api.deepseek.com/v1"),
            ApiProviderTemplate("together", "Together AI", "https://api.together.ai/v1"),
            ApiProviderTemplate("fireworks", "Fireworks AI", "https://api.fireworks.ai/inference/v1"),
            ApiProviderTemplate("siliconflow", "SiliconFlow", "https://api.siliconflow.cn/v1"),
            ApiProviderTemplate("deepinfra", "DeepInfra", "https://api.deepinfra.com/v1/openai"),
        )
    }
}
