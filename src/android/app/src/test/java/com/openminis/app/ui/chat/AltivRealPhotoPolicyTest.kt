package com.openminis.app.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Commercial asset retrieval regression tests across all website niches. */
class AltivRealPhotoPolicyTest {
    @Test fun realPhotosAreRequestedInsteadOfSvgPlaceholders() {
        for (request in listOf(
            "Crie um site para pet shop com fotos reais de cães",
            "Crie galeria com fotos reais para concessionária",
            "Atualize a vitrine do restaurante com fotos de pratos",
            "Melhore fotos da loja virtual",
        )) {
            val decision = AltivVisualBuilder.decide(request, false, emptyList(), false, false)
            assertEquals(request, AltivVisualBuilder.Route.WEB_PROJECT, decision.route)
            assertTrue(request, decision.instructions.contains("ALTIVE REAL PHOTO ASSETS"))
            assertTrue(decision.instructions.contains("altive-photo-search.py"))
            assertTrue(decision.instructions.contains("DOWNLOADED"))
            assertTrue(decision.instructions.contains("ALTIVE-PHOTO-CREDITS.json"))
            assertTrue(decision.instructions.contains("SVG icons/emoji"))
            assertTrue(decision.instructions.contains("SUBJECT comparison"))
        }
    }

    @Test fun photoSearchDoesNotAuthorizeCopyrightViolation() {
        val t = AltivRealPhotoPolicy.instructions
        assertTrue(t.contains("Wikimedia Commons"))
        assertTrue(t.contains("license"))
        assertTrue(t.contains("do not scrape Google"))
        assertTrue(t.contains("NEVER claim a generic"))
        assertTrue(t.contains("No API key") || t.contains("no API key"))
        assertFalse(t.contains("ignore copyright"))
    }

    @Test fun newWebTaskHasSafeMultiSourceInstructions() {
        val d = AltivVisualBuilder.decide(
            "Crie uma landing page moderna para uma barbearia",
            false, emptyList(), false, false
        )
        assertEquals(AltivVisualBuilder.Route.WEB_PROJECT, d.route)
        for (term in listOf(
            "altive-media-search.py", "Pexels", "Pixabay", "Unsplash",
            "Wikimedia", "Openverse", "ALTIVE-PHOTO-CREDITS.json",
            "ALTIVE_PEXELS_API_KEY", "ALTIVE_UNSPLASH_ACCESS_KEY",
            "MUST hotlink", "NOT_VERIFIED", "read_image",
            "no API key", "do not scrape Google"
        )) {
            assertTrue("Missing photo safety requirement: $term",
                d.instructions.contains(term, ignoreCase = true))
        }
    }

    @Test fun ordinaryQuestionsDoNotAutomaticallyDownloadImages() {
        assertEquals(AltivVisualBuilder.Route.NONE,
            AltivVisualBuilder.decide("O que é SVG?", false, emptyList(), false, false).route)
    }
}
