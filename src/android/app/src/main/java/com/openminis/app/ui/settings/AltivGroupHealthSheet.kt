package com.openminis.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openminis.app.R
import com.openminis.app.data.model.ModelEntry
import com.openminis.app.data.repository.ProviderRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Manual health-check view for models explicitly selected in a group.
 * No automatic requests on opening, no scheduled work and no secret logging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AltivGroupHealthSheet(
    entries: List<ModelEntry>,
    providerRepository: ProviderRepository,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selectedEntries = entries.take(AltivModelHealthProbe.MAX_MODELS_PER_RUN)
    val results = remember(selectedEntries.map { it.id }) {
        mutableStateListOf<AltivModelHealthProbe.Result>().apply {
            addAll(selectedEntries.map {
                AltivModelHealthProbe.Result(AltivModelHealthProbe.Status.WAITING)
            })
        }
    }
    var running by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val close: () -> Unit = { job?.cancel(); onDismiss() }
    val statusLabels = mapOf(
        AltivModelHealthProbe.Status.WAITING to R.string.altiv_health_waiting,
        AltivModelHealthProbe.Status.RUNNING to R.string.altiv_health_running,
        AltivModelHealthProbe.Status.WORKING to R.string.altiv_health_working,
        AltivModelHealthProbe.Status.NO_CREDENTIAL to R.string.altiv_health_no_key,
        AltivModelHealthProbe.Status.DISABLED to R.string.altiv_health_disabled,
        AltivModelHealthProbe.Status.INCOMPATIBLE to R.string.altiv_health_incompatible,
        AltivModelHealthProbe.Status.AUTH_FAILED to R.string.altiv_health_auth,
        AltivModelHealthProbe.Status.RATE_LIMITED to R.string.altiv_health_limit,
        AltivModelHealthProbe.Status.TIMEOUT to R.string.altiv_health_timeout,
        AltivModelHealthProbe.Status.FAILED to R.string.altiv_health_failed,
    )

    ModalBottomSheet(
        onDismissRequest = close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.altiv_health_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.altiv_health_warning),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            if (entries.size > selectedEntries.size) {
                Text(
                    stringResource(R.string.altiv_health_cap, selectedEntries.size, entries.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (!running) {
                            job = scope.launch {
                                running = true
                                try {
                                    results.indices.forEach { i ->
                                        coroutineContext.ensureActive()
                                        results[i] = AltivModelHealthProbe.Result(AltivModelHealthProbe.Status.RUNNING)
                                        results[i] = AltivModelHealthProbe.check(selectedEntries[i], providerRepository, context)
                                    }
                                } catch (cancel: CancellationException) {
                                    throw cancel
                                } finally {
                                    running = false
                                }
                            }
                        }
                    },
                    enabled = !running && selectedEntries.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.altiv_health_start))
                }
                OutlinedButton(onClick = close) {
                    Text(stringResource(R.string.quicktest_done))
                }
            }
            val workingCount = results.count { it.status == AltivModelHealthProbe.Status.WORKING }
            Text(
                stringResource(R.string.altiv_health_summary, workingCount, selectedEntries.size),
                style = MaterialTheme.typography.titleSmall,
            )
            selectedEntries.forEachIndexed { i, entry ->
                val result = results[i]
                val label = providerRepository.instance(entry.providerInstanceId)?.label.orEmpty()
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(entry.model.displayName, fontWeight = FontWeight.SemiBold)
                    if (label.isNotBlank()) {
                        Text(label, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (result.status == AltivModelHealthProbe.Status.RUNNING) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                        Text(
                            stringResource(statusLabels.getValue(result.status)),
                            color = if (result.status == AltivModelHealthProbe.Status.WORKING)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (result.latencyMs > 0L) {
                            Text("${result.latencyMs} ms", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
