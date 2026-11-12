package com.openminis.app.data.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AltivOnlineCostPolicyTest {
    private val openRouter = ProviderInstance(
        id = "openrouter-1", label = "OpenRouter",
        providerType = ProviderType.openRouter,
        credentialType = ProviderCredential.apiKey,
    )
    private val openAI = ProviderInstance(
        id = "openai-1", label = "OpenAI",
        providerType = ProviderType.openAI,
        credentialType = ProviderCredential.apiKey,
    )

    private fun entry(id: String, providerId: String = openRouter.id): ModelEntry =
        ModelEntry(
            providerInstanceId = providerId,
            baseModel = LLMModel(id, id, "OpenRouter"),
        )

    @After fun resetSamples() = AltivModelHealthRegistry.clear()

    @Test fun onlyExplicitlyMarkedOpenRouterModelsAreConsideredFree() {
        val free = entry("vendor/model:free")
        val paid = entry("vendor/model")
        val deceptive = entry("vendor/model:free", providerId = openAI.id)
        assertTrue(AltivOnlineCostPolicy.isExplicitlyFree(free, openRouter))
        assertFalse(AltivOnlineCostPolicy.isExplicitlyFree(paid, openRouter))
        assertFalse(AltivOnlineCostPolicy.isExplicitlyFree(deceptive, openAI))
        assertFalse(AltivOnlineCostPolicy.isExplicitlyFree(free, null))
        assertEquals(listOf(free), AltivOnlineCostPolicy.eligibleEntries(
            listOf(free, paid, deceptive), listOf(openRouter, openAI)
        ))
    }

    @Test fun mixedFallbackGroupIsNeverLabeledCostFree() {
        val free = entry("free-model:free")
        val paid = entry("paid-model")
        val safeGroup = ModelGroup(name = "Somente grátis",
            memberEntryIds = mutableListOf(free.id))
        val mixedGroup = ModelGroup(name = "Misto",
            memberEntryIds = mutableListOf(free.id, paid.id))
        assertTrue(AltivOnlineCostPolicy.isGroupFreeOnly(
            safeGroup, listOf(free, paid), listOf(openRouter)
        ))
        assertFalse(AltivOnlineCostPolicy.isGroupFreeOnly(
            mixedGroup, listOf(free, paid), listOf(openRouter)
        ))
    }

    @Test fun recentHealthyModelsUseMeasuredLatencyWithoutReorderingUntested() {
        val now = 100_000L
        AltivModelHealthRegistry.record("slow", true, checkedAtMillis = now,
            latencyMs = 3500)
        AltivModelHealthRegistry.record("fast", true, checkedAtMillis = now,
            latencyMs = 900)
        val arranged = AltivModelHealthRegistry.prioritize(
            listOf("slow", "unknown", "fast"), entryId = { it }, nowMillis = now
        )
        assertEquals(listOf("fast", "slow", "unknown"), arranged)
        assertEquals(listOf("slow", "unknown", "fast"),
            AltivModelHealthRegistry.prioritize(
                listOf("slow", "unknown", "fast"), entryId = { it },
                nowMillis = now + AltivModelHealthRegistry.MAX_AGE_MS
            ))
    }

    @Test fun onlineFirstKeepsOfflineEnginesAndRequiresOptInForBilling() {
        val root = File("src/main/java/com/openminis/app")
        val picker = root.resolve("ui/chat/ChatModelPickerSheet.kt").readText()
        val vm = root.resolve("ui/chat/ChatViewModel.kt").readText()
        val tester = root.resolve("ui/settings/AltivBatchModelTestSheet.kt").readText()
        assertTrue(picker.contains("IA local pausada · mostrar opções"))
        assertTrue(picker.contains("Somente :free evita alternar"))
        assertTrue(vm.contains("getBoolean(\"free_only\", false)"))
        assertTrue(vm.contains("SubmitOutcome.Rejected(\"free_only_no_model\")"))
        assertTrue(vm.contains("fun selectOfflineModel("))
        assertTrue(tester.contains("AltivModelHealthRegistry.record("))
        assertTrue(root.resolve("offline/AltivOfflineInference.kt").exists())
    }
}
