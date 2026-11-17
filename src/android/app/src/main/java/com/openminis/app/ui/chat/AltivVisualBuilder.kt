package com.openminis.app.ui.chat

/**
 * Vision-aware route for a website reference image or an attached project.
 * Never silently changes providers, enables a paid model, or claims to see pixels.
 */
internal object AltivVisualBuilder {
    enum class Route { NONE, NATIVE_VISION, VISION_GROUP, PROJECT_FILES, NEEDS_VISION, WEB_PROJECT }
    data class Decision(val route: Route, val instructions: String = "") {
        val blocked: Boolean get() = route == Route.NEEDS_VISION
    }

    private val siteWords = listOf("site", "website", "landing", "página", "pagina", "web",
        "react", "layout", "interface", "frontend", "front-end", "ui/ux", "design",
        "galeria", "showroom", "portfólio", "portfolio", "carrossel", "cards", "hero",
        "menu mobile", "rodapé", "imagens do site", "fotos do site",
        "sistema", "aplicativo", "dashboard", "painel", "portal", "vitrine",
        "loja virtual", "e-commerce", "catálogo", "catalogo",
        "formulário", "formulario", "responsividade", "contraste",
        "tela", "tipografia", "navegação", "navegacao", "botões", "botoes",
        "agendamento", "fotos", "carrossel", "section", "seção", "secao")
    private val refWords = listOf("referência", "referencia", "imagem", "print", "captura",
        "screenshot", "igual", "idêntic", "identic", "fidelidade", "replic",
        "reprodu", "copi", "clon", "visual", "modelo", "anex")

    private val taskVerbs = listOf("crie", "criar", "cria ", "desenvolva", "desenvolver",
        "melhore", "melhorar", "corrija", "corrigir", "atualize", "atualizar",
        "construa", "refaça", "refazer", "faça um", "fazer um",
        "ajuste", "ajustar", "otimize", "otimizar", "conserte", "repare",
        "evolua", "evoluir", "aperfeiçoe", "aperfeicoe", "refine",
        "revise", "revisar", "melhoremos", "troque", "substitua",
        "implemente", "implementar", "modernize", "personalize")

    private fun isWebProjectTask(message: String): Boolean {
        val t = message.lowercase()
        return (siteWords.any { it in t } && taskVerbs.any { it in t }) ||
            (listOf("localhost", "preview", "prévia local") .any { it in t } &&
                listOf("abra", "inicie", "me dê", "entregue", "mostre", "visualizar").any { it in t })
    }

    fun decide(
        message: String,
        hasImage: Boolean,
        attachmentNames: List<String>,
        supportsNativeVision: Boolean,
        visionGroupConfigured: Boolean,
    ): Decision {
        val hasProjectZip = attachmentNames.any { it.trim().lowercase().endsWith(".zip") }
        if (!hasImage && !hasProjectZip) {
            return if (isWebProjectTask(message))
                Decision(Route.WEB_PROJECT, instructionsFor(Route.WEB_PROJECT))
            else Decision(Route.NONE)
        }
        val text = message.lowercase()
        val relevant = (siteWords.any { it in text } && refWords.any { it in text }) ||
            listOf("exatamente igual", "igual à imagem", "igual a imagem",
                "reproduza esta imagem", "copie a imagem", "clone esta tela").any { it in text }
        if (!relevant) return Decision(Route.NONE)
        val route = when {
            hasProjectZip -> Route.PROJECT_FILES
            !hasImage -> Route.NONE
            supportsNativeVision -> Route.NATIVE_VISION
            visionGroupConfigured -> Route.VISION_GROUP
            else -> Route.NEEDS_VISION
        }
        return Decision(route, if (route == Route.NONE) "" else instructionsFor(route))
    }

    fun blockedNotice(): String =
        "ALTIVE Visual Builder: o modelo selecionado não declara suporte a imagens e " +
        "não há Grupo de Visão configurado. Para reproduzir a referência, " +
        "selecione um modelo que aceite imagens, configure um Grupo de Visão, " +
        "ou anexe um ZIP com o projeto e os recursos visuais reais. " +
        "Seu texto e anexos foram preservados; nenhuma API paga foi acionada."

