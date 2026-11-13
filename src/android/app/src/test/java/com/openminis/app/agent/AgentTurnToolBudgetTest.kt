package com.openminis.app.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AgentTurnToolBudgetTest {
    @Test fun browserCallsStopAtTheirLimitEvenWithUniqueJavascript() {
        val budget = AgentTurnToolBudget(maxTotal = 20, maxBrowser = 3, maxShell = 10)
        repeat(3) { assertNull(budget.reserve("browser_use")) }
        assertTrue(budget.exhausted())
        assertNotNull(budget.reserve("browser_use"))
        assertEquals(3, budget.snapshot().first)
    }

    @Test fun shellCallsStopAtTheirLimitAndOtherToolsAreCounted() {
        val budget = AgentTurnToolBudget(maxTotal = 12, maxBrowser = 10, maxShell = 2)
        assertNull(budget.reserve("file_read"))
        assertNull(budget.reserve("shell_execute"))
        assertNull(budget.reserve("shell_execute"))
        assertTrue(budget.exhausted())
        assertNotNull(budget.reserve("file_edit"))
        assertEquals(3, budget.snapshot().first)
    }

    @Test fun overallBudgetCatchesVariedNames() {
        val budget = AgentTurnToolBudget(maxTotal = 3, maxBrowser = 5, maxShell = 5)
        for (name in listOf("file_read", "file_edit", "file_write")) {
            assertNull(budget.reserve(name))
        }
        assertTrue(budget.exhausted())
        assertNotNull(budget.reserve("read_image"))
    }

    @Test fun newRequestStartsWithNewBudget() {
        val first = AgentTurnToolBudget(maxTotal = 1)
        assertNull(first.reserve("browser_use"))
        assertTrue(first.exhausted())
        val next = AgentTurnToolBudget(maxTotal = 1)
        assertFalse(next.exhausted())
        assertNull(next.reserve("browser_use"))
    }

    @Test fun concurrentBatchDoesNotExceedBudget() {
        val budget = AgentTurnToolBudget(maxTotal = 50, maxBrowser = 50, maxShell = 50)
        val pool = Executors.newFixedThreadPool(8)
        try {
            val futures = (1..400).map { pool.submit<Boolean> { budget.reserve("file_read") == null } }
            val allowed = futures.count { it.get(10, TimeUnit.SECONDS) }
            assertEquals(50, allowed)
            assertEquals(50, budget.snapshot().first)
        } finally {
            pool.shutdownNow()
        }
    }
}
