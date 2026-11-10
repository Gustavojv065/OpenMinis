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
        assertEquals(6, AltivOfflineCategory.entries.size)
        assertEquals(3, models.count { it.id.contains("coder") })
        assertTrue(models.all { it.minimumDeviceRamGb in 4..12 })
        assertTrue(models.all { it.codingLevel in 1..4 })
        assertEquals(1, models.count { it.minimumDeviceRamGb >= 10 })
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
        assertTrue(screen.contains("A conversa no chat principal e as ferramentas offline virão depois."))
        assertTrue(screen.contains("Testar offline"))
        assertTrue(settings.contains("onOfflineModelsClick"))
        assertTrue(screen.contains("AltivOfflineRunTestDialog"))
        assertTrue(nav.contains("Routes.ALTIV_OFFLINE_MODELS"))
    }
}
