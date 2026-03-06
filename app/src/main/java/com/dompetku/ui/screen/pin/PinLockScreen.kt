package com.dompetku.ui.screen.pin

import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.ui.components.DompetKuLogo
import com.dompetku.ui.theme.*

// ── Modes ─────────────────────────────────────────────────────────────────────
enum class PinMode { UNLOCK, SET }

/**
 * Full-screen PIN screen.
 *
 * UNLOCK mode: user enters existing PIN (or uses biometric) to proceed.
 * SET mode:    user enters a new PIN twice (enter → confirm) to save it.
 *
 * @param mode         UNLOCK or SET
 * @param bioEnabled   show biometric button in UNLOCK mode
 * @param onSuccess    called after successful unlock OR successful PIN set
 * @param onCancel     optional cancel callback (shown as "Batal" text button)
 */
@Composable
fun PinLockScreen(
    mode:       PinMode = PinMode.UNLOCK,
    bioEnabled: Boolean = false,
    onSuccess:  () -> Unit,
    onCancel:   (() -> Unit)? = null,
    viewModel:  PinViewModel = hiltViewModel(),
) {
    // Two-step state for SET mode: enter → confirm
    var pinStep  by remember { mutableStateOf(PinStep.ENTER) }
    var entered  by remember { mutableStateOf("") }   // first entry (SET mode)
    var current  by remember { mutableStateOf("") }   // active field
    var errorMsg by remember { mutableStateOf("") }

    val result by viewModel.result.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    // React to ViewModel result
    LaunchedEffect(result) {
        when (result) {
            is PinResult.Success -> {
                viewModel.resetResult()
                onSuccess()
            }
            is PinResult.Error -> {
                errorMsg = (result as PinResult.Error).message
                current  = ""
                if (mode == PinMode.SET) { pinStep = PinStep.ENTER; entered = "" }
                viewModel.resetResult()
            }
            PinResult.Idle -> { /* no-op */ }
        }
    }

    // 3x4 numpad grid keys — index 9 = BIO or blank, index 10 = 0, index 11 = backspace
    val keys = listOf("1","2","3","4","5","6","7","8","9",
        if (mode == PinMode.UNLOCK && bioEnabled) "BIO" else "",
        "0", "⌫")

    val subtitle = when {
        mode == PinMode.SET && pinStep == PinStep.CONFIRM -> "Konfirmasi PIN"
        mode == PinMode.SET                               -> "Buat PIN 4 Digit"
        else                                              -> "Masukkan PIN"
    }

    fun handleKey(key: String) {
        errorMsg = ""
        when (key) {
            "⌫" -> { if (current.isNotEmpty()) current = current.dropLast(1) }
            "BIO" -> {
                activity?.let { act ->
                    viewModel.authenticateBiometric(act) { onSuccess() }
                }
            }
            else -> {
                if (current.length >= 6) return
                val next = current + key

                current = next
                if (next.length < 6) return

                // 4 digits reached
                when (mode) {
                    PinMode.UNLOCK -> viewModel.verifyPin(next)
                    PinMode.SET -> {
                        if (pinStep == PinStep.ENTER) {
                            entered  = next
                            current  = ""
                            pinStep  = PinStep.CONFIRM
                        } else {
                            // Confirm step
                            if (next == entered) {
                                viewModel.savePin(next)
                            } else {
                                errorMsg = "PIN tidak cocok"
                                current  = ""
                                entered  = ""
                                pinStep  = PinStep.ENTER
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(GreenPrimary, GreenDark),
                    start  = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end    = androidx.compose.ui.geometry.Offset(400f, 1200f),
                )
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(24.dp),
        ) {
            DompetKuLogo(size = 48.dp, color = Color.White)

            Spacer(Modifier.height(16.dp))

            Text(
                text       = "DompetKu",
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text     = subtitle,
                fontSize = 13.sp,
                color    = Color.White.copy(alpha = 0.75f),
            )

            Spacer(Modifier.height(32.dp))

            // 4-dot PIN indicator
            PinDotRow(filledCount = current.length)

            Spacer(Modifier.height(12.dp))

            // Error message
            if (errorMsg.isNotEmpty()) {
                Text(
                    text       = errorMsg,
                    fontSize   = 13.sp,
                    color      = Color(0xFFFCA5A5),
                    modifier   = Modifier.padding(bottom = 8.dp),
                )
            } else {
                Spacer(Modifier.height(21.dp))
            }

            Spacer(Modifier.height(16.dp))

            // 3×4 numpad
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { key ->
                            NumpadKey(
                                key     = key,
                                onClick = { if (key.isNotEmpty()) handleKey(key) },
                            )
                        }
                    }
                }
            }

            // Cancel button
            if (onCancel != null) {
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onCancel) {
                    Text(
                        text     = "Batal",
                        fontSize = 14.sp,
                        color    = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

// ── 4-dot row ─────────────────────────────────────────────────────────────────

@Composable
private fun PinDotRow(filledCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(6) { i ->
            val filled = i < filledCount
            val bg by animateColorAsState(
                targetValue   = if (filled) Color.White else Color.White.copy(alpha = 0.3f),
                animationSpec = tween(150),
                label         = "dot$i",
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(bg, CircleShape),
            )
        }
    }
}

// ── Single numpad key ──────────────────────────────────────────────────────────

@Composable
private fun NumpadKey(key: String, onClick: () -> Unit) {
    val isEmpty    = key.isEmpty()
    val isSpecial  = key == "⌫" || key == "BIO"

    Button(
        onClick  = onClick,
        enabled  = !isEmpty,
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = if (isSpecial)
                Color.White.copy(alpha = 0.15f)
            else
                Color.White.copy(alpha = 0.2f),
            contentColor           = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor   = Color.Transparent,
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
        modifier  = Modifier.size(width = 72.dp, height = 64.dp),
    ) {
        when (key) {
            "⌫"   -> Icon(PhosphorIcons.Regular.Backspace, null, tint = Color.White, modifier = Modifier.size(22.dp))
            "BIO" -> Icon(PhosphorIcons.Regular.Fingerprint, null, tint = Color.White, modifier = Modifier.size(26.dp))
            ""    -> { /* invisible placeholder */ }
            else  -> Text(key, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ── PIN step state (SET mode) ─────────────────────────────────────────────────
private enum class PinStep { ENTER, CONFIRM }
