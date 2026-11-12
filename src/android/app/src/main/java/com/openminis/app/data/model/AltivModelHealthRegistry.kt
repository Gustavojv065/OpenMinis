package com.openminis.app.data.model

/**
 * Optional, process-local routing hint from the explicit group health check.
 *
 * No credentials, prompts, responses or spend are recorded. An expired sample
 * is equivalent to "unknown", so a temporary incident never blocks a model.
 * This only reorders candidates that the user has already put in a group:
 * it never adds new providers or starts network tests automatically.
 */
internal object AltivModelHealthRegistry {
    const val MAX_AGE_MS: Long = 10 * 60 * 1000L
    private const val MAX_SAMPLES = 256

    private data class Sample(
        val working: Boolean,
        val checkedAtMillis: Long,
        val latencyMs: Long,
    )
    private val samples = LinkedHashMap<String, Sample>()

    @Synchronized
    fun record(
        entryId: String,
        working: Boolean,
        checkedAtMillis: Long = System.currentTimeMillis(),
        latencyMs: Long = 0L,
    ) {
        if (entryId.isBlank()) return
        samples.remove(entryId)
        samples[entryId] = Sample(working, checkedAtMillis, latencyMs.coerceAtLeast(0))
        while (samples.size > MAX_SAMPLES) samples.remove(samples.keys.first())
    }

    @Synchronized
    fun clear() = samples.clear()

    @Synchronized
    fun priority(entryId: String, nowMillis: Long = System.currentTimeMillis()): Int {
        val sample = samples[entryId] ?: return 1
        val age = nowMillis - sample.checkedAtMillis
        if (age < 0 || age >= MAX_AGE_MS) return 1
        return if (sample.working) 0 else 2
    }

    /** Lower positive sample latency comes first within the healthy tier.
     * Unknown latency is neutral and never suggests an untested model is fast.
     * Buckets avoid reshuffling for tiny, noisy differences.
     */
    @Synchronized
    fun latencyBucket(entryId: String, nowMillis: Long = System.currentTimeMillis()): Long {
        val sample = samples[entryId] ?: return Long.MAX_VALUE
        val age = nowMillis - sample.checkedAtMillis
        if (!sample.working || age < 0 || age >= MAX_AGE_MS || sample.latencyMs <= 0)
            return Long.MAX_VALUE
        return sample.latencyMs / 250L
    }

    /**
     * Healthy first, untested second, recently failed last.
     * Among healthy entries with measured latency, prefer quicker responses.
     * Untested results and samples with unknown latency keep prior order.
     */
    fun <T> prioritize(
        candidates: List<T>,
        entryId: (T) -> String,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<T> = candidates.withIndex().sortedWith(
        compareBy<IndexedValue<T>>(
            { priority(entryId(it.value), nowMillis) },
            { latencyBucket(entryId(it.value), nowMillis) },
            { it.index },
        ),
    ).map { it.value }
}
