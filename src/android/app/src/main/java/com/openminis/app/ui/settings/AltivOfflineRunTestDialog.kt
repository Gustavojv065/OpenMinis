package com.openminis.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openminis.app.offline.AltivOfflineInference
import com.openminis.app.offline.AltivOfflineModel
import kotlinx.coroutines.launch

/** Manual offline inference smoke test. No shell, no GitHub and no network API. */
@Composable
internal fun AltivOfflineRunTestDialog(
    model: AltivOfflineModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val inference = remember { AltivOfflineInference(context) }
    val scope = rememberCoroutineScope()
    var prompt by remember(model.id) {
        mutableStateOf(
            "Crie uma função TypeScript chamada somar(a: number, b: number) " +
                "que retorne a soma, com um exemplo de uso."
        )
    }
    var running by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf<String?>(null) }
    var metrics by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!running) onDismiss() },
        title = { Text("Teste offline · " + model.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Execução local pela CPU, sem chave de API. " +
                        "Pode ser lenta em celulares mais simples. " +
                        "Não modifica nenhum arquivo ou repositório.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it.take(800) },
                    label = { Text("Pedido de programação") },
                    enabled = !running,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
                if (running) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text("Modelo carregado na RAM; gerando resposta local...")
                    }
                }
                output?.let { response ->
                    Column(
                        modifier = Modifier.heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            response,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(metrics, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !running && prompt.isNotBlank(),
                onClick = {
                    running = true
                    output = null
                    metrics = ""
                    scope.launch {
                        try {
                            val result = inference.complete(model, prompt)
                            output = result.text.ifBlank { "(Modelo não gerou texto.)" }
                            metrics = "Tempo: " + result.elapsedMs + " ms · " +
                                "%.2f".format(result.tokensPerSecond) + " tokens/s"
                        } catch (e: Exception) {
                            output = "Não foi possível executar offline: " +
                                (e.message ?: "erro desconhecido")
                        } finally {
                            running = false
                        }
                    }
                },
            ) { Text("Gerar offline") }
        },
        dismissButton = {
            TextButton(enabled = !running, onClick = onDismiss) { Text("Fechar") }
        },
    )
}
