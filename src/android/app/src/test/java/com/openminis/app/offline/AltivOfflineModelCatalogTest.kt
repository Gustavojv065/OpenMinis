package com.openminis.app.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AltivOfflineModelCatalogTest {
    @Test fun onlyTwoCuratedSmallTextModelsInFirstPhase() {
        val models = AltivOfflineModelCatalog.models
        assertEquals(2, models.size)
        assertEquals(models.size, models.map { it.id }.toSet().size)
        assertTrue(models.all { it.category == AltivOfflineCategory.TEXT })
        assertTrue(models.all { it.sizeBytes in 150_000_000L..700_000_000L })
        assertTrue(models.all { it.fileName.endsWith(".gguf") })
        assertTrue(models.all { it.license.isNotBlank() && it.sourceUrl.isNotBlank() })
        assertTrue(models.all { it.hasTrustedSource() })
        assertEquals(6, AltivOfflineCategory.entries.size)
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
        assertTrue(screen.contains("motor de inferência"))
        assertTrue(settings.contains("onOfflineModelsClick"))
        assertTrue(nav.contains("Routes.ALTIV_OFFLINE_MODELS"))
    }
}
