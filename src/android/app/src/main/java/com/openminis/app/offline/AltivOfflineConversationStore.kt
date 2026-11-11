package com.openminis.app.offline

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Per-model chat log, private to the app. No provider API requests and no
 * cloud sync. Clearing app data or uninstalling also clears this history.
 */
internal class AltivOfflineConversationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "altiv_offline_chats", Context.MODE_PRIVATE
    )

    companion object {
        /**
         * Keep standalone conversations under their original key. Inside the
         * main chat, isolate local turns by both session and model so two
         * different projects cannot accidentally mix offline histories.
         */
        internal fun keyFor(modelId: String, sessionId: String? = null): String {
            require(AltivOfflineModelCatalog.byId(modelId) != null) {
                "Modelo fora do catálogo offline."
            }
            if (sessionId == null) return "messages_" + modelId
            require(sessionId.isNotBlank() && sessionId.length <= 160) {
                "Identificador da conversa inválido."
            }
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(sessionId.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            return "messages_inchat_" + digest + "_" + modelId
        }
    }

    fun read(modelId: String, sessionId: String? = null): List<AltivOfflineConversationPrompt.Turn> = runCatching {
        val raw = prefs.getString(keyFor(modelId, sessionId), null) ?: return@runCatching emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val role = runCatching {
                AltivOfflineConversationPrompt.Role.valueOf(obj.optString("role"))
            }.getOrNull() ?: return@mapNotNull null
            val value = obj.optString("text").take(
                AltivOfflineConversationPrompt.MAX_TEXT_LENGTH
            )
            if (value.isBlank()) null else AltivOfflineConversationPrompt.Turn(role, value)
        }.takeLast(AltivOfflineConversationPrompt.MAX_TURNS)
    }.getOrDefault(emptyList())

    fun save(modelId: String, turns: List<AltivOfflineConversationPrompt.Turn>, sessionId: String? = null) {
        val payload = JSONArray()
        turns.takeLast(AltivOfflineConversationPrompt.MAX_TURNS).forEach { turn ->
            payload.put(JSONObject().put("role", turn.role.name).put(
                "text", turn.text.take(AltivOfflineConversationPrompt.MAX_TEXT_LENGTH)
            ))
        }
        prefs.edit().putString(keyFor(modelId, sessionId), payload.toString()).apply()
    }

    fun clear(modelId: String, sessionId: String? = null) {
        prefs.edit().remove(keyFor(modelId, sessionId)).apply()
    }
}
