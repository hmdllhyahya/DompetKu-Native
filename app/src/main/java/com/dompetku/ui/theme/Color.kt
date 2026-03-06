package com.dompetku.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary Brand ─────────────────────────────────────────────────────────────
val GreenPrimary  = Color(0xFF2DAB7F)
val GreenDark     = Color(0xFF1D8A63)
val GreenLight    = Color(0xFFE8F7F1)
val GreenMedium   = Color(0xFFB6E4D0)

// ── Background & Surface ──────────────────────────────────────────────────────
val BackgroundApp = Color(0xFFF0F3F7)
val CardWhite     = Color(0xFFFFFFFF)
val SurfaceCard   = Color(0xFFFFFFFF)
val PageBg        = BackgroundApp

// ── Text ──────────────────────────────────────────────────────────────────────
val TextDark      = Color(0xFF111827)
val TextMedium    = Color(0xFF6B7280)
val TextLight     = Color(0xFF9CA3AF)

// ── Divider ───────────────────────────────────────────────────────────────────
val DividerColor  = Color(0xFFE5E7EB)

// ── Semantic ──────────────────────────────────────────────────────────────────
val RedExpense    = Color(0xFFEF4444)
val RedLight      = Color(0xFFFEF2F2)
val OrangeWarn    = Color(0xFFF97316)

// ── Analytics / Category accent ───────────────────────────────────────────────
val BlueAccent    = Color(0xFF3B82F6)
val PurpleAccent  = Color(0xFF8B5CF6)
val CyanAccent    = Color(0xFF06B6D4)
val PinkAccent    = Color(0xFFEC4899)
val YellowAccent  = Color(0xFFEAB308)
val IndigoAccent  = Color(0xFF6366F1)
val TealAccent    = Color(0xFF14B8A6)
val RosePink      = Color(0xFFF43F5E)

// ── Dark cards (Analytics / Forecast) ─────────────────────────────────────────
val DarkForest    = Color(0xFF0F3D2B)   // Financial forecast card
val DarkCharcoal  = Color(0xFF111827)   // Lifestyle analysis card

// ── Brand gradients (start / end pairs) ───────────────────────────────────────
// Used in AccountCard; keep in sync with BRAND_MAP from JSX
object BrandGradients {
    val BCA           = Pair(Color(0xFF0053A0), Color(0xFF2196F3))
    val BRI           = Pair(Color(0xFF003F88), Color(0xFF1976D2))
    val BNI           = Pair(Color(0xFFFF6B00), Color(0xFFFFA040))
    val Mandiri       = Pair(Color(0xFF003399), Color(0xFFFFD700))
    val BSI           = Pair(Color(0xFF005C35), Color(0xFF34C38F))
    val CIMB          = Pair(Color(0xFFAB1519), Color(0xFFFF5722))
    val Permata       = Pair(Color(0xFF005B9F), Color(0xFF29B6F6))
    val Danamon       = Pair(Color(0xFFE00000), Color(0xFFFF6B6B))
    val Panin         = Pair(Color(0xFF1B3FA0), Color(0xFF4FC3F7))
    val OCBC          = Pair(Color(0xFFC8102E), Color(0xFFFF8A65))
    val GoPay         = Pair(Color(0xFF00AED6), Color(0xFF26C6DA))
    val OVO           = Pair(Color(0xFF4B2D8C), Color(0xFF9C6FE4))
    val DANA          = Pair(Color(0xFF108BE8), Color(0xFF64B5F6))
    val ShopeePay     = Pair(Color(0xFFEE4D2D), Color(0xFFFF8C6B))
    val Jenius        = Pair(Color(0xFF0066CC), Color(0xFF4DA6FF))
    val Jago          = Pair(Color(0xFF00C4A7), Color(0xFF5EDDCC))
    val JagoSyariah   = Pair(Color(0xFF00875A), Color(0xFF00C4A7))
    val LinkAja       = Pair(Color(0xFFE82529), Color(0xFFFF6B6E))
    val Flip          = Pair(Color(0xFF00A651), Color(0xFF4DC87F))
    val SeaBank       = Pair(Color(0xFF1A1A2E), Color(0xFF16213E))
    val Blu           = Pair(Color(0xFF0099FF), Color(0xFF66CCFF))
    val Maybank       = Pair(Color(0xFFFFD700), Color(0xFFFFB300))
    val BTN           = Pair(Color(0xFF003580), Color(0xFF0066CC))
    val Bukopin       = Pair(Color(0xFF003087), Color(0xFF0052CC))
    val Muamalat      = Pair(Color(0xFF005B9F), Color(0xFF0288D1))
    val BJB           = Pair(Color(0xFF003366), Color(0xFF0055A5))
    val BankJatim     = Pair(Color(0xFF007A3D), Color(0xFF00AA55))
    val BankNTT       = Pair(Color(0xFF8B0000), Color(0xFFCC0000))
    val Standard      = Pair(Color(0xFF0B3D91), Color(0xFF1565C0))
    val HSBC          = Pair(Color(0xFFDB0011), Color(0xFFFF5252))
    val Citibank      = Pair(Color(0xFF003B70), Color(0xFF0066B3))
    val Default       = Pair(GreenPrimary,  GreenDark)

}
