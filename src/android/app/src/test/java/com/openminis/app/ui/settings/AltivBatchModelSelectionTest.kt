package com.openminis.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivBatchModelSelectionTest {
    @Test fun testsAtMostTwelveUniqueModelsPerExplicitBatch() {
        val candidates = (1..28).map { "model-$it" } + listOf("model-1", "model-2")
        val first = AltivBatchModelSelection.nextBatch(candidates, emptySet())
        assertEquals(12, first.size)
        assertEquals(12, first.toSet().size)
        val second = AltivBatchModelSelection.nextBatch(candidates, first.toSet())
        assertEquals(12, second.size)
        assertFalse(first.any { it in second })
        val third = AltivBatchModelSelection.nextBatch(candidates, (first + second).toSet())
        assertEquals(4, third.size)
    }

    @Test fun noBatchWhenAllModelsWereChecked() {
        assertTrue(AltivBatchModelSelection.nextBatch(listOf("a"), setOf("a")).isEmpty())
    }

    @Test fun onlyVerifiedWorkingAndEligibleModelsAreSelected() {
        val selection = AltivBatchModelSelection.addWorking(
            currentSelection = setOf("manual"),
            workingIds = setOf("good", "not-in-group"),
            eligibleIds = setOf("good", "failed", "manual"),
        )
        assertEquals(setOf("manual", "good"), selection)
    }
}
