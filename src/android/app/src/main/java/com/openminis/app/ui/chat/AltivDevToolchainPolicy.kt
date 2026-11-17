package com.openminis.app.ui.chat

/**
 * Commercial ALTIVE developer-environment readiness policy.
 *
 * This is intentionally a read-only check on the first DEVELOPMENT request,
 * not an install-on-login. The guest Alpine PRoot environment can be offline;
 * package installation needs network, storage and informed user consent.
 */
internal object AltivDevToolchainPolicy {
    val instructions: String = """
[ALTIVE DEV ENVIRONMENT — first programming task]
- Before the first coding/build task of this session, if shell_execute is
  available and the licensed workspace is ready, call it ONCE with exactly:
      sh /usr/local/bin/altive-dev-check
  This command is a bundled offline, READ-ONLY tool inventory. Do not run it
  again on every message if there is already a verified report in this session.
- Show the user a concise status: READY, MISSING, NOT_VERIFIED and why the
  missing tools matter for the CURRENT project. Do not infer availability
  from general model knowledge, an old report or GitHub MCP connectivity.
- The Alpine PRoot environment is separate from the Android host and from
  ChatGPT plugins. Git CLI, GitHub MCP and GitHub API are different facilities.
  Do not require Git CLI if the user's task is satisfied by an authorized MCP.
- For React/Vite/TypeScript: require node and npm; npm project dependencies
  belong to the project, not global installs. For Python: require python3;
  install py3-pip only when pip packages are needed, and prefer a venv.
  Git, bash, curl, unzip, jq and native build tools are task-dependent.
- If a required command is MISSING, tell the user what needs installation,
  the proposed exact Alpine package(s), network/storage implications and
  ask explicit permission BEFORE calling apk add. Never silently execute
  apk update/upgrade, curl|sh, pip install to system Python or global npm.
- If a check fails because PRoot, shell, license, network or permissions are
  unavailable, report NOT_VERIFIED rather than saying a command is missing.
  Offer a non-destructive repair plan; never reset rootfs, projects or backups.
- After an authorized package installation, check the command again and show
  its actual version. Do not claim success merely because apk returned zero.
- If the user only greets you or asks a question, do not install anything.
  The greeting may mention "Verificar ambiente de desenvolvimento", but the
  check is invoked when actual development starts and tools are available.
[END ALTIVE DEV ENVIRONMENT]
    """.trimIndent()
}
