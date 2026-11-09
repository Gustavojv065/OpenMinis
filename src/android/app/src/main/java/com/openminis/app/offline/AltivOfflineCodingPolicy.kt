package com.openminis.app.offline

/**
 * Conservative mobile selection. These are recommendations, NOT proof that
 * a large model will run or that it can safely change a production repository.
 * Cloud fallback is never enabled by this policy; it needs explicit consent.
 */
internal object AltivOfflineCodingPolicy {
    enum class Task { SIMPLE_TEXT, CODING, PLANNING, LARGE_REPOSITORY, VISUAL_3D }
    data class Advice(val modelId: String?, val needsOnlineTools: Boolean, val rationale: String)

    fun deviceSuitable(model: AltivOfflineModel, totalRamGb: Int): Boolean =
        totalRamGb >= model.minimumDeviceRamGb

    fun recommend(
        task: Task,
        totalRamGb: Int,
        installedIds: Set<String>,
    ): Advice {
        val choices = AltivOfflineModelCatalog.models.filter {
            it.id in installedIds && deviceSuitable(it, totalRamGb)
        }
        val needsOnline = task == Task.LARGE_REPOSITORY || task == Task.VISUAL_3D
        val ordered = when (task) {
            Task.SIMPLE_TEXT, Task.PLANNING -> choices.sortedWith(
                compareByDescending<AltivOfflineModel> { it.id.startsWith("qwen3-4b") }
                    .thenByDescending { it.codingLevel }
            )
            else -> choices.sortedWith(
                compareByDescending<AltivOfflineModel> { it.id.contains("coder") }
                    .thenByDescending { it.codingLevel }
            )
        }
        val selected = ordered.firstOrNull()
        return Advice(
            modelId = selected?.id,
            needsOnlineTools = needsOnline,
            rationale = when {
                selected == null ->
                    "Nenhum modelo local adequado está instalado. Escolha um pequeno para teste."
                needsOnline ->
                    "O modelo local pode ajudar a planejar, mas edições grandes/3D precisam " +
                        "de um agente com ferramentas, testes e autorização de rede."
                else -> "Modelo local sugerido para esta tarefa. Valide o resultado antes de aplicar."
            },
        )
    }
}