    private fun instructionsFor(route: Route): String = """
[ALTIVE VISUAL BUILDER — premium web creation, reference and preview workflow]
${AltivRootQualityMemory.instructions}
${AltivDevToolchainPolicy.instructions}
- Detect available enabled frontend/UI-UX/visual-QA skills; read their actual SKILL.md
  before using. Never invent loaded skills or results.
- Inspect existing project files and all supplied assets before editing.
- For layout fidelity map the header, hero image/text/CTAs, service cards,
  differentiators, about section and contact section before coding.
- Use supplied image assets and components instead of replacing them with giant
  Unicode arrows or globally resized SVG icons.
- Before redesigning, create a reversible checkpoint. Preserve functional existing
  sections and compare BEFORE / AFTER; rollback regressions.
- For a new site, identify the client, target audience, and brand identity first.
  When real browsing is available, investigate 2-4 actual relevant professional
  references for layout/typography/palette/mobile UX, then make an ORIGINAL design.
  No copying images/designs without rights, no invented references.
- Apply mobile-first layout at 360/390px and balanced desktop at 1440px; use a
  hamburger on small widths, readable body type, 44px touch targets, and WCAG AA
  contrast including footer and Google review blocks.
- Keep professional monochrome icons; no giant emojis, poor contrast, fake
  reviews, dummy ratings, or invisible empty sections. 3D/animation is optional,
  lightweight and must respect reduced-motion. Content must remain visible when
  JavaScript animations fail.
- Run real typecheck/build; use browser_use set_viewport then visual_audit at 390px
  and 1440px, inspect warnings, then take actual screenshots. The DOM audit
  is HEURISTIC only, not visual sign-off. Check overflowing elements, broken
  images, hidden sections, small touch targets and approximate contrast.
- Inspect screenshots at 360/390px mobile and 1440px
  desktop, compare section-by-section with the reference, and correct defects.
- Iteration limit: at most two visual refinement cycles; do not loop browser_use
  or Execute JS without an identifiable code change.
- A passing build, HTTP 200 or a written report is NOT visual validation.
  Confirm actual on-screen visibility in EVERY section; DOM presence alone is not
  visual validation. If screenshots cannot be compared, mark Visual QA NOT VERIFIED.
- After EVERY web creation or update, start or reuse the correct local web server.
  Parse its ACTUAL port, verify HTTP AND page loading, then provide the clickable
  localhost preview URL. Verify that an update is visible in the preview.
  Keep the process running for the user's session when supported.
  Never fabricate a running server, screenshot, or verified result.
- For source assets, verify paths, permissions, image decoding and license.
  Don't assume screenshots or Google Business data are accessible.
- Do not claim pixel-perfect similarity without inspecting the generated and
  reference images. Respect reduced motion and accessibility.
- Never pay for or switch to another API/model automatically.
${routeSpecific(route)}
[END ALTIVE VISUAL BUILDER]
""".trimIndent()

    private fun routeSpecific(route: Route): String = when (route) {
        Route.NATIVE_VISION -> "- Catalogue declares image input, but that is not proof " +
            "of image decoding. FIRST describe the actual reference layout from the " +
            "attached pixels. If the model cannot see details, stop and disclose it."
        Route.VISION_GROUP -> "- Main model is text-only. FIRST call read_image on the " +
            "real attachment path, requesting a description of layout geometry, " +
            "hero, card grid, sizes and spacing using the configured Vision Group. " +
            "If it fails, do not guess visual details or change providers."
        Route.WEB_PROJECT -> "- Site task without a reference image: follow the " +
            "premium workflow, run build + visible QA and deliver verified localhost " +
            "automatically; do not ask the customer to type npm commands."
        Route.PROJECT_FILES -> "- A project ZIP is attached. Inspect the file manifest, " +
            "extract safely into a NEW workspace directory (reject unsafe paths), " +
            "read actual React/CSS/image files, and build that project. Do not run " +
            "untrusted ZIP scripts before reviewing them. If the model cannot view " +
            "the screenshot, use ZIP assets but do not claim visual comparison."
        else -> ""
    }
}
