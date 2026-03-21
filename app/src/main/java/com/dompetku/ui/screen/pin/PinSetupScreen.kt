package com.dompetku.ui.screen.pin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dompetku.util.HapticHelper

private enum class PinSetupStage { VERIFY_OLD, ENTER_NEW, CONFIRM_NEW }

@Composable
fun PinSetupScreen(
    isChangePin: Boolean,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: PinViewModel = hiltViewModel(),
) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var stage by remember(isChangePin) {
        mutableStateOf(if (isChangePin) PinSetupStage.VERIFY_OLD else PinSetupStage.ENTER_NEW)
    }
    var current by remember { mutableStateOf("") }
    var firstEntry by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(result) {
        when (result) {
            is PinResult.Success -> {
                if (stage == PinSetupStage.VERIFY_OLD) {
                    stage = PinSetupStage.ENTER_NEW
                    current = ""
                    errorMsg = ""
                } else if (stage == PinSetupStage.CONFIRM_NEW) {
                    viewModel.setPinEnabled(true)
                    onSuccess()
                }
                viewModel.resetResult()
            }
            is PinResult.Error -> {
                errorMsg = (result as PinResult.Error).message
                current = ""
                if (stage != PinSetupStage.VERIFY_OLD) {
                    stage = PinSetupStage.ENTER_NEW
                    firstEntry = ""
                }
                viewModel.resetResult()
            }
            PinResult.Idle -> Unit
        }
    }

    val subtitle = when (stage) {
        PinSetupStage.VERIFY_OLD -> "Masukkan PIN Lama"
        PinSetupStage.ENTER_NEW -> if (isChangePin) "Masukkan PIN Baru" else "Buat PIN 6 Digit"
        PinSetupStage.CONFIRM_NEW -> "Konfirmasi PIN Baru"
    }

    fun handleKey(key: String) {
        HapticHelper.tapLight(context)
        errorMsg = ""
        when (key) {
            "⌫" -> if (current.isNotEmpty()) current = current.dropLast(1)
            else -> {
                if (current.length >= 6) return
                val next = current + key
                current = next
                if (next.length < 6) return

                when (stage) {
                    PinSetupStage.VERIFY_OLD -> viewModel.verifyPin(next)
                    PinSetupStage.ENTER_NEW -> {
                        firstEntry = next
                        current = ""
                        stage = PinSetupStage.CONFIRM_NEW
                    }
                    PinSetupStage.CONFIRM_NEW -> {
                        if (next == firstEntry) {
                            viewModel.savePin(next)
                        } else {
                            errorMsg = "PIN tidak cocok"
                            current = ""
                            firstEntry = ""
                            stage = PinSetupStage.ENTER_NEW
                        }
                    }
                }
            }
        }
    }

    PinScreenLayout(
        subtitle = subtitle,
        filledCount = current.length,
        errorMsg = errorMsg,
        keys = PinSetupKeys,
        onKeyPress = ::handleKey,
        onCancel = onCancel,
    )
}
