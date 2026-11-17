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

    @Test fun standaloneGalleryAndMobileFixesStillReceiveRootQualityMemory() {
        for (request in listOf("Corrija a galeria", "Melhore os cards", "Ajuste o menu mobile", "Atualize o hero")) {
            val d = AltivVisualBuilder.decide(request, false, emptyList(), false, false)
            assertEquals(request, AltivVisualBuilder.Route.WEB_PROJECT, d.route)
            assertTrue(request, d.instructions.contains("ALTIVE ROOT QUALITY MEMORY"))
        }
    }

    @Test fun rootQualityLessonsApplyAcrossCustomerBusinessNiches() {
        val commands = listOf(
            "Crie um sistema de agenda para clínica odontológica",
            "Desenvolva portal para membros da igreja",
            "Modernize a vitrine de uma farmácia",
            "Melhore o catálogo de produtos do mercado",
            "Refine a tela de agendamentos da barbearia",
            "Corrija a responsividade no painel da clínica",
            "Ajuste o formulário da loja virtual",
            "Substitua fotos de veículos no showroom",
            "Evolua o dashboard do restaurante"
        )
        for (command in commands) {
            val d = AltivVisualBuilder.decide(command, false, emptyList(), false, false)
            assertEquals("Missing root policy for $command", AltivVisualBuilder.Route.WEB_PROJECT, d.route)
            assertTrue(d.instructions.contains("GLOBAL LESSONS — ANY CUSTOMER NICHE"))
            assertTrue(d.instructions.contains("Do not use this section as authorization to edit another repository"))
            assertTrue(d.instructions.contains("Any essential missing, unrelated or placeholder image"))
            assertTrue(d.instructions.contains("SEMANTIC IMAGE CHECK NOT VERIFIED"))
            assertTrue(d.instructions.contains("Supabase"))
        }
    }

    @Test fun regularQuestionsDoNotTriggerExpensiveVisualWorkflow() {
        for (command in listOf("Como funciona o Supabase?", "Explique React", "O que é uma loja virtual?",
                            "Qual é o preço do carro?", "Como usar formulário?")) {
            assertEquals(command, AltivVisualBuilder.Route.NONE,
                AltivVisualBuilder.decide(command, false, emptyList(), false, false).route)
        }
    }

    @Test fun firstDevelopmentTaskIncludesReadOnlyToolchainProbe() {
        val commands = listOf(
            "Crie um site premium para um pet shop",
            "Modernize o dashboard da clínica",
            "Desenvolva o portal de clientes"
        )
        for (command in commands) {
            val decision = AltivVisualBuilder.decide(
                command, false, emptyList(), false, false
            )
            assertEquals(AltivVisualBuilder.Route.WEB_PROJECT, decision.route)
            assertTrue(decision.instructions.contains("sh /usr/local/bin/altive-dev-check"))
            assertTrue(decision.instructions.contains("BEFORE calling apk add"))
            assertTrue(decision.instructions.contains("READ-ONLY"))
            assertTrue(decision.instructions.contains("NOT_VERIFIED"))
        }
    }

    @Test fun nonWebProgrammingTasksCheckEnvironmentWithoutVisualRebuild() {
        for (request in listOf(
            "Crie um script Python para exportação de planilhas",
            "Corrija uma API de pagamentos",
            "Atualize código Kotlin do serviço",
            "Desenvolva automação GitHub",
            "Implemente dependências no backend"
        )) {
            val d = AltivVisualBuilder.decide(request, false, emptyList(), false, false)
            assertEquals(request, AltivVisualBuilder.Route.DEV_ENVIRONMENT, d.route)
            assertTrue(d.instructions.contains("sh /usr/local/bin/altive-dev-check"))
            assertFalse(d.instructions.contains("ALTIVE ROOT QUALITY MEMORY"))
        }
        val greeting = AltivVisualBuilder.decide("olá", false, emptyList(), false, false)
        assertEquals(AltivVisualBuilder.Route.NONE, greeting.route)
    }

    @Test fun toolchainCheckerShipsWithRootfsAndDoesNotInstallAnything() {
        val file = java.io.File("src/main/assets/default_mount/usr/local/bin/altive-dev-check")
        assertTrue("bundled toolchain check missing", file.isFile)
        val source = file.readText()
        assertTrue(source.contains("ALTIVE_DEV_CHECK_V1"))
        assertTrue(source.contains("command -v"))
        for (binary in listOf("git", "python3", "pip3", "node", "npm", "npx", "bash", "curl",
                               "wget", "tar", "unzip", "zip", "jq", "make", "gcc", "g++")) {
            assertTrue("Missing diagnostic for $binary", source.contains("probe ") && source.contains(" $binary "))
        }
        assertFalse("Probe must never install packages",
            source.lineSequence().any { it.trimStart().startsWith("apk add ") })
        assertFalse("Probe must not execute curl or wget", source.contains("curl -"))
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
