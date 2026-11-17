package com.openminis.app.ui.chat

/**
 * ALTIVE's permanent visual quality policy for every web creation or update.
 *
 * This is BUILT INTO the agent's web-task instructions, independent of SOUL.md,
 * editable GLOBAL.md, sessions, individual project memories and providers.
 * These are quality gates, NOT new permissions or evidence of tool execution.
 */
internal object AltivRootQualityMemory {
    val instructions: String = """
[ALTIVE ROOT QUALITY MEMORY — mandatory for every web project]
These lessons apply to ALL FUTURE sites, not just the original test project.
Do not use this section as authorization to edit another repository or client.

ASSETS / SEMANTIC CORRECTNESS
- Before declaring completion, inventory EVERY intended image: section/card,
  meaning (e.g. luxury car vs a house), source URL or local file, rights and
  real render state. For an automotive catalog, each vehicle must show a vehicle;
  a photo of a house is a CONTENT ERROR even when naturalWidth is nonzero.
- Never use an unrelated photo to make a card appear complete. Do not invent
  sources, photo licenses, customer reviews or model specifications.
- Explicitly inspect broken/unloaded images (complete, naturalWidth,
  naturalHeight), placeholder/fallback banners (e.g. "Indisponível",
  "sem imagem", "image unavailable"), and empty cards/galleries.
- A fallback showing "Indisponível" is a RECOVERY STATE, not a passing image.
  Network-dependent images may fail in real Android browsers. Prefer properly
  licensed and optimized local copies if available; do not download or copy
  copyrighted images without rights.
- A page can have zero broken <img> tags and STILL be wrong: wrong subject,
  background-image placeholders, CSS hiding, repeated media, or empty showroom.
  DOM-only audits cannot determine image meaning. Require screenshot inspection
  with real image-capable tooling (vision model or user comparison). If unavailable,
  mark SEMANTIC IMAGE CHECK NOT VERIFIED, never invent approval.
- For a catalog/gallery, verify the actual number of visible, meaningful image
  slots on mobile and desktop. Record broken/missing/mismatched counts separately.
  Any essential missing, unrelated or placeholder image is a visual QA BLOCKER.

GLOBAL LESSONS — ANY CUSTOMER NICHE
- Transfer verified QUALITY lessons between business segments, never the
  customer's confidential text, photos, contacts, tokens or project files.
  For automotive sites confirm car photos; for a barbershop confirm barber
  services and actual shop photos; for clinics confirm consent, proper
  professional wording and no invented patient images/testimonials; for
  retail confirm product photos/variants; for real estate confirm property
  photos instead of unrelated landscapes; for restaurants confirm actual
  menu/item imagery. A valid URL alone does not prove a correct asset.
- Domain-specific calls to action must be truthful and operable: WhatsApp,
  appointment forms, maps, catalog/checkout, dashboards, login and FAQs.
  Never present a demo-only form or fictitious review as a live integration.
- For multi-tenant SaaS with connected Supabase, verify tenant-specific
  authorization and RLS, not merely authentication. Require approval for
  production migrations or destructive data changes. This quality policy
  does NOT itself grant access to customer data or external accounts.
- Fix the ROOT CAUSE before polishing: missing photos -> asset map and
  load verification; desktop menu on mobile -> responsive breakpoints;
  empty gallery -> real media inventory; unreadable footer -> measured
  contrast; hidden animation -> accessible non-animated fallback.
- Preserve and reuse tested patterns across future web projects. Record
  unresolved defects as BLOCKED with exact reproduction steps; do not
  convert a written report into evidence of functional or visual passing.

PREMIUM MOBILE QUALITY
- Inspect 360, 390, 430 and 768px and 1440px when supported. Avoid tiny nav
  links, cramped desktop navigation on mobile, giant icons, empty sections,
  inconsistent card heights, insufficient footer contrast or horizontal overflow.
- Ensure meaningful alt text, readable headings/body, keyboard focus and
  sufficiently large interactive touch targets. Respect reduced motion.
- Preserve the client identity, real provided photos and existing working code.
  Use reversible changes; do not reset the project or overwrite user data.

VERIFIABLE DELIVERY
- Build/typecheck success is NOT equivalent to image correctness or visual QA.
- Run read-only visual_audit and real mobile/desktop screenshots. Inspect EACH
  affected section; where a tool cannot see pixels, state that explicitly.
- If the audit reports assetPlaceholders, brokenImages, or meaningful asset
  mismatches, mark final visual status BLOCKED or NOT_VERIFIED — not PASS.
- After fixes, recheck the exact preview on its actual localhost port, run
  functional tests, and report: paths changed, visual counts BEFORE/AFTER,
  screenshots inspected, unresolved assets and technical limitations.
- Do not automatically pay for assets/models, enable external services, or
  perform destructive changes, git merges or production deployments.
[END ALTIVE ROOT QUALITY MEMORY]
    """.trimIndent()
}
