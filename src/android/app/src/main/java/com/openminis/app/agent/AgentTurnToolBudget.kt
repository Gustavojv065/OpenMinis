package com.openminis.app.agent

/**
 * ALTIVE DEV AI: a hard, per-user-request tool execution budget.
 *
 * ToolLoopDetector looks for identical arguments/results in a short sliding
 * window. A model can evade that detector accidentally by changing its
 * JavaScript expression on every call. This gate bounds the number of REAL
 * executions even when every browser_use / shell_execute call is unique.
 *
 * One instance belongs to one runAgentLoopBody; a fresh user request gets a
 * fresh budget. Individual tools are reserved before execution so concurrent
 * tool batches cannot exceed the ceiling.
 */
internal class AgentTurnToolBudget(
    private val maxTotal: Int = 72,
    private val maxBrowser: Int = 24,
    private val maxShell: Int = 32,
) {
    init {
        require(maxTotal > 0 && maxBrowser > 0 && maxShell > 0)
    }

    private var total = 0
    private var browsers = 0
    private var shells = 0

    /** Return a model-facing failure reason instead of executing when over budget. */
    @Synchronized
    fun reserve(toolName: String): String? {
        val blocked = exhaustedUnsafe() ||
            (toolName == "browser_use" && browsers >= maxBrowser) ||
            (toolName == "shell_execute" && shells >= maxShell)
        if (blocked) {
            return "[TOOL BUDGET EXHAUSTED] ALTIVE DEV AI paused this request to prevent " +
                "a repetitive tool loop. No further tools should be called. " +
                "Summarize what was actually completed, state untested items, " +
                "and wait for a new user instruction."
        }
        total++
        if (toolName == "browser_use") browsers++
        if (toolName == "shell_execute") shells++
        return null
    }

    /** Main agent loop checks this at the next safe tool-result boundary. */
    @Synchronized
    fun exhausted(): Boolean = exhaustedUnsafe()

    private fun exhaustedUnsafe(): Boolean =
        total >= maxTotal || browsers >= maxBrowser || shells >= maxShell

    @Synchronized
    fun snapshot(): Triple<Int, Int, Int> = Triple(total, browsers, shells)
}
