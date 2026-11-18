package com.openminis.app.ui.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AltivPrematureWorkContinuationTest {
    private fun should(
        text: String, reason: String? = "stop", tools: Boolean = true,
        helper: Boolean = false, waiting: Boolean = false, retries: Int = 0,
    ) = AltivPrematureWorkContinuation.shouldRetry(
        text, reason, tools, helper, waiting, retries
    )

    @Test fun resumesCleanAnnouncementAfterSuccessfulShellTool() {
        assertTrue(should(
            "Servidor ativo (HTTP 200), build aprovado.\n" +
                "Agora vou executar a auditoria de viewport e breakpoints via JavaScript no navegador."
        ))
        assertTrue(should("Vou verificar o menu do site usando o navegador.", "end_turn"))
        assertTrue(should("Em seguida vou testar o formulário."))
    }

    @Test fun resumesWhenNemotronPromisesActionAfterStatusOnSameLine() {
        // Regression from the Android screenshot (ALTIVE Build #156).
        assertTrue(should(
            "Servidor ativo (HTTP 200). Agora vou fazer a inspeção visual completa dos 5 problemas reportados."
        ))
        assertTrue(should(
            "Build aprovado. Agora vou executar a auditoria de viewport e breakpoints via JavaScript no navegador."
        ))
        // Only the trailing sentence matters: never resume after a real conclusion.
        assertFalse(should(
            "Agora vou executar a auditoria. Auditoria concluída e relatório apresentado."
        ))
        assertFalse(should(
            "Servidor ativo (HTTP 200). Agora vou fazer a inspeção visual completa dos 5 problemas reportados.",
            retries = AltivPrematureWorkContinuation.MAX_RETRIES
        ))
        assertTrue(AltivPrematureWorkContinuation.needsUserVisibleInterruption(
            "Servidor ativo (HTTP 200). Agora vou fazer a inspeção visual completa dos 5 problemas reportados.",
            "stop", true, false, false, 2
        ))
    }

    @Test fun neverContinuesAnActualAnswerOrInformationalMessage() {
        assertFalse(should("Site corrigido. Typecheck e build aprovados."))
        assertFalse(should("O próximo passo seria validar o mobile."))
        assertFalse(should("Você pode executar este teste manualmente."))
        assertFalse(should("Não vou executar sem sua autorização."))
        assertFalse(should("Posso fazer isso se você quiser."))
        assertFalse(should(""))
    }

    @Test fun neverOverridesProviderOrUserSafetySignals() {
        assertFalse(should("Agora vou executar os testes.", reason = "length"))
        assertFalse(should("Agora vou executar os testes.", reason = null))
        assertFalse(should("Agora vou executar os testes.", tools = false))
        assertFalse(should("Agora vou executar os testes.", helper = true))
        assertFalse(should("Agora vou executar os testes.", waiting = true))
    }

    @Test fun boundedRetriesAndVisibleInterruption() {
        val text = "Agora vou executar o teste no navegador."
        assertTrue(should(text, retries = 0))
        assertTrue(should(text, retries = 1))
        assertFalse(should(text, retries = 2))
        assertTrue(AltivPrematureWorkContinuation.needsUserVisibleInterruption(
            text, "stop", true, false, false, 2
        ))
        assertFalse(AltivPrematureWorkContinuation.needsUserVisibleInterruption(
            "Build e testes concluídos.", "stop", true, false, false, 2
        ))
    }

    @Test fun agentLoopGuardsBudgetsAndPersistsResumeNudge() {
        val source = File("src/main/java/com/openminis/app/ui/chat/ChatViewModel.kt").readText()
        assertTrue(source.contains("!turnToolBudget.exhausted() && turn + 1 < turnCap"))
        assertTrue(source.contains("AltivPrematureWorkContinuation.shouldRetry"))
        assertTrue(source.contains("AltivPrematureWorkContinuation.needsUserVisibleInterruption"))
        assertTrue(source.contains("prematureActionNudges++"))
        assertTrue(source.contains("markLiveInterruption()"))
        assertTrue(source.contains("chatRepository.appendMessage(nudgeSid"))
    }
}
