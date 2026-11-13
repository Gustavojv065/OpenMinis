package com.openminis.app.ui.chat

/**
 * Vision-aware route for a website reference image or an attached project.
 * Never silently changes providers, enables a paid model, or claims to see pixels.
 */
internal object AltivVisualBuilder {
    enum class Route { NONE, NATIVE_VISION, VISION_GROUP, PROJECT_FILES, NEEDS_VISION }
    data class Decision(val route: Route, val instructions: String = "") {
        val blocked: Boolean get() = route == Route.NEEDS_VISION
    }

    private val siteWords = listOf("site", "website", "landing", "página", "pagina", "web",
        "react", "layout", "interface", "frontend", "front-end", "ui/ux", "design")
    private val refWords = listOf("referência", "referencia", "imagem", "print", "captura",
        "screenshot", "igual", "idêntic", "identic", "fidelidade", "replic",
        "reprodu", "copi", "clon", "visual", "modelo", "anex")

    fun decide(
        message: String,
        hasImage: Boolean,
        attachmentNames: List<String>,
        supportsNativeVision: Boolean,
        visionGroupConfigured: Boolean,
    ): Decision {
        val hasProjectZip = attachmentNames.any { it.trim().lowercase().endsWith(".zip") }
        if (!hasImage && !hasProjectZip) return Decision(Route.NONE)
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
[ALTIVE VISUAL BUILDER — attached reference workflow]
- Detect available enabled frontend/UI-UX/visual-QA skills; read their actual SKILL.md
  before using. Never invent loaded skills or results.
- Inspect existing project files and all supplied assets before editing.
- For layout fidelity map the header, hero image/text/CTAs, service cards,
  differentiators, about section and contact section before coding.
- Use supplied image assets and components instead of replacing them with giant
  Unicode arrows or globally resized SVG icons.
- Run real typecheck/build; inspect screenshots at 360/390px mobile and 1440px
  desktop, compare section-by-section with the reference, and correct defects.
- Iteration limit: at most two visual refinement cycles; do not loop browser_use
  or Execute JS without an identifiable code change.
- A passing build, HTTP 200 or a written report is NOT visual validation.
  If screenshots cannot be compared, mark Visual QA NOT VERIFIED.
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
        Route.PROJECT_FILES -> "- A project ZIP is attached. Inspect the file manifest, " +
            "extract safely into a NEW workspace directory (reject unsafe paths), " +
            "read actual React/CSS/image files, and build that project. Do not run " +
            "untrusted ZIP scripts before reviewing them. If the model cannot view " +
            "the screenshot, use ZIP assets but do not claim visual comparison."
        else -> ""
    }
}
