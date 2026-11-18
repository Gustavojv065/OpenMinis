package com.openminis.app.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivAutonomousSupervisorPolicyTest {
    @Test fun supervisorRoutesEveryBusinessWebsite() {
        for (prompt in listOf(
            "Crie uma landing page premium para cafeteria",
            "Desenvolva site para pet shop",
            "Modernize a vitrine de uma imobiliária",
            "Corrija o layout do restaurante"
        )) {
            val decision = AltivVisualBuilder.decide(prompt, false, emptyList(), false, false)
            assertEquals(prompt, AltivVisualBuilder.Route.WEB_PROJECT, decision.route)
            assertTrue(prompt, decision.instructions.contains("FIVE ROLES"))
            assertTrue(prompt, decision.instructions.contains("read_image"))
            assertTrue(prompt, decision.instructions.contains("memory_get"))
            assertTrue(prompt, decision.instructions.contains("SKILL.md"))
            assertTrue(prompt, decision.instructions.contains("NOT_VERIFIED"))
        }
    }

    @Test fun programmingWithoutWebUsesRealToolsButNotVisualRebuild() {
        val result = AltivVisualBuilder.decide(
            "Corrija uma API em Python", false, emptyList(), false, false)
        assertEquals(AltivVisualBuilder.Route.DEV_ENVIRONMENT, result.route)
        assertTrue(result.instructions.contains("FIVE ROLES"))
        assertTrue(result.instructions.contains("altive-dev-check"))
        assertFalse(result.instructions.contains("ALTIVE ROOT QUALITY MEMORY"))
    }

    @Test fun conversationsWithoutDevelopmentHaveNoSupervisorTax() {
        for (prompt in listOf("Olá", "Que horas são?", "Explique o que é café")) {
            val result = AltivVisualBuilder.decide(prompt, false, emptyList(), false, false)
            assertEquals(prompt, AltivVisualBuilder.Route.NONE, result.route)
            assertTrue(prompt, result.instructions.isEmpty())
        }
    }

    @Test fun journalRecordsAttemptWithoutPretendingQAPassed() {
        assertEquals("NOT_STARTED", AltivAutonomousTaskJournal.acceptsResult(false, false))
        assertEquals("FAILED", AltivAutonomousTaskJournal.acceptsResult(false, true))
        assertEquals("ATTEMPTED_NOT_VERIFIED", AltivAutonomousTaskJournal.acceptsResult(true, true))
        assertEquals("visual_review", AltivAutonomousTaskJournal.phaseForTool("read_image", "implementation"))
        assertEquals("browser_review", AltivAutonomousTaskJournal.phaseForTool("browser_use", "implementation"))
        assertEquals("implementation", AltivAutonomousTaskJournal.phaseForTool("shell_execute", "brief"))
        assertEquals("delegation", AltivAutonomousTaskJournal.phaseForTool("subagent_task", "brief"))
    }

    @Test fun noExternalChangesOrFakePluginClaims() {
        val rules = AltivAutonomousSupervisorPolicy.instructions
        assertTrue(rules.contains("without approval"))
        assertTrue(rules.contains("not a passed QA check"))
        assertTrue(rules.contains("Never pretend"))
        assertTrue(rules.contains("Figma"))
        assertTrue(rules.contains("Superpowers"))
        assertTrue(rules.contains("3D"))
        assertTrue(rules.contains("five"))
    }
}
