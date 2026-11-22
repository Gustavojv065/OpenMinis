package com.openminis.app.ui.chat

/**
 * Licensed ALTIVE developer-environment readiness policy.
 *
 * The commercial app automatically provisions and tests its ARM64 sandbox
 * after server-validated license activation, without user terminal commands.
 * This policy remains an extra read-only preflight for coding tasks.
 */
internal object AltivDevToolchainPolicy {
    val instructions: String = """
[ALTIVE DEV ENVIRONMENT — first programming task]
- The licensed ALTIVE app automatically prepares Node.js, NPM/NPX,
  Python/PIP and Git on first entry. Fresh installs use the offline ARM64
  toolchain bundled with the APK; older roots are repaired by the commercial
  onboarding flow. The customer must NOT have to type Shell/PowerShell commands.
- Before the first coding/build task of this session, when shell_execute
  is available and the licensed workspace is ready, run ONCE:
      sh /usr/local/bin/altive-dev-check
  This is a bundled read-only inventory. Do not rerun it on every message.
- Show READY, MISSING, NOT_VERIFIED based solely on ACTUAL tool results.
  If a required tool is missing, guide the customer to the app's automatic
  environment-repair screen. Never fabricate shell output, npm success, a
  localhost URL, a successful Git operation or a passing build.
- Do not tell the customer to install packages manually or require Termux,
  PowerShell or global npm installation. Do not execute apk add yourself as an
  agent: the licensing/onboarding provisioner owns that signed Alpine operation.
- The Alpine PRoot guest is separate from Android host and remote tooling.
  GitHub API/MCP access can still be used when the shell is unavailable.
  TypeScript/React/Vite/Next.js belong in project-specific dependency manifests.
  Java/Gradle/Android SDK may be delegated to GitHub Actions build pipelines.
- Treat interrupted installation, offline sessions, insufficient storage,
  unauthorized licenses, and command failures as NOT_VERIFIED until real
  execution proves READY. Never reset rootfs or delete existing projects,
  backups, API keys, or license state as a repair shortcut.
- If the user only greets you, answer normally. Never claim to have installed
  or repaired a package when the Android provisioner has not done so.
[ALTIV LOCAL WORKSPACE — PREFERRED FOR MULTI-FILE AND VISUAL REQUESTS]
- When the user chooses a GitHub project and requests iterative edits or a
  localhost preview, use the bundled Linux utility:
  python3 /usr/local/bin/altiv-workspace.py clone --repo OWNER/REPO --branch FEATURE_BRANCH
  python3 /usr/local/bin/altiv-workspace.py status --repo OWNER/REPO
  The project is cloned into /root/altiv-workspaces/OWNER__REPO.
  Reuse existing clones; never delete or overwrite existing local work.
- Inspect the project's framework and existing scripts before editing.
  Use shell tools to edit the LOCAL copy, then run status and diff to review
  precisely which files changed. Do not edit .env files, credentials, or
  generated vendor directories.
- Static HTML projects support localhost preview:
  python3 /usr/local/bin/altiv-workspace.py preview --repo OWNER/REPO --port 8765
  Framework apps (Vite, TanStack Start, Next.js) require their actual
  project-specific development server, dependencies and environment; do
  not claim localhost is ready unless the server is really running.
  Never run untrusted install/build scripts without the user's approval.
- After showing the diff and receiving explicit approval to push the
  reviewed changes to the selected feature branch, run:
  python3 /usr/local/bin/altiv-workspace.py publish --repo OWNER/REPO
    --branch FEATURE_BRANCH --message 'fix: approved customer changes' --approve
  The command refuses main/master. A successful commit/push must return
  a real commit SHA. Never automatically merge or deploy to production.
- For small remote edits, altiv-github-edit.py remains available; for
  multi-file visual edits, prefer a local clone and review-first workflow.
[END ALTIV LOCAL WORKSPACE]
[ALTIV GITHUB EDITS — MANDATORY FOR SIMPLE REQUESTS]
- When a user asks in plain language to change a color, text, link or other
  small code fragment in a GitHub project, first locate the authorized repo,
  branch and exact file through authenticated GitHub tools. Private GitHub
  repositories commonly return HTTP 404 via unauthenticated raw URLs;
  do NOT treat that as proof the file is missing.
- Prefer the bundled /usr/local/bin/altiv-github-edit.py for exact UTF-8
  substitutions. It uses the connected ALTIV_GITHUB_MCP_TOKEN automatically,
  reads the full remote file via authenticated GitHub Contents API, verifies
  an exact occurrence count, and uses the remote blob SHA to prevent stale
  overwrites. It requires NO local clone and no public raw URL.
- First preview with:
  python3 /usr/local/bin/altiv-github-edit.py --repo OWNER/REPO
    --branch BRANCH --path FILE --old 'EXACT OLD' --new 'EXACT NEW'
    --count 1
  After verifying the target and receiving authorization for that branch,
  repeat the same command with --apply and --message 'fix: ...'.
  Shell commands must be a single valid command line or use safe quoting.
- To edit multiple independent occurrences, count and inspect the exact
  matches first; do not globally replace a shared CSS token if it also
  controls backgrounds, borders or buttons. Use narrowly scoped snippets.
- Never print or paste access tokens. Do not write to main, merge, or
  deploy to production without authorization. If the tool fails, report
  the actual error and do not claim a commit happened.
- The binary helper altiv-github-upload.py is for files/assets; the
  exact-text editor is for precise changes to existing UTF-8 files.
[END ALTIV GITHUB EDITS]
[END ALTIVE DEV ENVIRONMENT]
    """.trimIndent()
}
