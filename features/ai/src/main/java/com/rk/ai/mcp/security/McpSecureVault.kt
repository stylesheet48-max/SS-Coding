package com.rk.ai.mcp.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore hardware-backed secure vault for MCP server credentials, API tokens, and sensitive runtime
 * configuration.
 */
object McpSecureVault {
    private const val TAG = "McpSecureVault"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "ss_coding_mcp_master_key"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val PREFS_NAME = "ss_coding_mcp_secure_vault"
    private const val VERSION_PREFIX = "v1:"

    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ensureMasterKey()
        }
    }

    private fun ensureMasterKey() {
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec =
                    KeyGenParameterSpec.Builder(
                            MASTER_KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
                Log.i(TAG, "Initialized new hardware-backed AES-256-GCM master key in AndroidKeyStore")
            }
        }
            .onFailure { e -> Log.e(TAG, "Failed to initialize KeyStore master key", e) }
    }

    private fun getMasterKey(): SecretKey? {
        return runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey
        }
            .getOrNull()
    }

    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        val secretKey = getMasterKey() ?: return plaintext // Fallback to raw if Keystore unavailable
        return runCatching {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
            "$VERSION_PREFIX$ivBase64:$cipherTextBase64"
        }
            .getOrElse { e ->
                Log.e(TAG, "Encryption failed, falling back to unencrypted text", e)
                plaintext
            }
    }

    fun decrypt(encryptedBlob: String): String {
        if (encryptedBlob.isEmpty()) return ""
        if (!encryptedBlob.startsWith(VERSION_PREFIX)) {
            // Not encrypted with v1 scheme; return as legacy plaintext
            return encryptedBlob
        }
        val secretKey = getMasterKey() ?: return encryptedBlob
        return runCatching {
            val parts = encryptedBlob.removePrefix(VERSION_PREFIX).split(":")
            if (parts.size != 2) return encryptedBlob
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        }
            .getOrElse { e ->
                Log.e(TAG, "Decryption failed", e)
                ""
            }
    }

    fun saveSecret(key: String, secret: String) {
        val prefs = sharedPreferences ?: return
        val encrypted = encrypt(secret)
        prefs.edit().putString(key, encrypted).apply()
    }

    fun getSecret(key: String): String? {
        val prefs = sharedPreferences ?: return null
        val stored = prefs.getString(key, null) ?: return null
        return decrypt(stored)
    }

    fun deleteSecret(key: String) {
        sharedPreferences?.edit()?.remove(key)?.apply()
    }
}
