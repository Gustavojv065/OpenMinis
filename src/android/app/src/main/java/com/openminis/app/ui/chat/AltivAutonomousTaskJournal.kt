package com.openminis.app.ui.chat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable, bounded execution breadcrumbs for ONE chat/workspace session.
 *
 * This is deliberately not customer memory: never store prompts, secrets, paths,
 * command arguments or raw tool outputs. A successful tool invocation is an
 * ATTEMPT, never evidence that visual or commercial QA passed.
 */
internal object AltivAutonomousTaskJournal {
    private const val STORE = "altive_autonomous_tasks_v1"
    private const val MAX_EVENTS = 12
    private val lock = Any()

    internal fun phaseForTool(tool: String, previous: String): String = when (tool.lowercase()) {
        "read_image" -> "visual_review"
        "browser_use" -> "browser_review"
        "memory_get", "memory_write" -> if (previous == "brief") "context" else previous
        "shell_execute", "file_write", "file_edit", "file_read" -> "implementation"
        "subagent_task", "delegate_task" -> "delegation"
        else -> previous
    }

    internal fun acceptsResult(success: Boolean, attempted: Boolean): String = when {
        !attempted -> "NOT_STARTED"
        success -> "ATTEMPTED_NOT_VERIFIED"
        else -> "FAILED"
    }

    private fun key(sessionId: String): String = "session:" + sessionId.take(128)

    fun begin(context: Context, sessionId: String) {
        if (sessionId.isBlank()) return
        synchronized(lock) {
            val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            val previous = prefs.getString(key(sessionId), null)
                ?.let { runCatching { JSONObject(it) }.getOrNull() }
            // A follow-up in the SAME session resumes its existing bounded
            // milestones; only a new session creates a new journal. The user
            // can say "continue" without losing the last verified tool attempt.
            val state = previous ?: JSONObject()
                .put("schema", 1)
                .put("phase", "brief")
                .put("role", "supervisor")
                .put("started_at_ms", System.currentTimeMillis())
                .put("last_tool", JSONObject.NULL)
                .put("last_tool_status", "NOT_STARTED")
                .put("events", JSONArray())
                .put("vision_evidence", "NOT_VERIFIED")
                .put("build_evidence", "NOT_VERIFIED")
                .put("browser_evidence", "NOT_VERIFIED")
            state.put("status", "ACTIVE")
            prefs.edit().putString(key(sessionId), state.toString()).apply()
        }
    }

    fun noteTool(context: Context, sessionId: String, tool: String, success: Boolean) {
        if (sessionId.isBlank() || tool.isBlank()) return
        synchronized(lock) {
            val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            val raw = prefs.getString(key(sessionId), null) ?: return
            val state = runCatching { JSONObject(raw) }.getOrNull() ?: return
            if (state.optString("status") == "COMPLETE") return
            val events = state.optJSONArray("events") ?: JSONArray()
            val compact = JSONArray()
            val from = (events.length() - MAX_EVENTS + 1).coerceAtLeast(0)
            for (i in from until events.length()) compact.put(events.opt(i))
            compact.put(JSONObject()
                .put("tool", tool.take(48))
                .put("result", acceptsResult(success, attempted = true))
                .put("at_ms", System.currentTimeMillis()))
            state.put("events", compact)
                .put("phase", phaseForTool(tool, state.optString("phase", "brief")))
                .put("last_tool", tool.take(48))
                .put("last_tool_status", acceptsResult(success, attempted = true))
                .put("status", if (success) "ACTIVE" else "NEEDS_REPAIR")
            // Evidence stays NOT_VERIFIED until the model inspects real results.
            // NEVER equate shell exit 0 with a verified build or browser success.
            prefs.edit().putString(key(sessionId), state.toString()).apply()
        }
    }

    fun markInterrupted(context: Context, sessionId: String) {
        if (sessionId.isBlank()) return
        synchronized(lock) {
            val prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            val state = runCatching { JSONObject(prefs.getString(key(sessionId), null) ?: return) }.getOrNull()
                ?: return
            state.put("status", "INTERRUPTED")
            prefs.edit().putString(key(sessionId), state.toString()).apply()
        }
    }

    fun contextForResume(context: Context, sessionId: String): String {
        if (sessionId.isBlank()) return ""
        return synchronized(lock) {
            val raw = context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
                .getString(key(sessionId), null) ?: return@synchronized ""
            val state = runCatching { JSONObject(raw) }.getOrNull() ?: return@synchronized ""
            val tool = state.optString("last_tool").takeUnless { it.isBlank() || it == "null" } ?: "none"
            """[ALTIVE SESSION WORK JOURNAL — descriptive, NOT QA evidence]
Scope: current chat/workspace session only; never another project's files.
State: ${state.optString("status", "UNKNOWN")}
Last stage: ${state.optString("phase", "brief")}
Last actual tool: $tool (${state.optString("last_tool_status", "NOT_STARTED")})
Build, vision and browser sign-off: NOT_VERIFIED until checked against real outputs.
If resuming, inspect current workspace and evidence before continuing. Do not
repeat destructive actions or claim that earlier attempts passed.
[END ALTIVE SESSION WORK JOURNAL]"""
        }
    }
}
