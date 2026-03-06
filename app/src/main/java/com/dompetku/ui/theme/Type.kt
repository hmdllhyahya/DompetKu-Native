package com.dompetku.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dompetku.R


// ── Poppins font family ────────────────────────────────────────────────────────
// Place font files in res/font/:
//   poppins_regular.ttf, poppins_medium.ttf, poppins_semibold.ttf,
//   poppins_bold.ttf, poppins_extrabold.ttf
// Download from: https://fonts.google.com/specimen/Poppins
val PoppinsFamily = FontFamily(
    Font(R.font.poppins_regular,   FontWeight.Normal),
    Font(R.font.poppins_medium,    FontWeight.Medium),
    Font(R.font.poppins_semibold,  FontWeight.SemiBold),
    Font(R.font.poppins_bold,      FontWeight.Bold),
    Font(R.font.poppins_extrabold, FontWeight.ExtraBold),
)

// ── Material3 Typography override ─────────────────────────────────────────────
val DompetKuTypography = Typography(
    // Display
    displayLarge = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp, lineHeight = 40.sp
    ),
    displayMedium = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 36.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 32.sp
    ),

    // Headline
    headlineLarge = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 30.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 26.sp
    ),

    // Title
    titleLarge = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 20.sp
    ),

    // Body
    bodyLarge = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.Normal,
        fontSize = 11.sp, lineHeight = 16.sp
    ),

    // Label
    labelLarge = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PoppinsFamily, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 14.sp
    ),
)
