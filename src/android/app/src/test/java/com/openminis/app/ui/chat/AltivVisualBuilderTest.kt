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

    @Test fun webCreationWithoutReferenceReceivesPremiumWorkflowAndPreview() {
        val d = AltivVisualBuilder.decide(
            "Crie uma landing page premium para barbearia com fotos reais",
            false, emptyList(), false, false)
        assertEquals(AltivVisualBuilder.Route.WEB_PROJECT, d.route)
        assertTrue(d.instructions.contains("localhost"))
        assertTrue(d.instructions.contains("mobile-first"))
        assertTrue(d.instructions.contains("WCAG AA"))
        assertTrue(d.instructions.contains("visual_audit"))
        assertTrue(d.instructions.contains("HEURISTIC"))
        assertTrue(d.instructions.contains("screenshots"))
        assertTrue(d.instructions.contains("rollback regressions"))
    }

    @Test fun requestForExistingLocalPreviewReceivesVerifiedServerRule() {
        val d = AltivVisualBuilder.decide(
            "Me dê o localhost para visualizar",
            false, emptyList(), false, false)
        assertEquals(AltivVisualBuilder.Route.WEB_PROJECT, d.route)
        assertTrue(d.instructions.contains("ACTUAL port"))
        assertFalse(d.blocked)
    }

    @Test fun permanentAssetRulesApplyToAllNewAndUpdatedWebProjects() {
        val scenarios = listOf(
            AltivVisualBuilder.decide("Crie uma landing page para uma concessionária", false, emptyList(), false, false),
            AltivVisualBuilder.decide("Corrija o site da Vértice Motors", false, emptyList(), false, false),
            AltivVisualBuilder.decide("Refaça um site igual ao print", true, listOf("ref.png"), true, false),
            AltivVisualBuilder.decide("Refaça o site usando o projeto ZIP de referência", false, listOf("site.zip"), false, false),
        )
        for (scenario in scenarios) {
            val rules = scenario.instructions
            assertTrue("root quality policy missing for ${scenario.route}", rules.contains("ALTIVE ROOT QUALITY MEMORY"))
            assertTrue(rules.contains("SEMANTIC IMAGE CHECK NOT VERIFIED"))
            assertTrue(rules.contains("placeholder image"))
            assertTrue(rules.contains("real mobile/desktop screenshots"))
            assertTrue(rules.contains("BLOCKED or NOT_VERIFIED"))
            assertTrue(rules.contains("Do not use this section as authorization to edit another repository"))
        }
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
