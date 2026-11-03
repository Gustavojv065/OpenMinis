package com.openminis.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AltivModelHealthProbeTest {
    @Test
    fun `detects auth errors, quota errors and timeouts without leaking details`() {
        val probe = AltivModelHealthProbe
        assertEquals(probe.Status.AUTH_FAILED, probe.classifyError("HTTP 401 unauthorized"))
        assertEquals(probe.Status.AUTH_FAILED, probe.classifyError("HTTP 403 forbidden"))
        assertEquals(probe.Status.RATE_LIMITED, probe.classifyError("HTTP 429 rate limit"))
        assertEquals(probe.Status.RATE_LIMITED, probe.classifyError("insufficient_quota"))
        assertEquals(probe.Status.TIMEOUT, probe.classifyError("request timed out"))
        assertEquals(probe.Status.FAILED, probe.classifyError("HTTP 500 internal error"))
    }

    @Test
    fun `skips non text output models`() {
        val probe = AltivModelHealthProbe
        assertTrue(probe.supportsText(null))
        assertTrue(probe.supportsText(emptyList()))
        assertTrue(probe.supportsText(listOf("text", "image")))
        assertFalse(probe.supportsText(listOf("audio")))
        assertFalse(probe.supportsText(listOf("image")))
    }

    @Test
    fun `user must explicitly start tests and the batch is bounded`() {
        val sheet = File("src/main/java/com/openminis/app/ui/settings/AltivGroupHealthSheet.kt").readText()
        assertTrue(sheet.contains("onClick = {"))
        assertTrue(sheet.contains("AltivModelHealthProbe.check("))
        assertFalse(sheet.contains("LaunchedEffect"))
        assertEquals(12, AltivModelHealthProbe.MAX_MODELS_PER_RUN)
        assertTrue(sheet.contains("MAX_MODELS_PER_RUN"))
    }

    @Test
    fun `group fallback UI and on demand health panel coexist`() {
        val group = File("src/main/java/com/openminis/app/ui/settings/ModelGroupDetailScreen.kt").readText()
        assertTrue(group.contains("RoutingStrategy.fallback"))
        assertTrue(group.contains("FallbackStrategy.always"))
        assertTrue(group.contains("AltivGroupHealthSheet("))
    }
}
