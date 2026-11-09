package com.openminis.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openminis.app.offline.AltivOfflineCategory
import com.openminis.app.offline.AltivOfflineModel
import com.openminis.app.offline.AltivOfflineModelCatalog
import com.openminis.app.offline.AltivOfflineModelDownloads
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Catalog and real downloads; offline inference is a separate future feature. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AltivOfflineModelsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val downloads = remember { AltivOfflineModelDownloads(context) }
    var categoryIndex by remember { mutableIntStateOf(0) }
    var wifiOnly by remember { mutableStateOf(true) }
    var refresh by remember { mutableIntStateOf(0) }
    var confirmDownload by remember { mutableStateOf<AltivOfflineModel?>(null) }
    var confirmDelete by remember { mutableStateOf<AltivOfflineModel?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(3_000)
            refresh++
        }
    }
    val categories = AltivOfflineCategory.entries
    val category = categories[categoryIndex]
    val models = AltivOfflineModelCatalog.inCategory(category)
    // Reading state here makes the polling tick refresh all visible progress bars.
    @Suppress("UNUSED_VARIABLE")
    val tick = refresh
    val freeMb = downloads.freeSpaceBytes() / AltivOfflineModel.ONE_MB

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modelos offline") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = categoryIndex, edgePadding = 8.dp) {
                categories.forEachIndexed { index, entry ->
                    Tab(
                        selected = index == categoryIndex,
                        onClick = { categoryIndex = index },
                        text = {
                            Text(entry.label + " (" +
                                AltivOfflineModelCatalog.inCategory(entry).size + ")")
                        },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                item {
                    Text(
                        "ALTIV DEV AI — Catálogo local",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Baixe modelos pequenos para o armazenamento do celular. " +
                            "Esta primeira fase gerencia arquivos GGUF; o motor para " +
                            "conversar offline será integrado e testado separadamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Espaço livre estimado: " + freeMb + " MB",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Baixar somente por Wi-Fi",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(checked = wifiOnly, onCheckedChange = { wifiOnly = it })
                    }
                    Text(
                        if (wifiOnly) "Dados móveis não serão usados para downloads novos."
                        else "Atenção: downloads podem consumir centenas de MB do seu plano móvel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (models.isEmpty()) {
                    item {
                        Card {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Categoria em planejamento",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "Visão, voz, imagens, embeddings e música exigem " +
                                        "mecanismos de execução específicos. Nenhum " +
                                        "download é oferecido antes de validar a compatibilidade.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                items(models, key = { it.id }) { model ->
                    val state = downloads.state(model)
                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(model.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                model.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                model.quantization + " • aprox. " +
                                    model.sizeBytes / AltivOfflineModel.ONE_MB + " MB",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(model.ramGuidance, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Licença: " + model.license,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val label = when (state.phase) {
                                AltivOfflineModelDownloads.Phase.NOT_DOWNLOADED -> "Não baixado"
                                AltivOfflineModelDownloads.Phase.QUEUED -> "Na fila"
                                AltivOfflineModelDownloads.Phase.DOWNLOADING -> "Baixando..."
                                AltivOfflineModelDownloads.Phase.PAUSED -> "Pausado"
                                AltivOfflineModelDownloads.Phase.READY -> "GGUF baixado e reconhecido"
                                AltivOfflineModelDownloads.Phase.FAILED -> "Falha no download"
                            }
                            Text(label, fontWeight = FontWeight.SemiBold)
                            if (state.totalBytes > 0L &&
                                (state.phase == AltivOfflineModelDownloads.Phase.DOWNLOADING ||
                                 state.phase == AltivOfflineModelDownloads.Phase.QUEUED ||
                                 state.phase == AltivOfflineModelDownloads.Phase.PAUSED)
                            ) {
                                LinearProgressIndicator(
                                    progress = {
                                        (state.downloadedBytes.toFloat() /
                                            state.totalBytes.toFloat()).coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    (state.downloadedBytes / AltivOfflineModel.ONE_MB).toString() +
                                        " / " + (state.totalBytes / AltivOfflineModel.ONE_MB) + " MB",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            state.message?.let {
                                Text(it, color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (state.phase == AltivOfflineModelDownloads.Phase.NOT_DOWNLOADED) {
                                    Button(onClick = { confirmDownload = model }) {
                                        Text("Baixar")
                                    }
                                } else {
                                    OutlinedButton(onClick = { confirmDelete = model }) {
                                        Text(
                                            if (state.phase == AltivOfflineModelDownloads.Phase.READY)
                                                "Excluir arquivo" else "Cancelar / excluir"
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        context.startActivity(Intent(
                                            Intent.ACTION_VIEW, Uri.parse(model.sourceUrl)
                                        ).addCategory(Intent.CATEGORY_BROWSABLE))
                                    },
                                ) { Text("Fonte e licença") }
                            }
                            if (state.phase == AltivOfflineModelDownloads.Phase.READY) {
                                Text(
                                    "Arquivo no armazenamento do app. Ainda não ativado " +
                                        "como modelo de conversa no chat.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { refresh++ },
                    ) { Text("Atualizar downloads") }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "A verificação atual confere apenas tamanho mínimo e assinatura " +
                            "GGUF, não um hash criptográfico. Não execute arquivos de " +
                            "origem desconhecida. Downloads do armazenamento do " +
                            "aplicativo são apagados ao desinstalá-lo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    confirmDownload?.let { model ->
        AlertDialog(
            onDismissRequest = { confirmDownload = null },
            title = { Text("Baixar " + model.name + "?") },
            text = {
                Text(
                    "Arquivo aproximado de " +
                        model.sizeBytes / AltivOfflineModel.ONE_MB + " MB. " +
                        (if (wifiOnly) "Somente por Wi-Fi. " else "Pode usar dados móveis. ") +
                        "Não habilita conversa offline até instalarmos o motor de inferência."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    downloads.start(model, wifiOnly)
                        .onFailure { errorText = it.message ?: "Falha ao iniciar download." }
                    confirmDownload = null
                    refresh++
                }) { Text("Confirmar download") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDownload = null }) { Text("Cancelar") }
            },
        )
    }
    confirmDelete?.let { model ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Excluir " + model.name + "?") },
            text = {
                Text("O download será cancelado e o arquivo do aplicativo será excluído.")
            },
            confirmButton = {
                TextButton(onClick = {
                    downloads.remove(model)
                    confirmDelete = null
                    refresh++
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Voltar") }
            },
        )
    }
    errorText?.let { message ->
        AlertDialog(
            onDismissRequest = { errorText = null },
            title = { Text("Não foi possível iniciar o download") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorText = null }) { Text("OK") }
            },
        )
    }
}
