package com.openminis.app.commercial

/**
 * ALTIV Admin's published license-access Edge Function contract.
 * License codes and activation tokens are DIFFERENT from provider API keys.
 * Pure functions so local tests never need an actual customer license.
 */
internal object AltivLicenseActivationContract {
    const val FUNCTION_PATH = "/functions/v1/license-access"
    private val codePattern = Regex("^ALTIV-[A-Z0-9-]{6,100}$")
    val actions = setOf("activate", "validate", "deactivate")

    fun normalizeLicenseKey(raw: String): String =
        raw.trim().uppercase(java.util.Locale.ROOT).replace(Regex("\\s+"), "")

    fun acceptsCode(raw: String): Boolean = codePattern.matches(normalizeLicenseKey(raw))

    /** Server is authoritative: a syntactically valid key is NOT a valid license. */
    fun isValidActivationResult(ok: Boolean, token: String, status: String): Boolean =
        ok && token.isNotBlank() && status == "active"

    fun messageForStatus(httpCode: Int): String = when (httpCode) {
        400, 401 -> "Chave inválida ou ativação não autorizada."
        403 -> "Licença bloqueada, cancelada ou vencida."
        404 -> "Serviço de ativação não publicado no servidor."
        409 -> "Limite de dispositivos desta licença atingido."
        429 -> "Muitas tentativas. Aguarde alguns minutos."
        500, 503 -> "Serviço de licenciamento temporariamente indisponível."
        else -> "Não foi possível validar a licença (HTTP $httpCode)."
    }
}
