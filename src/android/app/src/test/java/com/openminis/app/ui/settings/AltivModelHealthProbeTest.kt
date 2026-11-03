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
        assertEquals(AltivModelHealthProbe.Status.AUTH_FAILED, probe.classifyError("HTTP 401 unauthorized"))
        assertEquals(AltivModelHealthProbe.Status.AUTH_FAILED, probe.classifyError("HTTP 403 forbidden"))
        assertEquals(AltivModelHealthProbe.Status.RATE_LIMITED, probe.classifyError("HTTP 429 rate limit"))
        assertEquals(AltivModelHealthProbe.Status.RATE_LIMITED, probe.classifyError("insufficient_quota"))
        assertEquals(AltivModelHealthProbe.Status.TIMEOUT, probe.classifyError("request timed out"))
        assertEquals(AltivModelHealthProbe.Status.FAILED, probe.classifyError("HTTP 500 internal error"))
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

    @Test
    fun `working models precede unknown and recently failing models, stably`() {
        val registry = com.openminis.app.data.model.AltivModelHealthRegistry
        registry.clear()
        val now = 100_000L
        registry.record("fail", working = false, checkedAtMillis = now)
        registry.record("ok", working = true, checkedAtMillis = now)
        val ranked = registry.prioritize(
            listOf("fail", "unknown1", "ok", "unknown2"),
            entryId = { it }, nowMillis = now + 1_000,
        )
        assertEquals(listOf("ok", "unknown1", "unknown2", "fail"), ranked)
        registry.clear()
    }

    @Test
    fun `health hints expire and restore the configured order`() {
        val registry = com.openminis.app.data.model.AltivModelHealthRegistry
        registry.clear()
        val now = 200_000L
        registry.record("failed", working = false, checkedAtMillis = now)
        registry.record("ok", working = true, checkedAtMillis = now)
        assertEquals(
            listOf("failed", "untested", "ok"),
            registry.prioritize(
                listOf("failed", "untested", "ok"),
                entryId = { it },
                nowMillis = now + registry.MAX_AGE_MS,
            ),
        )
        registry.clear()
    }

    @Test
    fun `fresh test updates previous failure and does not remove candidates`() {
        val registry = com.openminis.app.data.model.AltivModelHealthRegistry
        registry.clear()
        registry.record("one", working = false, checkedAtMillis = 10_000L)
        registry.record("one", working = true, checkedAtMillis = 20_000L)
        val candidates = listOf("two", "one", "three")
        val ranked = registry.prioritize(candidates, entryId = { it }, nowMillis = 20_100L)
        assertEquals(listOf("one", "two", "three"), ranked)
        assertEquals(candidates.size, ranked.size)
        registry.clear()
    }

    @Test
    fun `router only uses already selected group candidates and never probes automatically`() {
        val healthSheet = File("src/main/java/com/openminis/app/ui/settings/AltivGroupHealthSheet.kt").readText()
        val viewModel = File("src/main/java/com/openminis/app/ui/chat/ChatViewModel.kt").readText()
        assertTrue(healthSheet.contains("AltivModelHealthRegistry.record("))
        assertFalse(healthSheet.contains("LaunchedEffect"))
        assertTrue(viewModel.contains("AltivModelHealthRegistry.prioritize(result)"))
        assertTrue(viewModel.contains("AltivModelHealthRegistry.prioritize(out)"))
    }
}
