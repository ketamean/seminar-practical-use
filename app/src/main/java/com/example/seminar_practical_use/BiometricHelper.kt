package com.example.seminar_practical_use

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricHelper(private val activity: FragmentActivity) {

    enum class BiometricStatus {
        AVAILABLE,
        NO_HARDWARE,
        UNAVAILABLE,
        NOT_ENROLLED,
        UNKNOWN
    }

    sealed class AuthResult {
        data class Success(val cryptoObject: BiometricPrompt.CryptoObject?) : AuthResult()
        data class Error(val code: Int, val message: String) : AuthResult()
        object Failure : AuthResult()
    }

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    fun checkBiometricCapability(context: Context, allowWeakBiometrics: Boolean = false): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = if (allowWeakBiometrics) {
            BIOMETRIC_STRONG or BIOMETRIC_WEAK
        } else {
            BIOMETRIC_STRONG
        }
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            else -> BiometricStatus.UNKNOWN
        }
    }

    fun setupBiometricAuthentication(title: String = "Biometric Authentication",
                                     subtitle: String = "Please unlock to read the note",
                                     description: String = "Confirm your biometric to continue",
                                     allowDeviceCredential: Boolean = true,
                                     onResult: (AuthResult) -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)

        biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(AuthResult.Success(result.cryptoObject))
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(AuthResult.Error(errorCode, errString.toString()))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(AuthResult.Failure)
            }

        })

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)


        if (allowDeviceCredential) { // change to allow face and fingerprint
            promptBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        } else {
            promptBuilder.setNegativeButtonText("Cancel")
            promptBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        }

        promptInfo = promptBuilder.build()

    }

    fun authenticate() {
        biometricPrompt.authenticate(promptInfo)
    }
}