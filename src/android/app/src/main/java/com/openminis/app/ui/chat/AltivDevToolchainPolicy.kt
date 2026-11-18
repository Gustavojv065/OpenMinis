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
[END ALTIVE DEV ENVIRONMENT]
    """.trimIndent()
}
