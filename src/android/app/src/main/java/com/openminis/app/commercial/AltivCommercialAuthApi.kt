package com.openminis.app.commercial

import com.openminis.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

internal data class AltivAuthTokens(val accessToken: String, val refreshToken: String)

internal class AltivCommercialAuthApi(
    private val serviceUrl: String = BuildConfig.ALTIV_COMMERCIAL_URL,
    private val publishableKey: String = BuildConfig.ALTIV_COMMERCIAL_PUBLISHABLE_KEY,
) {
    private val base = serviceUrl.trimEnd('/')
    val configured: Boolean = runCatching {
        val uri = URI(base)
        uri.scheme == "https" && !uri.host.isNullOrBlank() &&
            uri.userInfo == null && publishableKey.isNotBlank()
    }.getOrDefault(false)

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    /** The publishable key is safe in the APK. Never ship the secret/service_role. */
    suspend fun signIn(email: String, password: String): AltivAuthTokens = withContext(Dispatchers.IO) {
        require(configured) { "Serviço ALTIV ainda não configurado" }
        require(email.contains('@') && password.isNotBlank()) { "Informe e-mail e senha" }
        val json = post(
            "/auth/v1/token?grant_type=password",
            JSONObject().put("email", email.trim()).put("password", password),
        )
        tokens(json)
    }

    suspend fun refresh(refreshToken: String): AltivAuthTokens = withContext(Dispatchers.IO) {
        require(configured) { "Serviço ALTIV ainda não configurado" }
        val json = post(
            "/auth/v1/token?grant_type=refresh_token",
            JSONObject().put("refresh_token", refreshToken),
        )
        tokens(json)
    }

    suspend fun subscription(accessToken: String): AltivSubscription? = withContext(Dispatchers.IO) {
        require(configured) { "Serviço ALTIV ainda não configurado" }
        val response = post(
            "/rest/v1/rpc/altiv_my_subscription",
            JSONObject(),
            bearerToken = accessToken,
        )
        val rows = JSONArray(response)
        if (rows.length() == 0) return@withContext null
        val first = rows.getJSONObject(0)
        AltivSubscription(
            active = first.optBoolean("is_active", false),
            plan = first.optString("plan_code", "monthly"),
            status = first.optString("subscription_status", "pending"),
            expiresAt = first.optString("expires_at").takeIf { it.isNotBlank() },
            checkedAt = first.optString("checked_at").takeIf { it.isNotBlank() },
        )
    }

    private fun tokens(json: String): AltivAuthTokens {
        val item = JSONObject(json)
        return AltivAuthTokens(
            accessToken = item.getString("access_token").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Resposta de autenticação inválida"),
            refreshToken = item.getString("refresh_token").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Resposta de autenticação inválida"),
        )
    }

    /** Throw generic errors; never display responses containing account details or tokens. */
    private fun post(path: String, json: JSONObject, bearerToken: String? = null): String {
        require(configured) { "Serviço ALTIV ainda não configurado" }
        val request = Request.Builder()
            .url(base + path)
            .header("apikey", publishableKey)
            .header("Accept", "application/json")
            .apply { if (bearerToken != null) header("Authorization", "Bearer $bearerToken") }
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException(when (response.code) {
                    400, 401, 403 -> "Credenciais inválidas ou sessão expirada"
                    429 -> "Muitas tentativas. Aguarde e tente novamente"
                    404 -> "Serviço de assinaturas ainda não implantado"
                    else -> "Não foi possível validar a assinatura (HTTP ${response.code})"
                })
            }
            response.body?.string().orEmpty()
        }
    }
}
