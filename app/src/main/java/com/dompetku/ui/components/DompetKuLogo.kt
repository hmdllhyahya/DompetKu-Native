package com.dompetku.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * DompetKu logo — ported exactly from JSX SVG (viewBox 0 0 32 32):
 *
 *   <rect x="2" y="9" width="21" height="15" rx="3.5" stroke />
 *   <path d="M15 14h8a2 2 0 010 4h-8" />          ← card slot
 *   <circle cx="19.5" cy="16" r="1.4" />           ← card dot
 *   <path d="M24 21.5 C27 19.5 29.5 17 29 14" />  ← wave 1
 *   <path d="M24.5 18 C27 16 29 13.5 28.5 11" />  ← wave 2
 *   <path d="M24 14.5 C26 12.5 27 10 26 8.5" />   ← wave 3
 */
@Composable
fun DompetKuLogo(
    size: Dp    = 28.dp,
    color: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val s = this.size.width  // actual px size
        val scale = s / 32f      // 32 = SVG viewBox width

        val stroke2  = Stroke(width = 2f   * scale, cap = StrokeCap.Round)
        val stroke19 = Stroke(width = 1.9f * scale, cap = StrokeCap.Round)
        val stroke18 = Stroke(width = 1.8f * scale, cap = StrokeCap.Round)
        val stroke16 = Stroke(width = 1.6f * scale, cap = StrokeCap.Round)

        // ── Rect: x=2 y=9 w=21 h=15 rx=3.5 ──────────────────────────────────
        val rectPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left   = 2f * scale,
                    top    = 9f * scale,
                    right  = (2f + 21f) * scale,
                    bottom = (9f + 15f) * scale,
                    radiusX = 3.5f * scale,
                    radiusY = 3.5f * scale,
                )
            )
        }
        drawPath(rectPath, color, style = stroke2)

        // ── Card slot path: M15 14 h8 a2 2 0 0 1 0 4 h-8 ────────────────────
        val slotPath = Path().apply {
            moveTo(15f * scale, 14f * scale)
            lineTo(23f * scale, 14f * scale)
            // arc: a2 2 0 0 1 0 4 → small arc going down-right
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left   = 21f * scale,
                    top    = 14f * scale,
                    right  = 25f * scale,
                    bottom = 18f * scale,
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            lineTo(15f * scale, 18f * scale)
        }
        drawPath(slotPath, color, style = stroke19)

        // ── Circle: cx=19.5 cy=16 r=1.4 ──────────────────────────────────────
        drawCircle(
            color  = color,
            radius = 1.4f * scale,
            center = Offset(19.5f * scale, 16f * scale),
        )

        // ── Wave 1: M24 21.5 C27 19.5 29.5 17 29 14 ─────────────────────────
        val wave1 = Path().apply {
            moveTo(24f * scale,   21.5f * scale)
            cubicTo(
                27f * scale,   19.5f * scale,
                29.5f * scale, 17f * scale,
                29f * scale,   14f * scale,
            )
        }
        drawPath(wave1, color, style = stroke2)

        // ── Wave 2: M24.5 18 C27 16 29 13.5 28.5 11 ─────────────────────────
        val wave2 = Path().apply {
            moveTo(24.5f * scale, 18f * scale)
            cubicTo(
                27f * scale,   16f * scale,
                29f * scale,   13.5f * scale,
                28.5f * scale, 11f * scale,
            )
        }
        drawPath(wave2, color, style = stroke18)

        // ── Wave 3: M24 14.5 C26 12.5 27 10 26 8.5 ──────────────────────────
        val wave3 = Path().apply {
            moveTo(24f * scale,  14.5f * scale)
            cubicTo(
                26f * scale,  12.5f * scale,
                27f * scale,  10f * scale,
                26f * scale,  8.5f * scale,
            )
        }
        drawPath(wave3, color, style = stroke16)
    }
}
