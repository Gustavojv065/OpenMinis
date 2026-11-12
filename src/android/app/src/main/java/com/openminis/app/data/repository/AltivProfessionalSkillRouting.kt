package com.openminis.app.data.repository

/**
 * Helps the existing agent discover the best installed development skills.
 * Does not invent skills or silently enable disabled ones.
 */
internal object AltivProfessionalSkillRouting {
    private val topics = listOf(
        "frontend", "front-end", "backend", "back-end", "3d", "three.js",
        "react", "typescript", "ui ux", "ui-ux", "visual",
        "debug", "testing", "testes", "github", "supabase", "security",
    )

    fun priority(name: String, description: String): Int {
        val title = name.lowercase()
        val details = description.lowercase()
        return topics.count { it in title } * 3 + topics.count { it in details }
    }

    const val GUIDANCE = """
ALTIV developer workflow (use only tools you actually have):
- Select the relevant available skills for the user's intent, read their SKILL.md
  before working, and avoid loading unrelated skills that waste tokens.
- For existing projects, inspect the real repository and identify the component
  before editing; preserve authentication, routing and existing bug fixes.
- Make sensible UI decisions for small requests instead of repeatedly asking.
  Ask when authorization, ambiguous targets, data loss or security require it.
- For React/TypeScript/3D, keep responsive layouts, accessible contrast,
  reduced-motion alternatives, performance budgets and tests.
- When asked to CREATE a site or app, deliver project files in the sandbox
  rather than only pasting code in chat when file tools are available.
  Show a verified preview URL only after the server is actually running.
- Before starting a larger feature, define up to three concrete acceptance
  checks; implement the smallest working version, then inspect results.
- Prefer small, reversible diffs; run the actual typecheck, build and tests.
  If tests cannot run, say what was not tested and give exact manual steps.
- Treat terminal output and tool execution as evidence; never claim a tool
  ran just because its call was described in the model's text.
- For any created or edited web page, an HTTP 200 or running localhost is only
  a SERVER check, NOT visual acceptance. Open the real localhost in browser_use
  when enabled. Capture and inspect screenshots at mobile and desktop sizes.
  Verify the header, hero text, images, main sections and CTAs are visible;
  inspect page text/DOM too. An empty white preview is a failure, not success.
- Fix invisible sections before reporting completion: avoid default CSS
  opacity:0, visibility:hidden, zero-height containers, overflow clipping
  or IntersectionObserver reveal animations that never start. Keep content
  visible by default; animations are optional progressive enhancement.
  Respect prefers-reduced-motion and show content if JavaScript is disabled.
- Check external image URLs load and have reasonable alt text, and validate
  mobile layout. If screenshots cannot be inspected, state that visual QA was
  NOT performed; do not claim a finished/verified landing page.
- For connected repos work on a branch and prepare a PR; never merge,
  deploy or change production without the user's authorization.
- Do not fabricate terminal results, live previews, commits or successful tests.
"""
}
