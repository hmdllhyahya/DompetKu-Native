package com.dompetku.ui.screen.pin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Backspace
import com.adamglin.phosphoricons.regular.Fingerprint
import com.dompetku.ui.components.DompetKuLogo
import com.dompetku.ui.theme.GreenDark
import com.dompetku.ui.theme.GreenPrimary

internal val PinSetupKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
internal val PinUnlockKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "BIO", "0", "⌫")

@Composable
internal fun PinScreenLayout(
    title: String = "DompetKu",
    subtitle: String,
    filledCount: Int,
    errorMsg: String,
    keys: List<String>,
    onKeyPress: (String) -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(GreenPrimary, GreenDark),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(400f, 1200f),
                )
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            DompetKuLogo(size = 48.dp, color = Color.White)
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(32.dp))
            PinDotRow(filledCount = filledCount)
            Spacer(Modifier.height(12.dp))

            if (errorMsg.isNotEmpty()) {
                Text(
                    text = errorMsg,
                    fontSize = 13.sp,
                    color = Color(0xFFFCA5A5),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else {
                Spacer(Modifier.height(21.dp))
            }

            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { key ->
                            NumpadKey(
                                key = key,
                                onClick = { if (key.isNotEmpty()) onKeyPress(key) },
                            )
                        }
                    }
                }
            }

            if (onCancel != null) {
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onCancel) {
                    Text(
                        text = "Batal",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PinDotRow(filledCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(6) { i ->
            val filled = i < filledCount
            val bg by animateColorAsState(
                targetValue = if (filled) Color.White else Color.White.copy(alpha = 0.3f),
                animationSpec = tween(150),
                label = "dot$i",
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(bg, CircleShape),
            )
        }
    }
}

@Composable
private fun NumpadKey(key: String, onClick: () -> Unit) {
    val isEmpty = key.isEmpty()
    val isSpecial = key == "⌫" || key == "BIO"

    Button(
        onClick = onClick,
        enabled = !isEmpty,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSpecial) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.2f),
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.Transparent,
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
        modifier = Modifier.size(width = 72.dp, height = 64.dp),
    ) {
        when (key) {
            "⌫" -> Icon(PhosphorIcons.Regular.Backspace, null, tint = Color.White, modifier = Modifier.size(22.dp))
            "BIO" -> Icon(PhosphorIcons.Regular.Fingerprint, null, tint = Color.White, modifier = Modifier.size(26.dp))
            "" -> Unit
            else -> Text(key, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
