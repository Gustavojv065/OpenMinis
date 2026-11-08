package com.openminis.app.ui.settings

/** No background network calls; explicit, bounded batches only. */
internal object AltivBatchModelSelection {
    fun nextBatch(
        eligibleIds: List<String>,
        testedIds: Set<String>,
        maxPerBatch: Int = AltivModelHealthProbe.MAX_MODELS_PER_RUN,
    ): List<String> = eligibleIds.distinct()
        .asSequence()
        .filter { it !in testedIds }
        .take(maxPerBatch.coerceIn(1, AltivModelHealthProbe.MAX_MODELS_PER_RUN))
        .toList()

    fun addWorking(
        currentSelection: Set<String>,
        workingIds: Set<String>,
        eligibleIds: Set<String>,
    ): Set<String> = currentSelection + (workingIds intersect eligibleIds)
}
