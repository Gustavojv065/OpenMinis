package com.openminis.app.ui.chat

/**
 * ALTIVE — don't mistake an announcement of the NEXT tool action for a
 * completed task. A model can finish normally with "Agora vou executar..."
 * but no tool call, which used to silently finish runAgentLoop.
 *
 * Narrow scope: only a final Portuguese action sentence, clean stop, real
 * available tools, main agent, existing authorized work. At most two retries
 * per run. This heuristic is NOT a promise that the task was completed.
 */
internal object AltivPrematureWorkContinuation {
    const val MAX_RETRIES = 2

    private val impendingAction = Regex(
        """(?iu)^(?:agora\s+vou|em\s+seguida\s+vou|na\s+sequ[êe]ncia\s+vou|a\s+seguir\s+vou|vou\s+(?:executar|iniciar|rodar|abrir|testar|verificar|capturar|corrigir|ajustar|inspecionar|validar|analisar|implementar)|come[çc]ando\s+(?:pela?|com)|iniciando\s+(?:a|o)|executando\s+(?:a|o))\b"""
    )

    const val REMINDER: String =
        "<system-reminder>Você anunciou uma próxima ação, mas terminou sem chamar " +
        "nenhuma ferramenta. Continue a tarefa já autorizada agora: execute a " +
        "próxima ferramenta disponível, se necessária, e verifique seu resultado. " +
        "Não responda novamente apenas 'vou executar'. Se a ferramenta estiver " +
        "indisponível ou sem permissão, informe a limitação e as pendências " +
        "concretas. Não invente resultados, não repita operações que falharam " +
        "e não instale pacotes, faça merge/deploy, publique ou altere dados " +
        "destrutivamente sem autorização explícita.</system-reminder>"

    fun shouldRetry(
        text: String,
        finishReason: String?,
        hasToolsAvailable: Boolean,
        isHelper: Boolean,
        userPromptWaiting: Boolean,
        retryCount: Int,
    ): Boolean {
        if (!hasToolsAvailable || isHelper || userPromptWaiting || retryCount >= MAX_RETRIES) return false
        if (finishReason != "stop" && finishReason != "end_turn") return false
        // Nemotron frequently combines an observation and the promised next
        // action on one line:
        // "Servidor ativo (HTTP 200). Agora vou fazer a inspeção visual."
        // Only the FINAL sentence may trigger a continuation. Earlier
        // announcements followed by an actual conclusion must not retry.
        val lastLine = text.lineSequence().map(String::trim).lastOrNull { it.isNotEmpty() } ?: return false
        val lastSentence = lastLine.split(Regex("""(?<=[.!?])\\s+""")).last().trim()
        if (lastSentence.isEmpty() || lastSentence.length > 300) return false
        return impendingAction.containsMatchIn(lastSentence)
    }

    fun needsUserVisibleInterruption(
        text: String, finishReason: String?, hasToolsAvailable: Boolean,
        isHelper: Boolean, userPromptWaiting: Boolean, retryCount: Int,
    ): Boolean = retryCount >= MAX_RETRIES && shouldRetry(
        text, finishReason, hasToolsAvailable, isHelper, userPromptWaiting, 0
    )
}
