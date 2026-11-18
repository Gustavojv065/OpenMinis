package com.openminis.app.commercial

import android.content.Context
import android.os.Build
import com.openminis.app.sandbox.PRootKernel
import com.openminis.app.sandbox.RootfsManager
import com.openminis.app.sandbox.ShellExecutor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Prepares the real ARM64 Linux programming environment after a server-validated
 * ALTIVE license or subscription. Bundled tools work offline on fresh installs;
 * legacy rootfs installations receive signed packages inside Alpine PRoot.
 */
internal object AltivToolchainProvisioner {
    private val mutex = Mutex()

    private val VERIFY = """
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
tmp=$(mktemp -d)
trap 'rm -rf "\${'$'}tmp"' EXIT
git init -q "\${'$'}tmp/project"
git -C "\${'$'}tmp/project" status --short >/dev/null
echo ALTIV_TOOLCHAIN_READY
""".trimIndent()

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
            val appContext = context.applicationContext
            val rootfs = RootfsManager.getInstance(appContext)
            onStage("Preparando o ambiente de programação...")
            PRootKernel.boot(appContext)
            check(rootfs.isInstalled) { "Ambiente Linux não foi instalado corretamente." }

            onStage("Verificando Node.js, NPM, Python e Git...")
            val first = ShellExecutor.execute(appContext, VERIFY)
            if (first.exitCode == 0 && first.output.contains("ALTIV_TOOLCHAIN_READY")) {
                onStage("Ambiente pronto para criar projetos.")
                return@runCatching
            }

            onStage("Instalando ferramentas de programação automaticamente...")
            val install = ShellExecutor.execute(appContext, INSTALL, timeout = 600_000L)
            check(install.exitCode == 0) {
                "Não foi possível concluir a instalação (código \${install.exitCode}). " +
                    "Verifique a conexão e o espaço livre e tente novamente."
            }
            onStage("Testando as ferramentas instaladas...")
            val second = ShellExecutor.execute(appContext, VERIFY)
            check(second.exitCode == 0 && second.output.contains("ALTIV_TOOLCHAIN_READY")) {
                "As ferramentas não passaram no teste real (código \${second.exitCode}). " +
                    "Tente novamente."
            }
            onStage("Ambiente pronto para criar projetos.")
        }
    }
}
