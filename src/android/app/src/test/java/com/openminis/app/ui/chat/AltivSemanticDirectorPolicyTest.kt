package com.openminis.app.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prevents repeating the Aurora Café off-topic photography regression. */
class AltivSemanticDirectorPolicyTest {
    @Test fun cafeWebsiteRequiresArtDirectionAndActualVision() {
        val d = AltivVisualBuilder.decide(
            "Crie uma landing page premium para a cafeteria Aurora Café",
            false, emptyList(), false, false)
        assertEquals(AltivVisualBuilder.Route.WEB_PROJECT, d.route)
        for (term in listOf(
            "ALTIVE SEMANTIC CREATIVE DIRECTOR",
            "subjectRequired",
            "MENU Espresso",
            "NYC skyline",
            "park benches",
            "read_image",
            "Vision Group",
            "visualVerdict=NOT_VERIFIED",
            "do not claim semantic approval",
            "businessNiche",
        )) {
            assertTrue("Required semantic guard missing: $term", d.instructions.contains(term))
        }
    }

    @Test fun semanticRulesMustApplyToOtherBusinesses() {
        for (request in listOf(
            "Crie um site de pet shop com galeria real",
            "Crie uma vitrine imobiliária com fotos",
            "Desenvolva site para restaurante com cardápio"
        )) {
            val result = AltivVisualBuilder.decide(
                request, false, emptyList(), false, false)
            assertEquals(AltivVisualBuilder.Route.WEB_PROJECT, result.route)
            assertTrue(result.instructions.contains("ALTIVE SEMANTIC CREATIVE DIRECTOR"))
            assertTrue(result.instructions.contains("Automat"))
            assertTrue(result.instructions.contains("property listing"))
        }
    }

    @Test fun actualVisualReviewCannotBeFakedByMetadataOrHttp200() {
        val instructions = AltivSemanticDirectorPolicy.instructions
        assertTrue(instructions.contains("Metadata keyword matching alone"))
        assertTrue(instructions.contains("NOT_VERIFIED"))
        assertTrue(instructions.contains("APPROVED/NOT_VERIFIED/REJECTED"))
        assertTrue(instructions.contains("no image-capable model"))
        assertTrue(instructions.contains("read_image"))
        assertTrue(instructions.contains("paid provider"))
    }

    @Test fun regularConversationDoesNotTriggerCostlyImagePipeline() {
        val d = AltivVisualBuilder.decide("Bom dia",
            false, emptyList(), false, false)
        assertEquals(AltivVisualBuilder.Route.NONE, d.route)
        assertFalse(d.instructions.contains("ALTIVE SEMANTIC CREATIVE DIRECTOR"))
    }
}
