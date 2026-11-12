package com.openminis.app.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AltivOfflineTaskRouterTest {
    @Test fun designRequestsAreCodeNotImageGeneration() {
        assertEquals(AltivOfflineTaskRouter.Task.CODE, AltivOfflineTaskRouter.classify(
            "Crie uma landing page de Pet Shop com fotos reais e cards 3D em HTML"
        ))
        assertEquals(AltivOfflineTaskRouter.Task.CODE, AltivOfflineTaskRouter.classify(
            "Corrija o CSS do site e mostre o código"
        ))
        assertEquals(AltivOfflineTaskRouter.Task.TEXT, AltivOfflineTaskRouter.classify(
            "Olá, tudo bem?"
        ))
    }

    @Test fun requestsForMediaAreRecognizedButNotFakeExecutions() {
        assertEquals(AltivOfflineTaskRouter.Task.IMAGE, AltivOfflineTaskRouter.classify(
            "Gere uma imagem realista de um gato"
        ))
        assertEquals(AltivOfflineTaskRouter.Task.VIDEO, AltivOfflineTaskRouter.classify(
            "Crie um vídeo cinematográfico de 10 segundos"
        ))
        assertEquals(AltivOfflineTaskRouter.Task.AUDIO, AltivOfflineTaskRouter.classify(
            "Crie uma narração em voz de uma história"
        ))
        assertFalse(AltivOfflineTaskRouter.isRunnable(AltivOfflineTaskRouter.Task.IMAGE))
        assertFalse(AltivOfflineTaskRouter.isRunnable(AltivOfflineTaskRouter.Task.VIDEO))
        assertFalse(AltivOfflineTaskRouter.isRunnable(AltivOfflineTaskRouter.Task.AUDIO))
        assertTrue(AltivOfflineTaskRouter.unsupportedMessage(
            AltivOfflineTaskRouter.Task.IMAGE
        ).contains("Não foi produzida nenhuma imagem"))
    }

    @Test fun routingKeepsCurrentWhenOnlyOversizedAlternativeExists() {
        val small = requireNotNull(AltivOfflineModelCatalog.byId("qwen25coder-15b-q4km"))
        val large = requireNotNull(AltivOfflineModelCatalog.byId("qwen25coder-7b-q4km"))
        assertEquals(small, AltivOfflineTaskRouter.chooseTextModel(
            small, listOf(small, large), AltivOfflineTaskRouter.Task.CODE,
            2_000_000_000L, 8,
        ))
        assertEquals(large, AltivOfflineTaskRouter.chooseTextModel(
            small, listOf(small, large), AltivOfflineTaskRouter.Task.CODE,
            6_000_000_000L, 12,
        ))
    }


    @Test fun imageForWhatsAppDoesNotBecomeCodeAndRamMatters() {
        assertEquals(AltivOfflineTaskRouter.Task.IMAGE, AltivOfflineTaskRouter.classify(
            "Crie uma imagem de gato para WhatsApp e meu site"
        ))
        val small = requireNotNull(AltivOfflineModelCatalog.byId("qwen25coder-15b-q4km"))
        val large = requireNotNull(AltivOfflineModelCatalog.byId("qwen25coder-7b-q4km"))
        assertEquals(small, AltivOfflineTaskRouter.chooseTextModel(
            small, listOf(small, large), AltivOfflineTaskRouter.Task.CODE,
            6_000_000_000L, 8,
        ))
    }

    @Test fun chatUsesLocalRoutesAndRealHtmlPreviewWithoutSilentShell() {
        val root = File("src/main/java/com/openminis/app")
        val vm = root.resolve("ui/chat/ChatViewModel.kt").readText()
        val picker = root.resolve("ui/chat/ChatModelPickerSheet.kt").readText()
        val markdown = root.resolve("ui/chat/StreamingMarkdownText.kt").readText()
        val project = root.resolve("ui/chat/AltivHtmlProjectPreview.kt").readText()
        assertTrue(vm.contains("AltivOfflineTaskRouter.classify(trimmed)"))
        assertTrue(vm.contains("AltivOfflineTaskRouter.chooseTextModel("))
        assertTrue(vm.contains("getBoolean(\"enabled\", true)"))
        assertTrue(picker.contains("Roteador automático offline"))
        assertTrue(picker.contains("onOfflineRouterChange"))
        assertTrue(markdown.contains("Salvar + prévia"))
        assertTrue(markdown.contains("AltivHtmlProjectPreview.save("))
        assertTrue(project.contains("filesDir, \"altiv-projects\""))
        assertTrue(project.contains("settings.javaScriptEnabled = false"))
        assertTrue(project.contains("settings.allowFileAccess = false"))
        assertTrue(project.contains("Localhost real: "))
        assertTrue(project.contains("AltivOfflinePreviewServer(project)"))
        val server = root.resolve("ui/chat/AltivOfflinePreviewServer.kt").readText()
        assertTrue(server.contains("127.0.0.1"))
        assertTrue(server.contains("server.close()"))
        assertTrue(server.contains("GET /index.html"))
        assertFalse(server.contains("shell_execute"))
        assertFalse(project.contains("shell_execute"))
    }
}
