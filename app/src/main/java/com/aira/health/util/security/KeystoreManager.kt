package com.aira.health.util.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages AES-256 encryption keys in the Android Keystore.
 * Key is generated on first cold start — before any UI is shown.
 * The DB key is completely independent of biometric authentication.
 */
@Singleton
class KeystoreManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val DB_KEY_ALIAS = "aira_db_key"
        private const val KEY_SIZE = 256
    }

    /**
     * Returns the AES-256 DB encryption key, generating it if not already present.
     * Called at Application.onCreate() before Room initializes.
     */
    fun getOrCreateDatabaseKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        return if (keyStore.containsAlias(DB_KEY_ALIAS)) {
            (keyStore.getEntry(DB_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            generateDatabaseKey()
        }
    }

    private fun generateDatabaseKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            DB_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(false) // Key is independent of biometrics
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Derives a 32-byte passphrase from the Keystore key for SQLCipher.
     * SQLCipher accepts a byte array passphrase directly for stronger security
     * than a String-based passphrase (avoids String pool retention).
     */
    fun getDatabasePassphrase(): ByteArray {
        val key = getOrCreateDatabaseKey()
        return key.encoded ?: throw IllegalStateException("Cannot extract Keystore key bytes")
    }
}
