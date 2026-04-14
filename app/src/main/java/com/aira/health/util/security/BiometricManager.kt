package com.aira.health.util.security

import android.content.Context
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages optional biometric App Lock.
 * This gates the UI only — it is completely independent of DB encryption.
 * Enabled by user in Settings → Security → App Lock.
 */
@Singleton
class BiometricManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val message: String) : AuthResult()
        object NotEnrolled : AuthResult()
        object NotAvailable : AuthResult()
    }

    fun isBiometricAvailable(): Boolean {
        val manager = AndroidBiometricManager.from(context)
        return manager.canAuthenticate(
            AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
            AndroidBiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == AndroidBiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun authenticate(activity: FragmentActivity, title: String = "Unlock Aira"): AuthResult {
        if (!isBiometricAvailable()) return AuthResult.NotAvailable

        return suspendCancellableCoroutine { cont ->
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (cont.isActive) cont.resume(AuthResult.Success)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (cont.isActive) cont.resume(AuthResult.Error(errString.toString()))
                }
                override fun onAuthenticationFailed() {
                    // Single failure — do not resolve, let user retry
                }
            })

            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle("Use your fingerprint or face to continue")
                .setAllowedAuthenticators(
                    AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
                    AndroidBiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            prompt.authenticate(info)
        }
    }
}
