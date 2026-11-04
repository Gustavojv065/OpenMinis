package com.openminis.app.ui.commercial

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PasswordVisualTransformation
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.openminis.app.commercial.AltivAuthTokens
import com.openminis.app.commercial.AltivCommercialAccountState
import com.openminis.app.commercial.AltivCommercialAuthApi
import com.openminis.app.commercial.AltivCommercialSessionStore
import com.openminis.app.commercial.AltivSubscription
import com.openminis.app.commercial.AltivSubscriptionPolicy
import com.openminis.app.ui.components.AppBrandLogo
import kotlinx.coroutines.launch

/**
 * Commercial account entry point for ALTIV builds only.
 * Does not lock the GPL app's local files or BYOK features. ALTIV-hosted
 * premium endpoints must independently enforce subscription on the server.
 */
@Composable
internal fun AltivCommercialEntry(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val api = remember { AltivCommercialAuthApi() }
    val store = remember { AltivCommercialSessionStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(if (api.configured) "checking" else "setup") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var subscription by remember { mutableStateOf<AltivSubscription?>(null) }

    suspend fun verify(tokens: AltivAuthTokens) {
        try {
            val current = api.subscription(tokens.accessToken)
            subscription = current
            AltivCommercialAccountState.signedIn.value = true
            AltivCommercialAccountState.subscription.value = current
            phase = if (AltivSubscriptionPolicy.allowsHostedServices(current)) "app" else "expired"
            error = null
        } catch (exception: Exception) {
            phase = "offline"
            error = exception.message ?: "Não foi possível validar a assinatura"
        }
    }

    LaunchedEffect(api) {
        if (!api.configured) return@LaunchedEffect
        val stored = store.read()
        if (stored == null) {
            phase = "login"
        } else {
            try {
                val fresh = api.refresh(stored.refreshToken)
                store.save(fresh)
                verify(fresh)
            } catch (_: Exception) {
                // Never grant a paid entitlement from an unverified cached token.
                phase = "login"
                error = "Entre novamente para verificar sua assinatura"
            }
        }
    }

    LaunchedEffect(store) {
        AltivCommercialAccountState.logoutRequests.collect {
            store.clear()
            subscription = null
            password = ""
            phase = if (api.configured) "login" else "setup"
        }
    }

    if (phase == "app" || phase == "local") {
        content()
        return
    }

    Scaffold { insets ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppBrandLogo(modifier = Modifier.padding(6.dp))
            Text("ALTIV DEV AI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Conta e assinatura", style = MaterialTheme.typography.titleMedium)
            when (phase) {
                "checking" -> {
                    CircularProgressIndicator()
                    Text("Verificando acesso com o servidor...")
                }
                "setup" -> {
                    Text("O aplicativo está pronto para conexão comercial. O servidor de assinaturas ainda não foi configurado nesta compilação.")
                    Text("Nenhuma credencial deve ser digitada até que o serviço seja publicado.")
                }
                "login" -> {
                    Text("Entre com as credenciais recebidas na contratação do serviço ALTIV.")
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    Button(
                        onClick = {
                            if (busy) return@Button
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    val tokens = api.signIn(email, password)
                                    store.save(tokens)
                                    password = ""
                                    verify(tokens)
                                } catch (exception: Exception) {
                                    error = exception.message ?: "Não foi possível entrar"
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy && email.contains('@') && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (busy) "Validando..." else "Entrar com e-mail e senha") }
                }
                "expired" -> {
                    Text("Seu acesso aos serviços comerciais ALTIV não está ativo.")
                    Text(
                        "Situação: ${AltivSubscriptionPolicy.safeStatus(subscription)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    subscription?.expiresAt?.let { Text("Vencimento: $it") }
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://wa.me/5565992666940?text=Quero%20renovar%20minha%20assinatura%20ALTIV%20DEV%20AI")))
                    }, modifier = Modifier.fillMaxWidth()) { Text("Solicitar renovação") }
                    OutlinedButton(onClick = {
                        phase = "checking"
                        busy = true
                        scope.launch {
                            try {
                                val old = store.read()
                                if (old == null) phase = "login"
                                else {
                                    val fresh = api.refresh(old.refreshToken)
                                    store.save(fresh)
                                    verify(fresh)
                                }
                            } catch (_: Exception) {
                                phase = "login"
                                error = "Entre novamente para consultar a renovação"
                            } finally {
                                busy = false
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                        Text("Verificar renovação")
                    }
                    OutlinedButton(onClick = {
                        store.clear()
                        AltivCommercialAccountState.signedIn.value = false
                        AltivCommercialAccountState.subscription.value = null
                        phase = "login"
                    }, modifier = Modifier.fillMaxWidth()) { Text("Trocar conta") }
                }
                "offline" -> {
                    Text("Não foi possível confirmar sua assinatura agora. Os serviços hospedados permanecem indisponíveis.")
                    OutlinedButton(onClick = { phase = "login" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Tentar novamente")
                    }
                }
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { phase = "local" }, modifier = Modifier.fillMaxWidth()) {
                Text("Continuar com meus arquivos e ferramentas locais")
            }
            Text(
                "Arquivos e conversas existentes permanecem no aparelho. O acesso aos serviços pagos exige assinatura validada no servidor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
