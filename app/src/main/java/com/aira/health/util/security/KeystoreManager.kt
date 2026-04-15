package com.aira.health.util.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages AES-256 encryption keys in the Android Keystore.
 * Key is generated on first cold start — before any UI is shown.
 * The DB key is completely independent of biometric authentication.
 */
@Singleton
class KeystoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val DB_KEY_ALIAS = "aira_db_key"
        private const val KEY_SIZE = 256
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val DB_PASSPHRASE_SIZE = 32
        private const val GCM_TAG_LENGTH_BITS = 128

        private const val SECURITY_PREFS_NAME = "aira_security_prefs"
        private const val DB_PASSPHRASE_CT_KEY = "db_passphrase_ct"
        private const val DB_PASSPHRASE_IV_KEY = "db_passphrase_iv"
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

    // SQLCipher passphrase is random and persisted encrypted with Android Keystore AES key.
    fun getDatabasePassphrase(): ByteArray {
        val key = getOrCreateDatabaseKey()

        getStoredEncryptedPassphrase()?.let { encrypted ->
            decryptPassphrase(key, encrypted)?.let { return it }
            clearStoredPassphrase()
        }

        val generatedPassphrase = ByteArray(DB_PASSPHRASE_SIZE).also { SecureRandom().nextBytes(it) }
        val encryptedPassphrase = encryptPassphrase(key, generatedPassphrase)
        storeEncryptedPassphrase(encryptedPassphrase)
        return generatedPassphrase
    }

    private fun encryptPassphrase(key: SecretKey, passphrase: ByteArray): EncryptedPassphrase {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val ciphertext = cipher.doFinal(passphrase)
        return EncryptedPassphrase(ciphertext = ciphertext, iv = cipher.iv)
    }

    private fun decryptPassphrase(key: SecretKey, encrypted: EncryptedPassphrase): ByteArray? {
        return runCatching {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, encrypted.iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            cipher.doFinal(encrypted.ciphertext)
        }.getOrNull()
    }

    private fun getStoredEncryptedPassphrase(): EncryptedPassphrase? {
        val prefs = context.getSharedPreferences(SECURITY_PREFS_NAME, Context.MODE_PRIVATE)
        val ciphertextB64 = prefs.getString(DB_PASSPHRASE_CT_KEY, null) ?: return null
        val ivB64 = prefs.getString(DB_PASSPHRASE_IV_KEY, null) ?: return null

        val ciphertext = Base64.decode(ciphertextB64, Base64.NO_WRAP)
        val iv = Base64.decode(ivB64, Base64.NO_WRAP)
        return EncryptedPassphrase(ciphertext = ciphertext, iv = iv)
    }

    private fun storeEncryptedPassphrase(encrypted: EncryptedPassphrase) {
        val prefs = context.getSharedPreferences(SECURITY_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(DB_PASSPHRASE_CT_KEY, Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP))
            .putString(DB_PASSPHRASE_IV_KEY, Base64.encodeToString(encrypted.iv, Base64.NO_WRAP))
            .apply()
    }

    private fun clearStoredPassphrase() {
        val prefs = context.getSharedPreferences(SECURITY_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(DB_PASSPHRASE_CT_KEY)
            .remove(DB_PASSPHRASE_IV_KEY)
            .apply()
    }

    private data class EncryptedPassphrase(
        val ciphertext: ByteArray,
        val iv: ByteArray
    )
}
