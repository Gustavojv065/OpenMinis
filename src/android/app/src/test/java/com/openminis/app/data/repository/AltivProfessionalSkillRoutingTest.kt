package com.openminis.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivProfessionalSkillRoutingTest {
    @Test fun frontendAnd3dSkillsHaveHigherPriorityThanGenericOnes() {
        assertTrue(
            AltivProfessionalSkillRouting.priority(
                "3D Website Creator", "React Three.js effects"
            ) > AltivProfessionalSkillRouting.priority("General helper", "General writing")
        )
        assertTrue(
            AltivProfessionalSkillRouting.priority(
                "Frontend Design Premium", "Responsive UI UX and TypeScript"
            ) > 0
        )
        assertEquals(0, AltivProfessionalSkillRouting.priority("Cooking", "Recipes"))
    }

    @Test fun codingGuidancePreservesExistingRepoAndRequiresTests() {
        val guidance = AltivProfessionalSkillRouting.GUIDANCE
        assertTrue(guidance.contains("SKILL.md"))
        assertTrue(guidance.contains("typecheck"))
        assertTrue(guidance.contains("without the user's authorization"))
        assertTrue(guidance.contains("repeatedly asking"))
        assertTrue(guidance.contains("project files in the sandbox"))
        assertTrue(guidance.contains("verified preview URL"))
        assertTrue(guidance.contains("what was not tested"))
        assertTrue(guidance.contains("an HTTP 200 or running localhost"))
        assertTrue(guidance.contains("Capture and inspect screenshots"))
        assertTrue(guidance.contains("An empty white preview is a failure"))
        assertTrue(guidance.contains("prefers-reduced-motion"))
    }

    @Test fun developerGuidanceIsActuallyInjectedIntoAgentSystemPrompt() {
        val vm = java.io.File(
            "src/main/java/com/openminis/app/ui/chat/ChatViewModel.kt"
        ).readText()
        assertTrue(
            vm.contains("append(com.openminis.app.data.repository.AltivProfessionalSkillRouting.GUIDANCE)")
        )
    }

    @Test fun currentOpenAiApiKeyCatalogListsQualityAndBudgetChoices() {
        val models = com.openminis.app.data.model.LLMModel.allOpenAI
        val ids = models.map { it.id }
        for (id in listOf(
            "gpt-6-astra", "gpt-6.1-sol",
            "gpt-6.1-terra", "gpt-6-luna",
        )) {
            val model = models.firstOrNull { it.id == id }
            assertTrue("Missing OpenAI official model $id", model != null)
            assertEquals(true, model?.supportsReasoning)
            assertEquals(1, ids.count { it == id })
        }
        // Existing sessions using older model IDs must still load.
        assertTrue(ids.contains("gpt-5.5"))
        assertTrue(ids.contains("gpt-4o-mini"))
    }
}
