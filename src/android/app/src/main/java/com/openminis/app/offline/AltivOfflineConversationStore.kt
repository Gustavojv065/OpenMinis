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

    private fun key(modelId: String): String {
        require(AltivOfflineModelCatalog.byId(modelId) != null) {
            "Modelo fora do catálogo offline."
        }
        return "messages_" + modelId
    }

    fun read(modelId: String): List<AltivOfflineConversationPrompt.Turn> = runCatching {
        val raw = prefs.getString(key(modelId), null) ?: return@runCatching emptyList()
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

    fun save(modelId: String, turns: List<AltivOfflineConversationPrompt.Turn>) {
        val payload = JSONArray()
        turns.takeLast(AltivOfflineConversationPrompt.MAX_TURNS).forEach { turn ->
            payload.put(JSONObject().put("role", turn.role.name).put(
                "text", turn.text.take(AltivOfflineConversationPrompt.MAX_TEXT_LENGTH)
            ))
        }
        prefs.edit().putString(key(modelId), payload.toString()).apply()
    }

    fun clear(modelId: String) {
        prefs.edit().remove(key(modelId)).apply()
    }
}
