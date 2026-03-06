package com.dompetku.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// ── DompetKu always uses light scheme — no dark mode ──────────────────────────
private val DompetKuColorScheme = lightColorScheme(
    primary           = GreenPrimary,
    onPrimary         = CardWhite,
    primaryContainer  = GreenLight,
    onPrimaryContainer= GreenDark,
    secondary         = BlueAccent,
    onSecondary       = CardWhite,
    background        = BackgroundApp,
    onBackground      = TextDark,
    surface           = CardWhite,
    onSurface         = TextDark,
    surfaceVariant    = BackgroundApp,
    onSurfaceVariant  = TextMedium,
    error             = RedExpense,
    onError           = CardWhite,
    outline           = DividerColor,
)

// ── Shapes ────────────────────────────────────────────────────────────────────
val DompetKuShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // pills / badges
    small      = RoundedCornerShape(10.dp),  // icon buttons
    medium     = RoundedCornerShape(14.dp),  // input fields
    large      = RoundedCornerShape(18.dp),  // cards
    extraLarge = RoundedCornerShape(24.dp),  // bottom sheet top corners
)

@Composable
fun DompetKuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DompetKuColorScheme,
        typography  = DompetKuTypography,
        shapes      = DompetKuShapes,
        content     = content,
    )
}
