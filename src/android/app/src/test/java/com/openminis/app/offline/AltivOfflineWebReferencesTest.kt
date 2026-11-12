package com.openminis.app.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AltivOfflineWebReferencesTest {
    @Test fun onlyResearchRequestsTriggerNetworkLookup() {
        assertFalse(AltivOfflineWebReferences.shouldResearch("Olá, tudo bem?"))
        assertFalse(AltivOfflineWebReferences.shouldResearch("Quanto é 2 mais 2?"))
        assertTrue(AltivOfflineWebReferences.shouldResearch(
            "Crie uma landing page de pet shop com fotos reais dos animais."
        ))
        assertTrue(AltivOfflineWebReferences.shouldResearch(
            "Pesquise referências de sites responsivos."
        ))
    }

    @Test fun searchTermsStayShortAndAvoidCommonWords() {
        assertEquals(
            "pet shop gatos",
            AltivOfflineWebReferences.searchTerm(
                "Crie uma landing page pet shop com fotos de gatos"
            )
        )
        val excessive = "foto de cachorro ".repeat(1000)
        assertTrue(AltivOfflineWebReferences.searchTerm(excessive).length <= 85)
    }

    @Test fun sourcesIncludeLinksAndCopyrightInformationWithoutClaimsOfExecution() {
        val result = AltivOfflineWebReferences.Result(
            listOf(AltivOfflineWebReferences.Reference(
                label = "Gatos",
                pageUrl = "https://openverse.org/image/placeholder",
                detail = "Foto; autor: teste; licença: CC BY"
            ))
        )
        assertTrue(result.markdownSources.contains("licença"))
        assertTrue(result.markdownSources.contains("https://openverse.org/image/placeholder"))
        assertTrue(result.promptContext.contains("autor: teste"))
    }

    @Test fun ggufNeverFallsBackToCloudModelsAndWebIsOptIn() {
        val root = File("src/main/java/com/openminis/app")
        val vm = root.resolve("ui/chat/ChatViewModel.kt").readText()
        val picker = root.resolve("ui/chat/ChatModelPickerSheet.kt").readText()
        val service = root.resolve("offline/AltivOfflineWebReferences.kt").readText()
        assertTrue(vm.contains("getBoolean(\"enabled\", false)"))
        assertTrue(vm.contains("_webResearchEnabled.value &&"))
        assertTrue(vm.contains("AltivOfflineWebReferences.search(trimmed)"))
        assertTrue(vm.contains("researched?.markdownSources.orEmpty()"))
        assertTrue(picker.contains("Switch("))
        assertTrue(picker.contains("Pesquisar fotos e referências na internet"))
        assertTrue(service.contains("https://api.openverse.org/v1/images/"))
        assertTrue(service.contains("https://pt.wikipedia.org/w/rest.php/v1/search/page"))
        assertFalse(service.contains("ProviderFactory"))
        assertFalse(service.contains("service_role"))
    }
}
