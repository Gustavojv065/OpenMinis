package com.openminis.app.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AltivVisualBuilderTest {
    @Test fun blindImageRequestIsStoppedBeforeTokensAreSpent() {
        val d = AltivVisualBuilder.decide("Site exatamente igual à imagem de referência",
            true, listOf("ref.png"), false, false)
        assertEquals(AltivVisualBuilder.Route.NEEDS_VISION, d.route)
        assertTrue(d.blocked)
        assertTrue(AltivVisualBuilder.blockedNotice().contains("nenhuma API paga"))
    }

    @Test fun visionGroupRequiresRealToolRead() {
        val d = AltivVisualBuilder.decide("Refaça o layout do site igual ao print",
            true, listOf("print.jpg"), false, true)
        assertEquals(AltivVisualBuilder.Route.VISION_GROUP, d.route)
        assertTrue(d.instructions.contains("read_image"))
        assertTrue(d.instructions.contains("Visual QA NOT VERIFIED"))
    }

    @Test fun nativeVisionStillNeedsEvidence() {
        val d = AltivVisualBuilder.decide("Landing igual à referência",
            true, listOf("ref.webp"), true, false)
        assertEquals(AltivVisualBuilder.Route.NATIVE_VISION, d.route)
        assertTrue(d.instructions.contains("not proof"))
        assertTrue(d.instructions.contains("360/390px"))
    }

    @Test fun projectZipOverridesUnavailableVision() {
        val d = AltivVisualBuilder.decide("Site igual à imagem, use o ZIP anexado",
            true, listOf("ref.png", "react.zip"), false, false)
        assertEquals(AltivVisualBuilder.Route.PROJECT_FILES, d.route)
        assertFalse(d.blocked)
        assertTrue(d.instructions.contains("extract safely"))
        assertTrue(d.instructions.contains("Do not run"))
    }

    @Test fun unrelatedImagesAndZipsAreNotIntercepted() {
        assertEquals(AltivVisualBuilder.Route.NONE,
            AltivVisualBuilder.decide("O que tem nesta foto?", true, listOf("photo.jpg"),
                false, false).route)
        assertEquals(AltivVisualBuilder.Route.NONE,
            AltivVisualBuilder.decide("Explique esse ZIP", false, listOf("code.zip"),
                false, false).route)
        assertEquals(AltivVisualBuilder.Route.NONE,
            AltivVisualBuilder.decide("Site igual à referência", false, emptyList(),
                false, false).route)
    }
}
