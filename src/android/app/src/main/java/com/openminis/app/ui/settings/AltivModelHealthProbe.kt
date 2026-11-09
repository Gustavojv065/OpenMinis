package com.openminis.app.ui.settings

import android.content.Context
import android.os.SystemClock
import com.openminis.app.data.model.AgentToolDefinition
import com.openminis.app.data.model.AgentToolParam
import com.openminis.app.data.model.LLMMessage
import com.openminis.app.data.model.LLMStreamChunk
import com.openminis.app.data.model.ModelEntry
import com.openminis.app.data.repository.ProviderRepository
import com.openminis.app.provider.LLMProvider
import com.openminis.app.provider.ProviderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * On-demand probes. CONNECTION checks text responses. CODING_TOOLS checks
 * actual structured function calls with TypeScript, without executing tools.
 * No project file, shell command or account data is accessed by the probe.
 */
internal object AltivModelHealthProbe {
    const val MAX_MODELS_PER_RUN = 12
    const val TIMEOUT_MS = 45_000L
    const val EXTENDED_TIMEOUT_MS = 75_000L
    private const val MAX_ATTEMPTS = 2
    private const val TOOL_NAME = "altiv_preview_code"

    enum class Mode { CONNECTION, CODING_TOOLS }
    enum class Status {
        WAITING, RUNNING, WORKING, NO_CREDENTIAL, DISABLED,
        INCOMPATIBLE, AUTH_FAILED, RATE_LIMITED, TIMEOUT, FAILED,
        BAD_REQUEST, MODEL_MISSING, SERVICE_UNAVAILABLE,
        NO_CREDIT, TOOL_UNSUPPORTED, INVALID_CODE,
    }

    data class Result(
        val status: Status,
        val latencyMs: Long = 0,
        val attempts: Int = 1,
    )

    fun supportsText(outputs: List<String>?): Boolean {
        val normalized = outputs.orEmpty().map { it.trim().lowercase() }
        return normalized.isEmpty() || "text" in normalized
    }

    /** Classify without showing response bodies, credentials or request data. */
    fun classifyError(message: String?): Status {
        val text = message.orEmpty().lowercase()
        fun code(n: Int) = Regex("""(^|\D)""" + n + """(\D|$)""").containsMatchIn(text)
        return when {
            code(401) || code(403) || "unauthorized" in text ||
                "invalid api key" in text || "authentication" in text -> Status.AUTH_FAILED
            code(402) || "insufficient balance" in text -> Status.NO_CREDIT
            code(429) || "rate limit" in text || "quota" in text -> Status.RATE_LIMITED
            code(404) || "model not found" in text || "model does not exist" in text ->
                Status.MODEL_MISSING
            code(400) || code(422) || "unsupported parameter" in text ||
                "invalid request" in text -> Status.BAD_REQUEST
            code(500) || code(502) || code(503) || code(504) ||
                "service unavailable" in text || "upstream error" in text ->
                Status.SERVICE_UNAVAILABLE
            "timeout" in text || "timed out" in text -> Status.TIMEOUT
            else -> Status.FAILED
        }
    }

    fun isRetryable(status: Status): Boolean =
        status == Status.SERVICE_UNAVAILABLE || status == Status.TIMEOUT

    /** Minimal syntax sanity check, not a full compiler or benchmark. */
    fun looksLikeSumFunction(source: String): Boolean =
        Regex("""\bfunction\s+sum\b|\b(?:const|let)\s+sum\s*=""").containsMatchIn(source) &&
            "+" in source && ("return" in source || "=>" in source)

    suspend fun check(
        entry: ModelEntry,
        providers: ProviderRepository,
        context: Context,
        mode: Mode = Mode.CONNECTION,
        timeoutMs: Long = TIMEOUT_MS,
    ): Result = withContext(Dispatchers.IO) {
        val instance = providers.instance(entry.providerInstanceId)
            ?: return@withContext Result(Status.DISABLED)
        if (!instance.isEnabled || entry.isHidden || entry.isUnavailableFromProvider) {
            return@withContext Result(Status.DISABLED)
        }
        if (!supportsText(entry.model.outputModalities)) {
            return@withContext Result(Status.INCOMPATIBLE)
        }
        val key = providers.requestCredential(instance)
            ?: return@withContext Result(Status.NO_CREDENTIAL)
        val started = SystemClock.elapsedRealtime()
        val boundedTimeout = timeoutMs.coerceIn(10_000L, EXTENDED_TIMEOUT_MS)
        var status = Status.FAILED
        var attempts = 0
        while (attempts < MAX_ATTEMPTS) {
            attempts++
            try {
                val provider = ProviderFactory.create(
                    instance, key, entry.model, context,
                    sessionId = ProviderFactory.oneOffSessionId(),
                    overrides = entry.overrides,
                )
                status = when (mode) {
                    Mode.CONNECTION -> checkText(provider, boundedTimeout)
                    Mode.CODING_TOOLS -> checkCodingTool(provider, boundedTimeout)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Exception) {
                status = classifyError(error.message)
            }
            if (attempts >= MAX_ATTEMPTS || !isRetryable(status)) break
            delay(700L)
        }
        Result(status, SystemClock.elapsedRealtime() - started, attempts)
    }

    private suspend fun checkText(provider: LLMProvider, timeoutMs: Long): Status {
        val reply = withTimeoutOrNull(timeoutMs) {
            provider.sendMessage(
                messages = listOf(LLMMessage(LLMMessage.Role.USER, "Reply with OK only.")),
                systemPrompt = null,
                maxTokens = 128,
                temperature = null,
            )
        } ?: return Status.TIMEOUT
        return if (reply.text.isNotBlank()) Status.WORKING else Status.FAILED
    }

    private val toolDefinition = AgentToolDefinition(
        name = TOOL_NAME,
        description = "Preview TypeScript without writing files.",
        parameters = mapOf(
            "file_path" to AgentToolParam("string", "Virtual path: altiv-probe.ts"),
            "source" to AgentToolParam("string", "TypeScript implementation of sum"),
        ),
        required = listOf("file_path", "source"),
    )

    /** The model may request a tool. The app NEVER executes this dummy tool. */
    private suspend fun checkCodingTool(provider: LLMProvider, timeoutMs: Long): Status {
        var sawTool = false
        var valid = false
        val completed = withTimeoutOrNull(timeoutMs) {
            provider.streamMessage(
                messages = listOf(LLMMessage(
                    LLMMessage.Role.USER,
                    "Call altiv_preview_code exactly once. Set file_path to altiv-probe.ts " +
                        "and source to TypeScript function sum(a: number, b: number): number " +
                        "returning a + b. Do not respond in prose.",
                )),
                systemPrompt = "Diagnostic only. Call the provided function, not the shell.",
                maxTokens = 320,
                temperature = null,
                tools = listOf(toolDefinition),
            ).collect { chunk ->
                if (chunk is LLMStreamChunk.ToolCallComplete && chunk.name == TOOL_NAME) {
                    sawTool = true
                    val path = chunk.args.optString("file_path", "")
                    val source = chunk.args.optString("source", "")
                    if (path == "altiv-probe.ts" && looksLikeSumFunction(source)) valid = true
                }
            }
            true
        }
        return when {
            completed == null -> Status.TIMEOUT
            valid -> Status.WORKING
            sawTool -> Status.INVALID_CODE
            else -> Status.TOOL_UNSUPPORTED
        }
    }
}
