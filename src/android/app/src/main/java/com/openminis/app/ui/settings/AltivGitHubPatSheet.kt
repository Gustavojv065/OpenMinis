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
import com.openminis.app.mcp.AltivGitHubRepositoryAccess
import com.openminis.app.mcp.AltivGitHubRepositoryScan
import com.openminis.app.mcp.AltivGitHubProjectInspector
import com.openminis.app.mcp.AltivGitHubProjectSignals
import com.openminis.app.mcp.AltivGitHubAccessException
import kotlinx.coroutines.launch

/** Dedicated ALTIV GitHub form: one PAT field, no Client ID/Secret/OAuth. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AltivGitHubPatSheet(
    mcpRepository: MCPRepository,
    envVarRepository: EnvVarRepository?,
    onDismiss: () -> Unit,
    onRepositoriesChecked: (Int?, Boolean) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val repoInspector = remember { AltivGitHubRepositoryAccess() }
    val projectInspector = remember { AltivGitHubProjectInspector() }
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
    var scan by remember { mutableStateOf<AltivGitHubRepositoryScan?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var projectSignals by remember { mutableStateOf<AltivGitHubProjectSignals?>(null) }
    var signalsBusy by remember { mutableStateOf(false) }
    var signalsError by remember { mutableStateOf<String?>(null) }

    // Refresh the previously saved token's access when the user reopens this sheet.
    LaunchedEffect(hasToken, existing?.enabled) {
        if (!hasToken || existing?.enabled != true || scan != null || busy) return@LaunchedEffect
        val saved = envVarRepository?.getValue(AltivGitHubPatConfig.ENV_NAME).orEmpty()
        if (saved.isBlank()) return@LaunchedEffect
        busy = true
        try {
            val verified = repoInspector.inspect(saved)
            scan = verified
            status = "Token validado e GitHub MCP configurado."
            onRepositoriesChecked(verified.repositories.size, verified.complete)
        } catch (failure: AltivGitHubAccessException) {
            scan = null
            status = null
            error = failure.message
            onRepositoriesChecked(null, false)
        } finally {
            busy = false
        }
    }

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
                onValueChange = {
                    token = it.take(700)
                    error = null
                    scan = null
                    status = null
                    onRepositoriesChecked(null, false)
                    projectSignals = null
                    signalsError = null
                },
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
            Text(
                "Esta lista mostra repositórios remotos. Ela não clona arquivos na pasta local. " +
                    "Para editar no terminal, autentique o Git separadamente (por exemplo, GitHub CLI). " +
                    "Abra uma nova conversa para carregar o MCP no agente.",
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
            if (busy) {
                Text("Verificando conta e repositórios no GitHub...")
                CircularProgressIndicator()
            }
            status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            scan?.let { result ->
                Text("Conta: @" + result.account, style = MaterialTheme.typography.titleSmall)
                Text(
                    (if (result.complete) result.repositories.size.toString()
                     else "Pelo menos " + result.repositories.size) +
                        " repositório(s) acessível(is) por este token",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (result.repositories.isEmpty()) {
                    Text(
                        "Token válido, mas nenhum repositório retornado. Revise os repositórios " +
                            "selecionados e a permissão Metadata: Read no GitHub.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        "Toque em um repositório para detectar arquivos de Lovable, Vercel ou Supabase. " +
                            "É preciso conceder Contents: Read apenas aos projetos desejados.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    result.repositories.take(100).forEach { repository ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy && !signalsBusy,
                            onClick = {
                                val selectedToken = token.trim().ifBlank {
                                    envVarRepository?.getValue(AltivGitHubPatConfig.ENV_NAME).orEmpty()
                                }
                                if (selectedToken.isBlank()) {
                                    signalsError = "Salve e valide seu token antes de verificar projetos."
                                } else {
                                    signalsBusy = true
                                    signalsError = null
                                    projectSignals = null
                                    scope.launch {
                                        try {
                                            projectSignals = projectInspector.inspect(
                                                selectedToken, repository.fullName
                                            )
                                        } catch (e: AltivGitHubAccessException) {
                                            signalsError = e.message
                                        } finally {
                                            signalsBusy = false
                                        }
                                    }
                                }
                            },
                        ) {
                            Text(
                                "Verificar " + repository.fullName +
                                    (if (repository.isPrivate) " (privado)" else ""),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (signalsBusy) {
                        Text("Verificando arquivos do repositório (somente leitura)...")
                        CircularProgressIndicator()
                    }
                    signalsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    projectSignals?.let { findings ->
                        Text(
                            "Serviços identificados em " + findings.repository,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (findings.labels().isEmpty()) {
                            Text("Nenhum arquivo conhecido localizado neste repositório.")
                        } else {
                            findings.labels().forEach { label -> Text("• " + label) }
                        }
                        Text(
                            "São indícios de configuração no código, não confirmação de " +
                                "conta conectada, deploy ativo ou permissão de edição. " +
                                "Para confirmar produção, autorize cada provedor separadamente.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (result.repositories.size > 100) {
                        Text(
                            "Exibindo os primeiros 100; os demais foram contabilizados.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (!result.complete) Text(
                    "Lista parcial: limite de 1.000 repositórios por consulta.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "A lista confirma acesso pela API GitHub, não que já exista um clone " +
                        "no workspace ou que a sessão MCP tenha sido iniciada.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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
                            val verified = repoInspector.inspect(chosen)
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
                            scan = verified
                            status = "Token validado e servidor GitHub MCP configurado."
                            onRepositoriesChecked(verified.repositories.size, verified.complete)
                            // Do not dismiss: let the user review the verified repository list.
                        } catch (failure: AltivGitHubAccessException) {
                            error = failure.message
                            scan = null
                            status = null
                            onRepositoriesChecked(null, false)
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text(if (busy) "Verificando..." else "Testar token, listar repositórios e conectar") }
            OutlinedButton(
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Fechar") }
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
                    scan = null
                    status = null
                    onRepositoriesChecked(null, false)
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
