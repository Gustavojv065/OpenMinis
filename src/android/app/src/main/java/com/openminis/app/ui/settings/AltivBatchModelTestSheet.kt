package com.openminis.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openminis.app.data.model.ModelEntry
import com.openminis.app.data.model.SystemVoiceEntries
import com.openminis.app.data.repository.ProviderRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** On-demand model validation: one small text request per model, max 12 per batch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AltivBatchModelTestSheet(
    entries: List<ModelEntry>,
    providerRepository: ProviderRepository,
    onWorking: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val results = remember { mutableStateMapOf<String, AltivModelHealthProbe.Result>() }
    val providers = entries.mapNotNull { entry ->
        providerRepository.instance(entry.providerInstanceId)
            ?.takeIf { it.isEnabled }?.let { it.id to it.label }
    }.distinctBy { it.first }
    var selectedProviderId by remember { mutableStateOf<String?>(null) }
    var providerMenuOpen by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(AltivModelHealthProbe.Mode.CONNECTION) }
    var extendedTimeout by remember { mutableStateOf(false) }
    var activeModel by remember { mutableStateOf("") }
    var job by remember { mutableStateOf<Job?>(null) }
    val close: () -> Unit = { job?.cancel(); onDismiss() }

    val providerIds = providers.map { it.first }.toSet()
    val candidates = entries.filter { entry ->
        entry.id.isNotBlank() &&
            entry.providerInstanceId in providerIds &&
            (selectedProviderId == null || entry.providerInstanceId == selectedProviderId) &&
            !entry.isHidden && !entry.isUnavailableFromProvider &&
            !SystemVoiceEntries.isSystemEntryId(entry.id) &&
            AltivModelHealthProbe.supportsText(entry.model.outputModalities)
    }.distinctBy { it.id }
    val ids = candidates.map { it.id }
    val resultKey: (String) -> String = { mode.name + ":" + it }
    val testedIds = results.keys.filter { it.startsWith(mode.name + ":") }
        .map { it.substringAfter(":") }.toSet()
    val remaining = candidates.count { it.id !in testedIds }
    val batchIds = AltivBatchModelSelection.nextBatch(ids, testedIds)
    val working = candidates.count {
        results[resultKey(it.id)]?.status == AltivModelHealthProbe.Status.WORKING
    }

    ModalBottomSheet(
        onDismissRequest = close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Testador automático de modelos", style = MaterialTheme.typography.titleLarge)
            Text(
                "Os testes fazem solicitações reais e podem consumir créditos. " +
                    "Nenhum começa sozinho. Até 12 modelos por lote, um por vez. " +
                    "Há no máximo uma nova tentativa em erro transitório (5xx/timeout).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Aprovados serão pré-selecionados, mas só entram no grupo ao " +
                    "tocar em Adicionar na tela anterior.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !running,
                    onClick = { mode = AltivModelHealthProbe.Mode.CONNECTION },
                ) { Text(if (mode == AltivModelHealthProbe.Mode.CONNECTION) "✓ Conexão" else "Conexão") }
                OutlinedButton(
                    enabled = !running,
                    onClick = { mode = AltivModelHealthProbe.Mode.CODING_TOOLS },
                ) {
                    Text(
                        if (mode == AltivModelHealthProbe.Mode.CODING_TOOLS)
                            "✓ Código + ferramentas" else "Código + ferramentas"
                    )
                }
            }
            TextButton(
                enabled = !running,
                onClick = { extendedTimeout = !extendedTimeout },
            ) {
                Text(if (extendedTimeout) "Timeout: 75 s por tentativa" else "Timeout: 45 s por tentativa")
            }
            Text(
                if (mode == AltivModelHealthProbe.Mode.CODING_TOOLS)
                    "Código + ferramentas exige gerar TypeScript por uma função de teste. " +
                        "Nenhum Shell, arquivo ou MCP é executado."
                else "Conexão confirma somente que o modelo retorna texto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { providerMenuOpen = true }, enabled = !running) {
                Text("Provedor: " + (providers.firstOrNull {
                    it.first == selectedProviderId
                }?.second ?: "Todos") + " ▾")
            }
            DropdownMenu(
                expanded = providerMenuOpen,
                onDismissRequest = { providerMenuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Todos") },
                    onClick = { selectedProviderId = null; providerMenuOpen = false },
                )
                providers.forEach { (id, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { selectedProviderId = id; providerMenuOpen = false },
                    )
                }
            }
            Text(
                "Testados: " + (candidates.size - remaining) + "/" + candidates.size +
                    " • Funcionando: " + working + " • Restantes: " + remaining,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !running && batchIds.isNotEmpty(),
                    onClick = {
                        if (running) return@Button
                        val planned = batchIds.mapNotNull { id ->
                            candidates.firstOrNull { it.id == id }
                        }
                        val plannedMode = mode
                        val plannedTimeout = if (extendedTimeout)
                            AltivModelHealthProbe.EXTENDED_TIMEOUT_MS
                        else AltivModelHealthProbe.TIMEOUT_MS
                        job = scope.launch {
                            running = true
                            try {
                                for (entry in planned) {
                                    coroutineContext.ensureActive()
                                    activeModel = entry.model.displayName
                                    val result = AltivModelHealthProbe.check(
                                        entry, providerRepository, context,
                                        mode = plannedMode, timeoutMs = plannedTimeout,
                                    )
                                    results[plannedMode.name + ":" + entry.id] = result
                                    // Explicit connection test only. An unsuccessful
                                    // tool test does not imply the text API is down.
                                    if (plannedMode == AltivModelHealthProbe.Mode.CONNECTION ||
                                        result.status == AltivModelHealthProbe.Status.WORKING) {
                                        com.openminis.app.data.model.AltivModelHealthRegistry.record(
                                            entry.id,
                                            working = result.status == AltivModelHealthProbe.Status.WORKING,
                                            latencyMs = result.latencyMs,
                                        )
                                    }
                                    if (result.status == AltivModelHealthProbe.Status.WORKING) {
                                        onWorking(setOf(entry.id))
                                    }
                                }
                            } catch (cancel: CancellationException) {
                                throw cancel
                            } finally {
                                running = false
                                activeModel = ""
                            }
                        }
                    },
                ) {
                    Text(if (running) "Testando..." else "Testar próximos " + batchIds.size)
                }
                OutlinedButton(onClick = close) {
                    Text(if (running) "Cancelar testes" else "Concluir seleção")
                }
            }
            if (running) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(activeModel, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (candidates.isEmpty()) {
                Text(
                    "Nenhum modelo de texto habilitado para testar. " +
                        "Atualize a lista do provedor ou confira sua chave de API.",
                )
            }
            candidates.filter { it.id in testedIds }.takeLast(12).forEach { entry ->
                val result = results[resultKey(entry.id)] ?: return@forEach
                val status = when (result.status) {
                    AltivModelHealthProbe.Status.WORKING -> "✓ Funcionando"
                    AltivModelHealthProbe.Status.AUTH_FAILED -> "Falha de autenticação"
                    AltivModelHealthProbe.Status.RATE_LIMITED -> "Cota ou saldo insuficiente"
                    AltivModelHealthProbe.Status.NO_CREDENTIAL -> "Sem chave"
                    AltivModelHealthProbe.Status.TIMEOUT -> "Tempo esgotado"
                    AltivModelHealthProbe.Status.INCOMPATIBLE -> "Incompatível"
                    AltivModelHealthProbe.Status.DISABLED -> "Desabilitado"
                    AltivModelHealthProbe.Status.BAD_REQUEST -> "Parâmetro/formato incompatível (400/422)"
                    AltivModelHealthProbe.Status.MODEL_MISSING -> "Modelo ausente ou indisponível (404)"
                    AltivModelHealthProbe.Status.SERVICE_UNAVAILABLE -> "Falha temporária do serviço (5xx)"
                    AltivModelHealthProbe.Status.NO_CREDIT -> "Sem créditos (402)"
                    AltivModelHealthProbe.Status.TOOL_UNSUPPORTED ->
                        "Não retornou chamada de ferramenta válida"
                    AltivModelHealthProbe.Status.INVALID_CODE ->
                        "Chamou a ferramenta, mas o código não passou"
                    else -> "Falhou"
                }
                Column {
                    Text(entry.model.displayName, fontWeight = FontWeight.SemiBold)
                    Text(
                        status + " • " + result.latencyMs + " ms" +
                            (if (result.attempts > 1) " • 2 tentativas" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (remaining == 0 && candidates.isNotEmpty() && !running) {
                Text("Todos os modelos deste filtro foram verificados nesta sessão.")
            }
            Text(
                "O teste de conexão não garante programação. O teste de ferramentas " +
                    "verifica apenas chamada estruturada com código simples, nunca um projeto real. " +
                    "Confirme o preço dos modelos; não há garantia de disponibilidade futura.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
