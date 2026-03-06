package com.dompetku.ui.screen.pin

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.dompetku.data.preferences.UserPreferences
import com.dompetku.util.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PinResult {
    object Idle    : PinResult
    object Success : PinResult
    data class Error(val message: String) : PinResult
}

@HiltViewModel
class PinViewModel @Inject constructor(
    private val prefs: UserPreferences,
) : ViewModel() {

    private val _result = MutableStateFlow<PinResult>(PinResult.Idle)
    val result = _result.asStateFlow()

    fun resetResult() { _result.value = PinResult.Idle }

    // ── Unlock mode: verify PIN against stored hash ───────────────────────────
    fun verifyPin(entered: String) {
        viewModelScope.launch {
            val p = prefs.appPrefsFlow.first()
            if (PinHasher.verify(entered, p.pinHash)) {
                _result.value = PinResult.Success
            } else {
                _result.value = PinResult.Error("PIN salah")
            }
        }
    }

    // ── Set mode: save new PIN hash ───────────────────────────────────────────
    fun savePin(pin: String) {
        viewModelScope.launch {
            val hash = PinHasher.hash(pin)
            prefs.setPinHash(hash)
            _result.value = PinResult.Success
        }
    }

    // ── Biometric authentication ──────────────────────────────────────────────
    // error code 10 = user cancel — treated as non-error per Master Prompt
    fun authenticateBiometric(activity: FragmentActivity, onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // errorCode 10 = user pressed cancel — not a real error
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    _result.value = PinResult.Error("Biometrik gagal: $errString")
                }
            }
            override fun onAuthenticationFailed() {
                _result.value = PinResult.Error("Biometrik gagal/dibatalkan.")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka DompetKu")
            .setSubtitle("Gunakan sidik jari atau wajah")
            .setNegativeButtonText("Pakai PIN")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}
