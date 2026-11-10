package com.openminis.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openminis.app.offline.AltivOfflineConversationPrompt
import com.openminis.app.offline.AltivOfflineConversationStore
import com.openminis.app.offline.AltivOfflineInference
import com.openminis.app.offline.AltivOfflineModelCatalog
import com.openminis.app.offline.AltivOfflineModelDownloads
import kotlinx.coroutines.launch

/**
 * A separate, private on-device conversation. It is NOT the network-backed
 * agent chat: no provider credentials, Git, Shell, MCP or auto-cloud fallback.
 * Loaded models are released by AltivOfflineInference after every reply.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AltivOfflineChatScreen(initialModelId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val downloads = remember { AltivOfflineModelDownloads(context) }
    val store = remember { AltivOfflineConversationStore(context) }
    val inference = remember { AltivOfflineInference(context) }
    val scope = rememberCoroutineScope()
    val installed = AltivOfflineModelCatalog.models.filter { model ->
        downloads.state(model).phase == AltivOfflineModelDownloads.Phase.READY &&
            AltivOfflineInference.totalRamGb(context) >= model.minimumDeviceRamGb
    }
    var selectedModelId by remember(initialModelId) {
        mutableStateOf(initialModelId)
    }
    val model = installed.firstOrNull { it.id == selectedModelId }
        ?: installed.firstOrNull()
    var menuOpen by remember { mutableStateOf(false) }
    var clearConfirmation by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var processing by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var performance by remember { mutableStateOf<String?>(null) }
    val turns = remember { mutableStateListOf<AltivOfflineConversationPrompt.Turn>() }
    val listState = rememberLazyListState()

    LaunchedEffect(model?.id) {
        turns.clear()
        model?.id?.let { turns.addAll(store.read(it)) }
        input = ""
        errorText = null
        performance = null
    }
    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) listState.animateScrollToItem(turns.lastIndex + 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversa offline") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(enabled = !processing && model != null,
                        onClick = { clearConfirmation = true }) { Text("Limpar") }
                },
            )
        },
        bottomBar = {
            if (model != null) {
                Column(Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = input,
                        onValueChange = { input = it.take(750) },
                        label = { Text("Escreva sua mensagem offline") },
                        minLines = 2,
                        maxLines = 4,
                        enabled = !processing,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !processing && input.isNotBlank(),
                        onClick = {
                            val current = model
                            val message = input.trim()
                            val previous = turns.toList()
                            val prompt = AltivOfflineConversationPrompt.build(previous, message)
                            val updated = AltivOfflineConversationPrompt.append(
                                previous, AltivOfflineConversationPrompt.Turn(
                                    AltivOfflineConversationPrompt.Role.USER, message
                                )
                            )
                            turns.clear()
                            turns.addAll(updated)
                            store.save(current.id, updated)
                            input = ""
                            processing = true
                            errorText = null
                            performance = null
                            scope.launch {
                                try {
                                    val response = inference.complete(current, prompt)
                                    val answer = response.text.ifBlank {
                                        "O modelo local não retornou texto nesta tentativa."
                                    }
                                    val finalTurns = AltivOfflineConversationPrompt.append(
                                        turns.toList(),
                                        AltivOfflineConversationPrompt.Turn(
                                            AltivOfflineConversationPrompt.Role.ASSISTANT, answer
                                        )
                                    )
                                    turns.clear()
                                    turns.addAll(finalTurns)
                                    store.save(current.id, finalTurns)
                                    performance = "Gerado localmente em " +
                                        response.elapsedMs / 1_000L + " s • " +
                                        "%.1f".format(response.tokensPerSecond) + " tokens/s"
                                } catch (e: Exception) {
                                    errorText = "Falha na execução offline: " +
                                        (e.message ?: "erro desconhecido") +
                                        ". Seu texto foi salvo; você pode tentar novamente."
                                } finally {
                                    processing = false
                                }
                            }
                        },
                    ) {
                        Text(if (processing) "Gerando no celular..." else "Enviar offline")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Sem API e sem internet · Mensagens salvas neste aparelho",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            if (model == null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Não há modelos GGUF baixados e compatíveis com a RAM deste celular. " +
                            "Volte a Modelos offline, baixe ou importe um arquivo e tente novamente.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = !processing,
                        onClick = { menuOpen = true },
                    ) { Text(model.name + " ▾") }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        installed.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.name) },
                                onClick = {
                                    selectedModelId = entry.id
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
                Text(
                    "O modelo usa a CPU e pode demorar. Não aplica alterações em " +
                        "GitHub, arquivos ou Shell. Para isso, use o agente online.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        if (turns.isEmpty()) {
                            Text(
                                "Faça uma pergunta ou peça um exemplo de React, CSS, " +
                                    "TypeScript ou melhorias visuais.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    itemsIndexed(turns, key = { index, _ -> index }) { _, turn ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    if (turn.role == AltivOfflineConversationPrompt.Role.USER)
                                        "Você" else model.name,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(turn.text)
                            }
                        }
                    }
                    item {
                        if (processing) Text("Aguarde a geração local...")
                        errorText?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        performance?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (clearConfirmation && model != null) {
        AlertDialog(
            onDismissRequest = { clearConfirmation = false },
            title = { Text("Apagar esta conversa offline?") },
            text = { Text("O histórico deste modelo será excluído apenas do aparelho.") },
            confirmButton = {
                TextButton(onClick = {
                    store.clear(model.id)
                    turns.clear()
                    clearConfirmation = false
                }) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { clearConfirmation = false }) { Text("Cancelar") }
            },
        )
    }
}
