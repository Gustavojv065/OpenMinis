package com.openminis.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.openminis.app.data.repository.EnvVarRepository
import com.openminis.app.data.repository.MCPRepository
import com.openminis.app.mcp.AltivGitHubPatConfig
import com.openminis.app.mcp.oauth.MCPOAuthController
import com.openminis.app.ui.components.DialogTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** No PAT is logged, persisted in servers.json or returned to the model. */
private object AltivGitHubPatVerifier {
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()

    suspend fun check(token: String): String? = withContext(Dispatchers.IO) {
        try {
            http.newCall(
                Request.Builder().url("https://api.github.com/user")
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .get().build()
            ).execute().use { response ->
                when (response.code) {
                    200 -> null
                    401 -> "Token inválido ou revogado. Revise no GitHub."
                    403 -> "O GitHub recusou a credencial. Confira permissões e políticas da conta."
                    429 -> "Muitas tentativas. Tente novamente mais tarde."
                    else -> "Falha ao verificar o token (HTTP " + response.code + ")."
                }
            }
        } catch (_: Exception) {
            "Não foi possível contatar o GitHub. Confira sua conexão."
        }
    }
}

/** Dedicated ALTIV GitHub form: one PAT field, no Client ID/Secret/OAuth. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AltivGitHubPatSheet(
    mcpRepository: MCPRepository,
    envVarRepository: EnvVarRepository?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val envEntries by (envVarRepository?.entries?.collectAsState()
        ?: remember { mutableStateOf(emptyList<EnvVarRepository.EnvVarEntry>()) })
    val servers by mcpRepository.servers.collectAsState()
    val existing = servers.firstOrNull { it.id == AltivGitHubPatConfig.ID }
    val savedEntry = envEntries.firstOrNull { it.key == AltivGitHubPatConfig.ENV_NAME }
    val hasToken = savedEntry != null &&
        !envVarRepository?.getValue(AltivGitHubPatConfig.ENV_NAME).isNullOrBlank()
    var token by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmRemoval by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .imePadding().navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("GitHub — acesso com token", style = MaterialTheme.typography.titleLarge)
            Text("O ALTIV DEV AI terá somente as permissões e os repositórios que você selecionou ao criar o token.")
            if (hasToken) Text(
                "Token salvo e protegido no dispositivo. Cole um novo para substituí-lo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("Token de acesso GitHub", style = MaterialTheme.typography.labelLarge)
            DialogTextField(
                value = token,
                onValueChange = { token = it.take(700); error = null },
                placeholder = if (hasToken) "Deixe vazio para usar o token salvo" else "Cole seu token aqui",
                visualTransformation = PasswordVisualTransformation(),
                enabled = !busy,
                singleLine = true,
            )
            Text(
                "Não precisa de Client ID, Client Secret ou OAuth. O GitHub controla as permissões do token.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/settings/personal-access-tokens/new")))
                }, enabled = !busy,
            ) { Text("Criar token no GitHub") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (busy) CircularProgressIndicator()
            Button(
                enabled = !busy && (token.isNotBlank() || hasToken),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (busy) return@Button
                    val vault = envVarRepository
                    if (vault == null) {
                        error = "O armazenamento criptografado não está disponível. Reabra o aplicativo."
                        return@Button
                    }
                    if (!AltivGitHubPatConfig.canReplace(existing)) {
                        error = "Já existe outro servidor chamado github. Revise-o antes de substituir."
                        return@Button
                    }
                    val entered = token.trim()
                    val chosen = entered.ifBlank {
                        vault.getValue(AltivGitHubPatConfig.ENV_NAME).orEmpty()
                    }
                    if (chosen.isBlank()) {
                        error = "Cole o token criado no GitHub."
                        return@Button
                    }
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            val problem = AltivGitHubPatVerifier.check(chosen)
                            if (problem != null) {
                                error = problem
                                return@launch
                            }
                            if (entered.isNotBlank()) {
                                val entry = vault.entries.value.firstOrNull {
                                    it.key == AltivGitHubPatConfig.ENV_NAME
                                }
                                val ok = if (entry == null) {
                                    vault.add(AltivGitHubPatConfig.ENV_NAME, chosen,
                                        "Token GitHub MCP")
                                } else {
                                    vault.update(entry.id, AltivGitHubPatConfig.ENV_NAME,
                                        chosen, "Token GitHub MCP")
                                }
                                if (!ok) {
                                    error = "Falha ao guardar o token com criptografia."
                                    return@launch
                                }
                            }
                            vault.entries.value.firstOrNull {
                                it.key == AltivGitHubPatConfig.ENV_NAME
                            }?.takeIf { !it.enabled }?.let { vault.setEnabled(it.id, true) }
                            if (!mcpRepository.add(AltivGitHubPatConfig.make(existing))) {
                                error = "Não foi possível registrar a conexão MCP."
                                return@launch
                            }
                            // Explicit switch from old OAuth to token-based auth.
                            MCPOAuthController(context).purge(AltivGitHubPatConfig.ID)
                            token = ""
                            onDismiss()
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text(if (busy) "Verificando..." else "Testar token e conectar") }
            if (existing != null && AltivGitHubPatConfig.canReplace(existing)) {
                OutlinedButton(
                    onClick = { confirmRemoval = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Desconectar GitHub e apagar token") }
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (confirmRemoval) {
        AlertDialog(
            onDismissRequest = { confirmRemoval = false },
            title = { Text("Desconectar GitHub?") },
            text = { Text(
                "A conexão e a credencial neste aparelho serão apagadas. " +
                "O token também deve ser revogado no GitHub, se não for mais utilizado.",
            ) },
            confirmButton = {
                TextButton(onClick = {
                    mcpRepository.delete(AltivGitHubPatConfig.ID)
                    envVarRepository?.let { vault ->
                        vault.entries.value.firstOrNull {
                            it.key == AltivGitHubPatConfig.ENV_NAME
                        }?.let { entry -> vault.delete(entry.id) }
                    }
                    MCPOAuthController(context).purge(AltivGitHubPatConfig.ID)
                    confirmRemoval = false
                    onDismiss()
                }) { Text("Remover") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoval = false }) { Text("Cancelar") }
            },
        )
    }
}
