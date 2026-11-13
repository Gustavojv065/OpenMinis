package com.openminis.app.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AltivOfflineModelCatalogTest {
    @Test fun fiveCuratedTextModelsIncludeCodingTiers() {
        val models = AltivOfflineModelCatalog.models
        assertEquals(5, models.size)
        assertEquals(models.size, models.map { it.id }.toSet().size)
        assertTrue(models.all { it.category == AltivOfflineCategory.TEXT })
        assertTrue(models.all { it.sizeBytes in 400_000_000L..5_000_000_000L })
        assertTrue(models.all { it.fileName.endsWith(".gguf") })
        assertTrue(models.all { it.license.isNotBlank() && it.sourceUrl.isNotBlank() })
        assertTrue(models.all { it.hasTrustedSource() })
        assertTrue(AltivOfflineCategory.entries.containsAll(listOf(
            AltivOfflineCategory.TEXT,
            AltivOfflineCategory.VISION,
            AltivOfflineCategory.TRANSCRIPTION,
            AltivOfflineCategory.TTS,
            AltivOfflineCategory.IMAGES,
            AltivOfflineCategory.UPSCALING,
            AltivOfflineCategory.MUSIC,
            AltivOfflineCategory.VIDEO,
        )))
        assertTrue(AltivMultimodalRoadmap.candidates.isNotEmpty())
        assertEquals(
            AltivMultimodalRoadmap.candidates.size,
            AltivMultimodalRoadmap.candidates.map { it.name }.toSet().size,
        )
        assertEquals(3, models.count { it.id.contains("coder") })
        assertTrue(models.all { it.minimumDeviceRamGb in 4..12 })
        assertTrue(models.all { it.codingLevel in 1..4 })
        assertEquals(1, models.count { it.minimumDeviceRamGb >= 10 })
    }

    @Test fun qwen06bDownloadUsesVerifiedGGUFName() {
        val model = requireNotNull(AltivOfflineModelCatalog.byId("qwen3-06b-q4km"))
        assertEquals("Qwen_Qwen3-0.6B-Q4_K_M.gguf", model.fileName)
        assertTrue(model.downloadUrl.contains("/bartowski/Qwen_Qwen3-0.6B-GGUF/resolve/main/"))
        assertTrue(model.downloadUrl.endsWith("/" + model.fileName))
        assertTrue(model.hasTrustedSource())
    }

    @Test fun allCategoriesAllowExternalDiscoveryWithoutClaimingRuntimeSupport() {
        val screen = File(
            "src/main/java/com/openminis/app/ui/settings/AltivOfflineModelsScreen.kt"
        ).readText()
        val downloads = File(
            "src/main/java/com/openminis/app/offline/AltivOfflineModelDownloads.kt"
        ).readText()
        val inference = File(
            "src/main/java/com/openminis/app/offline/AltivOfflineInference.kt"
        ).readText()
        val chat = File(
            "src/main/java/com/openminis/app/ui/chat/ChatViewModel.kt"
        ).readText()
        assertTrue(screen.contains("Explorar todos os modelos Qwen no navegador"))
        assertTrue(screen.contains("Procurar e baixar pelo navegador"))
        assertTrue(screen.contains("Execução no APK ainda indisponível"))
        assertTrue(screen.contains("Tentar novamente"))
        assertTrue(screen.contains("no 4G/5G os downloads ficarão aguardando Wi-Fi"))
        assertFalse(screen.contains("OutlinedButton(enabled = false, onClick = {})"))
        assertTrue(downloads.contains("fun retryFailed(model: AltivOfflineModel"))
        assertTrue(inference.contains("info.availMem > required"))
        assertFalse(inference.contains("check(totalRamGb(app) >= model.minimumDeviceRamGb)"))
        assertFalse(chat.contains("totalRamGb(context) < model.minimumDeviceRamGb"))
    }

    @Test fun untrustedHostsPathsAndFileExtensionsAreRejected() {
        val model = AltivOfflineModelCatalog.models.first()
        assertFalse(model.copy(downloadUrl =
            "https://example.invalid/resolve/main/" + model.fileName).hasTrustedSource())
        assertFalse(model.copy(downloadUrl =
            "https://huggingface.co.evil.invalid/resolve/main/" + model.fileName).hasTrustedSource())
        assertFalse(model.copy(fileName = "../outside.gguf").hasTrustedSource())
        assertFalse(model.copy(downloadUrl =
            "https://huggingface.co/owner/repo/resolve/main/other.gguf").hasTrustedSource())
    }

    @Test fun ramAwareRecommendationNeverTriggersImplicitCloudEditing() {
        val installed = AltivOfflineModelCatalog.models.map { it.id }.toSet()
        val task = AltivOfflineCodingPolicy.Task.CODING
        val lowRam = AltivOfflineCodingPolicy.recommend(task, 4, installed)
        assertEquals("qwen25coder-15b-q4km", lowRam.modelId)
        assertFalse(lowRam.needsOnlineTools)
        val highRam = AltivOfflineCodingPolicy.recommend(task, 12, installed)
        assertEquals("qwen25coder-7b-q4km", highRam.modelId)
        val complex = AltivOfflineCodingPolicy.recommend(
            AltivOfflineCodingPolicy.Task.LARGE_REPOSITORY, 4, installed
        )
        assertTrue(complex.needsOnlineTools)
        assertTrue(complex.rationale.contains("ferramentas"))
        assertEquals(null,
            AltivOfflineCodingPolicy.recommend(task, 4, emptySet()).modelId)
    }

    @Test fun downloadsAreOptInAndOfflineRuntimeIsNotMisrepresented() {
        val downloader = File(
            "src/main/java/com/openminis/app/offline/AltivOfflineModelDownloads.kt"
        ).readText()
        val screen = File(
            "src/main/java/com/openminis/app/ui/settings/AltivOfflineModelsScreen.kt"
        ).readText()
        val settings = File(
            "src/main/java/com/openminis/app/ui/settings/SettingsScreen.kt"
        ).readText()
        val nav = File(
            "src/main/java/com/openminis/app/ui/navigation/AppNavigation.kt"
        ).readText()
        assertTrue(downloader.contains("DownloadManager.Request"))
        assertTrue(downloader.contains("setAllowedOverMetered(!wifiOnly)"))
        assertTrue(downloader.contains("validGguf("))
        assertTrue(downloader.contains("appContext.getExternalFilesDir"))
        assertTrue(screen.contains("confirmDownload"))
        assertTrue(screen.contains("Confirmar download"))
        assertTrue(screen.contains("alternar para APIs online"))
        assertTrue(screen.contains("Conversar offline"))
        assertTrue(screen.contains("Testar offline"))
        assertTrue(settings.contains("onOfflineModelsClick"))
        assertTrue(screen.contains("AltivOfflineRunTestDialog"))
        assertTrue(nav.contains("Routes.ALTIV_OFFLINE_MODELS"))
    }
}
