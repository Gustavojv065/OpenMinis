package com.openminis.app.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivBusinessIntentPreflightTest {
    @Test fun detailingMustNotUseFuelStationPictures() {
        val d = AltivVisualBuilder.decide(
            "Crie uma landing page para estética automotiva com polimento e vitrificação",
            false, emptyList(), false, false
        )
        assertEquals(AltivVisualBuilder.Route.WEB_PROJECT, d.route)
        val prompt = d.instructions
        assertTrue(prompt.contains("ALTIVE FIRST PASS QUALITY PRECHECK"))
        assertTrue(prompt.contains("IDENTIFIED NICHE: automotive-detailing"))
        assertTrue(prompt.contains("Posto de combustível"))
        assertTrue(prompt.contains("máquina de polimento"))
        assertTrue(prompt.contains("--niche automotive-detailing"))
        assertTrue(prompt.contains("gas station"))
        assertTrue(prompt.contains("read_image"))
        assertTrue(prompt.contains("NOT_VERIFIED"))
    }

    @Test fun preflightIsRequiredAcrossNewAndExistingProjects() {
        for (request in listOf(
            "Crie uma landing page para barbearia premium",
            "Desenvolva site pet shop com galeria",
            "Crie site de cafeteria artesanal",
            "Crie loja de roupas com fotos",
            "Corrija galeria do site",
            "Refine o hero da página"
        )) {
            val d = AltivVisualBuilder.decide(request, false, emptyList(), false, false)
            assertEquals(request, AltivVisualBuilder.Route.WEB_PROJECT, d.route)
            assertTrue(request, d.instructions.contains("FIRST PASS QUALITY PRECHECK"))
            assertTrue(request, d.instructions.contains("Before code generation"))
        }
    }

    @Test fun photoPolicyWorksInFreshChatWithoutSavedMemory() {
        val before = AltivVisualBuilder.decide(
            "Crie uma página para estética automotiva", false, emptyList(), false, false
        )
        val after = AltivVisualBuilder.decide(
            "Crie uma página para estética automotiva", false, emptyList(), false, false
        )
        assertEquals(before.instructions, after.instructions)
        assertTrue(before.instructions.contains("ALTIVE ROOT QUALITY MEMORY"))
        assertTrue(before.instructions.contains("ALTIVE SEMANTIC CREATIVE DIRECTOR"))
    }

    @Test fun generalQuestionsDontTriggerCostlyQualityWorkflow() {
        for (request in listOf("Olá", "O que é polimento automotivo?", "Como funciona o CSS?")) {
            val d = AltivVisualBuilder.decide(request, false, emptyList(), false, false)
            assertEquals(AltivVisualBuilder.Route.NONE, d.route)
            assertFalse(d.instructions.contains("FIRST PASS QUALITY PRECHECK"))
        }
    }
}
