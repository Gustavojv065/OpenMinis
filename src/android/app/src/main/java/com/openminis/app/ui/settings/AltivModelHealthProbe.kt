package com.openminis.app.ui.settings

import android.content.Context
import android.os.SystemClock
import com.openminis.app.data.model.LLMMessage
import com.openminis.app.data.model.ModelEntry
import com.openminis.app.data.repository.ProviderRepository
import com.openminis.app.provider.ProviderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Opt-in group smoke tests: ONE tiny text request at a time, never a background
 * service. Does not expose or persist any credential or real conversation.
 * A working /models response is insufficient: this verifies generation.
 *
 * A successful probe reflects only this instant; it does NOT guarantee that
 * quota, latency or availability will remain the same later.
 */
internal object AltivModelHealthProbe {
    const val MAX_MODELS_PER_RUN = 12
    const val TIMEOUT_MS = 20_000L

    enum class Status {
        WAITING, RUNNING, WORKING, NO_CREDENTIAL, DISABLED,
        INCOMPATIBLE, AUTH_FAILED, RATE_LIMITED, TIMEOUT, FAILED,
    }

    data class Result(
        val status: Status,
        val latencyMs: Long = 0,
    )

    fun supportsText(outputs: List<String>?): Boolean {
        val normalized = outputs.orEmpty().map { it.trim().lowercase() }
        return normalized.isEmpty() || "text" in normalized
    }

    fun classifyError(message: String?): Status {
        val text = message.orEmpty().lowercase()
        return when {
            Regex("""(^|\\D)(401|403)(\\D|$)""").containsMatchIn(text) ||
                "unauthorized" in text || "invalid api key" in text ||
                "authentication" in text -> Status.AUTH_FAILED
            Regex("""(^|\\D)429(\\D|$)""").containsMatchIn(text) ||
                "rate limit" in text || "quota" in text -> Status.RATE_LIMITED
            "timeout" in text || "timed out" in text -> Status.TIMEOUT
            else -> Status.FAILED
        }
    }

    suspend fun check(
        entry: ModelEntry,
        providers: ProviderRepository,
        context: Context,
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
        try {
            val provider = ProviderFactory.create(
                instance, key, entry.model, context,
                sessionId = ProviderFactory.oneOffSessionId(),
                overrides = entry.overrides,
            )
            val reply = withTimeoutOrNull(TIMEOUT_MS) {
                provider.sendMessage(
                    messages = listOf(
                        LLMMessage(
                            role = LLMMessage.Role.USER,
                            content = "Connectivity test. Reply with OK only.",
                        ),
                    ),
                    systemPrompt = null,
                    maxTokens = 32,
                    temperature = null,
                )
            } ?: return@withContext Result(Status.TIMEOUT, SystemClock.elapsedRealtime() - started)
            val status = if (reply.text.isNotBlank()) Status.WORKING else Status.FAILED
            Result(status, SystemClock.elapsedRealtime() - started)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            Result(classifyError(error.message), SystemClock.elapsedRealtime() - started)
        }
    }
}
