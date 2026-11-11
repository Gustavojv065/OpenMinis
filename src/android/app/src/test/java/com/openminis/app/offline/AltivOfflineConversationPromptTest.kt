package com.openminis.app.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivOfflineConversationPromptTest {
    private val user = AltivOfflineConversationPrompt.Role.USER
    private val assistant = AltivOfflineConversationPrompt.Role.ASSISTANT

    @Test fun preservesRecentTurnsWithoutHugeContext() {
        val prior = (1..24).flatMap { index ->
            listOf(
                AltivOfflineConversationPrompt.Turn(user, "Pedido $index"),
                AltivOfflineConversationPrompt.Turn(assistant, "Resposta $index"),
            )
        }
        val built = AltivOfflineConversationPrompt.build(prior, "Adicione CSS ao botão")
        assertTrue(built.length <= AltivOfflineConversationPrompt.MAX_PROMPT_CHARS)
        assertTrue(built.contains("Resposta 24"))
        assertFalse(built.contains("Resposta 1\n"))
        assertTrue(built.contains("Adicione CSS ao botão"))
        assertTrue(built.contains("Não diga que alterou arquivos"))
    }

    @Test fun historyIsBoundedPerModel() {
        val source = (1..35).map {
            AltivOfflineConversationPrompt.Turn(user, "Mensagem $it")
        }
        val bounded = AltivOfflineConversationPrompt.append(
            source, AltivOfflineConversationPrompt.Turn(assistant, "x".repeat(2700))
        )
        assertEquals(16, bounded.size)
        assertEquals(2400, bounded.last().text.length)
        assertEquals("Mensagem 21", bounded.first().text)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyUserMessageIsRejected() {
        AltivOfflineConversationPrompt.build(emptyList(), "    ")
    }

    @Test fun errorMapperDoesNotReturnGenericFailureForHttpCodes() {
        assertTrue(AltivOfflineDownloadErrors.describe(404, false).contains("HTTP 404"))
        assertTrue(AltivOfflineDownloadErrors.describe(503, false).contains("HTTP 503"))
        assertTrue(AltivOfflineDownloadErrors.describe(999, false).contains("999"))
    }

    @Test fun offlineRouteAndRecoveryButtonsArePresent() {
        val root = java.io.File("src/main/java/com/openminis/app")
        val nav = root.resolve("ui/navigation/AppNavigation.kt").readText()
        val screen = root.resolve("ui/settings/AltivOfflineModelsScreen.kt").readText()
        val chat = root.resolve("ui/settings/AltivOfflineChatScreen.kt").readText()
        val download = root.resolve("offline/AltivOfflineModelDownloads.kt").readText()
        assertTrue(nav.contains("Routes.ALTIV_OFFLINE_CHAT"))
        assertTrue(nav.contains("onChatModelClick"))
        assertTrue(screen.contains("Conversar offline"))
        assertTrue(screen.contains("Importar GGUF"))
        assertTrue(screen.contains("Abrir no navegador"))
        assertTrue(download.contains("DownloadManager.COLUMN_REASON"))
        assertTrue(download.contains("importFromUri"))
        assertTrue(chat.contains("AltivOfflineConversationStore"))
        assertTrue(chat.contains("inference.complete(current, prompt)"))
        assertFalse(chat.contains("ProviderFactory.create"))
    }

    @Test fun modelPickerAllowsDownloadingBeforeFirstOfflineModel() {
        val root = java.io.File("src/main/java/com/openminis/app")
        val picker = root.resolve("ui/chat/ChatModelPickerSheet.kt").readText()
        val chat = root.resolve("ui/chat/ChatScreen.kt").readText()
        val scaffold = root.resolve("ui/navigation/ChatSplitScaffold.kt").readText()
        assertTrue(picker.contains("item(key = \"altiv-offline-models\")"))
        assertFalse(picker.contains("if (installedOfflineModels.isNotEmpty()) {"))
        assertTrue(picker.contains("Nenhum GGUF compatível instalado"))
        assertTrue(picker.contains("onManageOfflineModels()"))
        assertTrue(chat.contains("onManageOfflineModelsClick()"))
        assertTrue(scaffold.contains("onManageOfflineModelsClick = { navController.safeNavigate(Routes.ALTIV_OFFLINE_MODELS) }"))
    }

    @Test fun offlineHistoryIsScopedToMainChatWithoutBreakingLegacyKeys() {
        val model = "qwen3-06b-q4km"
        assertEquals("messages_" + model, AltivOfflineConversationStore.keyFor(model))
        val first = AltivOfflineConversationStore.keyFor(model, "session-a")
        val second = AltivOfflineConversationStore.keyFor(model, "session-b")
        assertTrue(first.startsWith("messages_inchat_"))
        assertFalse(first == second)
        assertFalse(first == AltivOfflineConversationStore.keyFor("qwen25coder-15b-q4km", "session-a"))
        assertEquals(first, AltivOfflineConversationStore.keyFor(model, "session-a"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun offlineHistoryRejectsBlankSessionId() {
        AltivOfflineConversationStore.keyFor("qwen3-06b-q4km", "  ")
    }

    @Test fun mainChatUsesInlineOfflineModeWithoutCloudProviderRouting() {
        val root = java.io.File("src/main/java/com/openminis/app")
        val chat = root.resolve("ui/chat/ChatScreen.kt").readText()
        val screen = root.resolve("ui/settings/AltivOfflineChatScreen.kt").readText()
        val scaffold = root.resolve("ui/navigation/ChatSplitScaffold.kt").readText()
        assertTrue(chat.contains("var inlineOfflineModelId by rememberSaveable(sessionId)"))
        assertTrue(chat.contains("inlineOfflineModelId = modelId"))
        assertTrue(chat.contains("if (isStreaming) {"))
        assertTrue(chat.contains("Aguarde a resposta online terminar"))
        assertTrue(chat.contains("chatSessionId = stableOfflineSessionId"))
        assertTrue(chat.contains("sessionId.takeUnless { it.startsWith(\"__new__\") }"))
        assertTrue(chat.contains("AltivOfflineChatScreen("))
        assertTrue(screen.contains("store.read(it, chatSessionId)"))
        assertTrue(screen.contains("store.save(current.id, finalTurns, chatSessionId)"))
        assertFalse(scaffold.contains("onOfflineModelClick = { modelId -> navController.safeNavigate(Routes.offlineChat(modelId)) }"))
        assertTrue(scaffold.contains("onManageOfflineModelsClick"))
    }
}
