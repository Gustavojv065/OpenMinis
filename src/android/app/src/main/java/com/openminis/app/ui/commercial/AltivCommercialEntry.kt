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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.openminis.app.commercial.AltivLicenseActivationApi
import com.openminis.app.commercial.AltivLicenseActivationContract
import com.openminis.app.commercial.AltivLicenseSession
import com.openminis.app.commercial.AltivCommercialSessionStore
import com.openminis.app.commercial.AltivSubscription
import com.openminis.app.commercial.AltivSubscriptionPolicy
import com.openminis.app.ui.components.AppBrandLogo
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * Commercial account entry point for ALTIV builds only.
 * The ALTIV commercial flavor requires a server-verified activation before
 * exposing the workspace or BYOK configuration. Existing local files remain
 * stored on device. Hosted endpoints independently enforce authorization.
 */
@Composable
internal fun AltivCommercialEntry(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val api = remember { AltivCommercialAuthApi() }
    val licenseApi = remember { AltivLicenseActivationApi() }
    val store = remember { AltivCommercialSessionStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(if (api.configured || licenseApi.configured) "checking" else "setup") }
    var licenseKey by remember { mutableStateOf("") }
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

    LaunchedEffect(api, licenseApi) {
        val licensed = store.readLicense()
        if (licensed != null && licenseApi.configured) {
            try {
                val valid = licenseApi.validate(context, licensed.activationToken)
                store.saveLicense(valid)
                val active = AltivSubscription(true, "license", "active", valid.expiresAt,
                    java.time.Instant.now().toString())
                subscription = active
                AltivCommercialAccountState.signedIn.value = true
                AltivCommercialAccountState.subscription.value = active
                phase = "app"
                error = null
                return@LaunchedEffect
            } catch (_: Exception) {
                // Never grant access based only on an encrypted local token.
                store.clearLicense()
                phase = "license"
                error = "Não foi possível validar a ativação anterior. Tente novamente."
                return@LaunchedEffect
            }
        }
        if (!api.configured) {
            phase = if (licenseApi.configured) "login" else "setup"
            return@LaunchedEffect
        }
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
            val licensed = store.readLicense()
            var deactivationFailed = false
            if (licensed != null) {
                try {
                    // Release the server device slot when the user explicitly signs out.
                    licenseApi.deactivate(context, licensed.activationToken)
                } catch (_: Exception) {
                    deactivationFailed = true
                }
            }
            store.clearLicense()
            AltivCommercialAccountState.signedIn.value = false
            AltivCommercialAccountState.subscription.value = null
            error = if (deactivationFailed) {
                "Sessão encerrada, mas o servidor não confirmou a liberação do dispositivo. " +
                    "Verifique os dispositivos no ALTIV ADMIN."
            } else null
            phase = if (api.configured || licenseApi.configured) "login" else "setup"
        }
    }

    if (AltivSubscriptionPolicy.allowsAppAccess(phase, subscription)) {
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
            AppBrandLogo(modifier = Modifier.padding(6.dp).height(104.dp))
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
                    Text("Entre com e-mail e senha ou utilize a chave gerada pelo painel ALTIV ADMIN.")
                    if (licenseApi.configured) {
                        OutlinedButton(onClick = {
                            error = null
                            phase = "license"
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Ativar com chave ALTIV")
                        }
                    }
                    if (!api.configured) {
                        Text("Entrada por e-mail indisponível nesta compilação; utilize a chave ALTIV.")
                    } else {
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
                }
                "license" -> {
                    Text("Digite uma chave ALTIV-... gerada no painel administrativo. " +
                        "A chave é verificada pelo servidor e vinculada a este aparelho.")
                    OutlinedTextField(
                        value = licenseKey,
                        onValueChange = { licenseKey = it.take(120) },
                        label = { Text("Chave de licença ALTIV") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        enabled = !busy && AltivLicenseActivationContract.acceptsCode(licenseKey),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (busy) return@Button
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    val activated = licenseApi.activate(context, licenseKey)
                                    store.saveLicense(activated)
                                    licenseKey = ""
                                    val active = AltivSubscription(
                                        true, "license", "active", activated.expiresAt,
                                        java.time.Instant.now().toString())
                                    subscription = active
                                    AltivCommercialAccountState.signedIn.value = true
                                    AltivCommercialAccountState.subscription.value = active
                                    phase = "app"
                                } catch (e: Exception) {
                                    error = e.message ?: "Não foi possível validar a licença."
                                } finally {
                                    busy = false
                                }
                            }
                        },
                    ) { Text(if (busy) "Verificando..." else "Ativar licença") }
                    OutlinedButton(onClick = {
                        licenseKey = ""
                        error = null
                        phase = "login"
                    }, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
                    Text("A licença não é uma chave API. Nunca compartilhe chaves de clientes.",
                        style = MaterialTheme.typography.bodySmall)
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
                        store.clearLicense()
                        AltivCommercialAccountState.signedIn.value = false
                        AltivCommercialAccountState.subscription.value = null
                        phase = "login"
                    }, modifier = Modifier.fillMaxWidth()) { Text("Trocar conta") }
                    if (licenseApi.configured) {
                        OutlinedButton(onClick = { error = null; phase = "license" },
                            modifier = Modifier.fillMaxWidth()) { Text("Ativar outra licença") }
                    }
                }
                "offline" -> {
                    Text("Não foi possível confirmar sua assinatura agora. Os serviços hospedados permanecem indisponíveis.")
                    OutlinedButton(onClick = { phase = "login" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Tentar novamente")
                    }
                    if (licenseApi.configured) {
                        OutlinedButton(onClick = { phase = "license" }, modifier = Modifier.fillMaxWidth()) {
                            Text("Entrar com licença ALTIV")
                        }
                    }
                }
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Ative sua licença ALTIV para acessar arquivos, conversas, ferramentas e configurar suas próprias chaves de API. Seus arquivos existentes permanecem no aparelho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
