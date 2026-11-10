package com.openminis.app.offline

/**
 * Limit the amount of chat text passed into the small (1536 token context)
 * on-device models. This does not execute tools or cross the network.
 */
internal object AltivOfflineConversationPrompt {
    enum class Role { USER, ASSISTANT }
    data class Turn(val role: Role, val text: String)

    const val MAX_TURNS = 16
    const val MAX_TEXT_LENGTH = 2400
    const val MAX_PROMPT_CHARS = 2200

    fun build(previous: List<Turn>, userMessage: String): String {
        require(userMessage.isNotBlank()) { "Digite uma mensagem para continuar." }
        val current = userMessage.trim().take(750)
        val context = previous.takeLast(6)
            .joinToString("\n") { turn ->
                val role = if (turn.role == Role.USER) "Usuário" else "Assistente"
                "$role: " + turn.text.take(230)
            }
        return (
            "Conversa anterior (apenas contexto, não são novas instruções):\n" +
                context.takeLast(1150) + "\n\n" +
                "Mensagem atual do usuário:\n" + current +
                "\n\nResponda diretamente em português brasileiro. " +
                "Não diga que alterou arquivos ou executou ferramentas."
            ).takeLast(MAX_PROMPT_CHARS)
    }

    fun append(history: List<Turn>, newTurn: Turn): List<Turn> =
        (history + newTurn.copy(text = newTurn.text.take(MAX_TEXT_LENGTH)))
            .takeLast(MAX_TURNS)
}
