package com.dompetku.ui.screen.pin

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dompetku.data.preferences.UserPreferences
import com.dompetku.util.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PinResult {
    object Idle : PinResult
    object Success : PinResult
    data class Error(val message: String) : PinResult
}

@HiltViewModel
class PinViewModel @Inject constructor(
    private val prefs: UserPreferences,
) : ViewModel() {

    private val _result = MutableStateFlow<PinResult>(PinResult.Idle)
    val result = _result.asStateFlow()

    fun resetResult() {
        _result.value = PinResult.Idle
    }

    fun verifyPin(entered: String) {
        viewModelScope.launch {
            val p = prefs.appPrefsFlow.first()
            val ok = withContext(Dispatchers.Default) {
                PinHasher.verify(entered, p.pinHash)
            }
            if (ok && p.pinHash.isNotBlank() && !p.pinHash.startsWith("pbkdf2$")) {
                val upgradedHash = withContext(Dispatchers.Default) {
                    PinHasher.hash(entered)
                }
                prefs.setPinHash(upgradedHash)
            }
            _result.value = if (ok) PinResult.Success else PinResult.Error("PIN salah")
        }
    }

    fun savePin(pin: String) {
        viewModelScope.launch {
            val hash = withContext(Dispatchers.Default) {
                PinHasher.hash(pin)
            }
            prefs.setPinHash(hash)
            _result.value = PinResult.Success
        }
    }

    fun setPinEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setPinEnabled(enabled)
        }
    }

    fun authenticateBiometric(
        activity: FragmentActivity,
        title: String = "Buka DompetKu",
        subtitle: String = "Gunakan sidik jari atau wajah",
        negativeText: String = "Pakai PIN",
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)? = null,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    val message = "Biometrik gagal: $errString"
                    _result.value = PinResult.Error(message)
                    onError?.invoke(message)
                }
            }

            override fun onAuthenticationFailed() {
                Unit
            }
        }

        val allowedAuth = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeText)
            .setAllowedAuthenticators(allowedAuth)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}
