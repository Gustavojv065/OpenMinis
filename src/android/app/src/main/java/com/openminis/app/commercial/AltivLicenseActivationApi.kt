package com.openminis.app.commercial

import android.content.Context
import android.os.Build
import com.openminis.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

internal data class AltivLicenseSession(
    val activationToken: String,
    val last4: String,
    val expiresAt: String?,
    val maxDevices: Int,
)

/**
 * Compatible with altiv-admin/supabase/functions/license-access.
 * The Edge Function validates licenses on the server: this client only reports the result.
 * Its function URL is a public endpoint, not a secret. The publishable key is public.
 * Production CI's protected environment variables take precedence over the public
 * fallback used by altiv-admin/src/lib/supabase.ts.
 */
internal class AltivLicenseActivationApi(
    serviceUrl: String = BuildConfig.ALTIV_COMMERCIAL_URL.ifBlank {
        "https://xdixbbhquctxioznxdma.supabase.co"
    },
    private val publishableKey: String = BuildConfig.ALTIV_COMMERCIAL_PUBLISHABLE_KEY.ifBlank {
        "sb_publishable__2UBUgQqikD_PG9PyVnOdQ_j_G18F8r"
    },
) {
    private val base = serviceUrl.trimEnd('/')
    val configured: Boolean = runCatching {
        val uri = URI(base)
        uri.scheme == "https" && uri.host?.endsWith(".supabase.co") == true &&
            uri.userInfo == null && uri.port == -1 && publishableKey.isNotBlank()
    }.getOrDefault(false)

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build()

    private fun deviceFingerprint(context: Context): String {
        val prefs = context.getSharedPreferences("altiv_install_identity", Context.MODE_PRIVATE)
        val existing = prefs.getString("device_uuid", null)
        if (!existing.isNullOrBlank()) return existing
        val fresh = UUID.randomUUID().toString()
        prefs.edit().putString("device_uuid", fresh).apply()
        return fresh
    }

    suspend fun activate(context: Context, rawKey: String): AltivLicenseSession =
        withContext(Dispatchers.IO) {
            val code = AltivLicenseActivationContract.normalizeLicenseKey(rawKey)
            require(AltivLicenseActivationContract.acceptsCode(code)) {
                "Informe uma chave de licença ALTIV válida."
            }
            val body = JSONObject().put("action", "activate")
                .put("licenseKey", code)
                .put("deviceFingerprint", deviceFingerprint(context))
                .put("deviceLabel", "ALTIVE DEV AI Android")
                .put("appVersion", BuildConfig.VERSION_NAME)
            val result = request(body)
            val license = result.optJSONObject("license")
                ?: throw IllegalStateException("Servidor retornou uma licença incompleta.")
            val token = result.optString("activationToken")
            val status = license.optString("status")
            if (!AltivLicenseActivationContract.isValidActivationResult(
                    result.optBoolean("ok"), token, status)) {
                throw IllegalStateException("Não foi possível confirmar esta licença.")
            }
            AltivLicenseSession(token, license.optString("last4"),
                license.optString("expiresAt").takeUnless { it.isBlank() || it == "null" },
                license.optInt("maxDevices", 1))
        }

    suspend fun validate(context: Context, activationToken: String): AltivLicenseSession =
        withContext(Dispatchers.IO) {
            require(activationToken.isNotBlank()) { "Ativação não encontrada." }
            val body = JSONObject().put("action", "validate")
                .put("activationToken", activationToken)
                .put("deviceFingerprint", deviceFingerprint(context))
                .put("deviceLabel", "ALTIVE DEV AI Android")
                .put("appVersion", BuildConfig.VERSION_NAME)
            val result = request(body)
            val license = result.optJSONObject("license")
                ?: throw IllegalStateException("Licença não encontrada no servidor.")
            require(result.optBoolean("ok") && license.optString("status") == "active") {
                "Licença não está ativa."
            }
            AltivLicenseSession(activationToken, license.optString("last4"),
                license.optString("expiresAt").takeUnless { it.isBlank() || it == "null" },
                license.optInt("maxDevices", 1))
        }

    suspend fun deactivate(context: Context, activationToken: String) = withContext(Dispatchers.IO) {
        require(activationToken.isNotBlank()) { "Ativação não encontrada." }
        val result = request(JSONObject().put("action", "deactivate")
            .put("activationToken", activationToken)
            .put("deviceFingerprint", deviceFingerprint(context))
            .put("deviceLabel", "ALTIVE DEV AI Android")
            .put("appVersion", BuildConfig.VERSION_NAME))
        if (!result.optBoolean("ok")) throw IllegalStateException("Não foi possível liberar o dispositivo.")
    }

    private fun request(json: JSONObject): JSONObject {
        require(configured) { "Serviço de licenças não configurado nesta versão." }
        val req = Request.Builder()
            .url(base + AltivLicenseActivationContract.FUNCTION_PATH)
            .header("apikey", publishableKey)
            .header("Accept", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return http.newCall(req).execute().use { response ->
            if (!response.isSuccessful) {
                // Never echo upstream error bodies containing identifiers or secrets.
                throw IllegalStateException(AltivLicenseActivationContract.messageForStatus(response.code))
            }
            val raw = response.body?.string().orEmpty()
            runCatching { JSONObject(raw) }.getOrElse {
                throw IllegalStateException("Resposta inválida do servidor de licenças.")
            }
        }
    }
}
