package com.openminis.app.commercial

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore-backed encrypted session storage.
 * Never writes access/refresh tokens as plaintext or logs them.
 * Independent from provider API keys, the Linux workspace and local backups.
 */
internal class AltivCommercialSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("altiv_commercial_secure_v1", Context.MODE_PRIVATE)
    private val alias = "online.altiv.devai.commercial_session_v1"

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    fun save(tokens: AltivAuthTokens) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val plain = JSONObject()
            .put("a", tokens.accessToken)
            .put("r", tokens.refreshToken).toString().toByteArray(Charsets.UTF_8)
        val encrypted = cipher.doFinal(plain)
        val packed = cipher.iv + encrypted
        prefs.edit().putString("session", Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun read(): AltivAuthTokens? {
        val stored = prefs.getString("session", null) ?: return null
        return try {
            val packed = Base64.decode(stored, Base64.NO_WRAP)
            if (packed.size < 29) return null
            val iv = packed.copyOfRange(0, 12)
            val encrypted = packed.copyOfRange(12, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            val item = JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))
            AltivAuthTokens(item.getString("a"), item.getString("r"))
        } catch (_: Exception) {
            // Restored app data cannot recover keys bound to a different device.
            clear()
            null
        }
    }

    /** License activation token: distinct from user OAuth sessions and API keys. */
    fun saveLicense(license: AltivLicenseSession) {
        val plain = JSONObject()
            .put("token", license.activationToken)
            .put("last4", license.last4)
            .put("expiresAt", license.expiresAt)
            .put("maxDevices", license.maxDevices)
            .toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val packed = cipher.iv + cipher.doFinal(plain)
        check(prefs.edit().putString(
            "license_activation", Base64.encodeToString(packed, Base64.NO_WRAP)
        ).commit()) { "Não foi possível salvar a ativação com segurança." }
    }

    fun readLicense(): AltivLicenseSession? {
        val stored = prefs.getString("license_activation", null) ?: return null
        return try {
            val packed = Base64.decode(stored, Base64.NO_WRAP)
            if (packed.size < 29) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(),
                GCMParameterSpec(128, packed.copyOfRange(0, 12)))
            val json = JSONObject(String(cipher.doFinal(
                packed.copyOfRange(12, packed.size)), Charsets.UTF_8))
            AltivLicenseSession(
                activationToken = json.getString("token"),
                last4 = json.optString("last4"),
                expiresAt = json.optString("expiresAt").takeUnless {
                    it.isBlank() || it == "null"
                },
                maxDevices = json.optInt("maxDevices", 1),
            )
        } catch (_: Exception) {
            clearLicense()
            null
        }
    }

    fun clearLicense() {
        prefs.edit().remove("license_activation").apply()
    }

    fun clear() {
        prefs.edit().remove("session").apply()
    }
}
