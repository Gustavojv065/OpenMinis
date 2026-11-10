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
- Prefer small, reversible diffs; run the actual typecheck, build and tests.
- For connected repos work on a branch and prepare a PR; never merge,
  deploy or change production without the user's authorization.
- Do not fabricate terminal results, live previews, commits or successful tests.
"""
}
