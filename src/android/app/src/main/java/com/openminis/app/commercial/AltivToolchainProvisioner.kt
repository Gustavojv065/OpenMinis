package com.openminis.app.commercial

import android.content.Context
import android.os.Build
import com.openminis.app.sandbox.PRootKernel
import com.openminis.app.sandbox.RootfsManager
import com.openminis.app.sandbox.ShellExecutor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * First-use, license-gated Linux toolchain preparation.
 *
 * Fresh installs have the signed Alpine ARM64 packages bundled in the APK.
 * On an upgraded app with an older, incomplete rootfs, Alpine's signed
 * packages are installed inside PRoot without requiring terminal commands.
 * Existing workspaces, license state and developer credentials are untouched.
 *
 * The UI calls this ONLY after a server-validated commercial entitlement.
 */
internal object AltivToolchainProvisioner {
    private val mutex = Mutex()

    private const val VERIFY = """
set -eu
test -f /etc/alpine-release
command -v node >/dev/null
command -v npm >/dev/null
command -v npx >/dev/null
command -v python3 >/dev/null
command -v git >/dev/null
command -v bash >/dev/null
node --version >/dev/null
npm --version >/dev/null
npx --version >/dev/null
python3 --version >/dev/null
python3 -m pip --version >/dev/null
git --version >/dev/null
bash -c 'echo BASH_OK' | grep -qx BASH_OK
node -e 'process.stdout.write("NODE_OK")' | grep -qx NODE_OK
python3 -c 'print("PYTHON_OK")' | grep -qx PYTHON_OK
dir=$(mktemp -d)
trap 'rm -rf "$dir"' EXIT
git init -q "$dir/project"
git -C "$dir/project" status --short >/dev/null
printf 'ALTIV_TOOLCHAIN_READY\\n'
"""

    private const val INSTALL = """
set -eu
test -f /etc/alpine-release
command -v apk >/dev/null
apk add --no-cache nodejs npm python3 py3-pip git bash curl wget unzip zip ca-certificates
"""

    suspend fun prepare(
        context: Context,
        onStage: (String) -> Unit,
    ): Result<Unit> = mutex.withLock {
        runCatching {
            require(Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }) {
                "Este APK requer Android ARM64."
            }
            val rootfs = RootfsManager.getInstance(context.applicationContext)
            onStage("Preparando o ambiente de programação...")
            // Performs a one-time bundled extraction. Never reset an existing
            // user rootfs, even if a package or version probe fails.
            PRootKernel.boot(context.applicationContext)
            check(rootfs.isInstalled) { "Ambiente Linux não foi instalado corretamente." }

            onStage("Verificando Node.js, NPM, Python e Git...")
            val check = ShellExecutor.execute(context.applicationContext, VERIFY)
            if (check.exitCode == 0 && check.output.contains("ALTIV_TOOLCHAIN_READY")) {
                onStage("Ambiente pronto para criar projetos.")
                return@runCatching
            }

            // Legacy installations retain an older Alpine rootfs without the
            // new prebundled tools. Never run apk on Android's host shell.
            onStage("Instalando ferramentas de programação automaticamente...")
            val install = ShellExecutor.execute(
                context.applicationContext, INSTALL, timeout = 600_000L,
            )
            check(install.exitCode == 0) {
                "Não foi possível concluir a instalação (código ${install.exitCode}). " +
                    "Verifique a conexão com a internet e o espaço livre e tente novamente."
            }
            onStage("Testando as ferramentas instaladas...")
            val second = ShellExecutor.execute(context.applicationContext, VERIFY)
            check(second.exitCode == 0 && second.output.contains("ALTIV_TOOLCHAIN_READY")) {
                "As ferramentas ainda não passaram na verificação real " +
                    "(código ${second.exitCode}). Tente novamente."
            }
            onStage("Ambiente pronto para criar projetos.")
        }
    }
}
