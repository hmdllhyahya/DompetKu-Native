package com.dompetku.ui.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.ui.components.DompetKuLogo
import com.dompetku.ui.theme.*
import kotlinx.coroutines.delay

// ── Fan item spec (mirrors ARCH_ITEMS from JSX) ───────────────────────────────
private data class FanItem(
    val id:    NavTab?,         // null → quickAdd action
    val dxDp:  Float,           // horizontal offset from FAB center (dp)
    val dyDp:  Float,           // vertical offset from FAB center (dp, negative = up)
    val color: Color,
    val label: String,
    val isAdd: Boolean = false, // the "Catat+" primary action button
    val isTransfer: Boolean = false,
    val delayMs: Int   = 0,
)

private val FAB_ITEMS = listOf(
    // Ordered by open delay (ascending) for stagger — MUST match JSX exactly
    FanItem(id = null,                dxDp =   0f, dyDp =  -96f, color = GreenPrimary, label = "Catat",    isAdd = true,  delayMs = 0),
    FanItem(id = NavTab.Transactions, dxDp = -112f, dyDp = -112f, color = BlueAccent,  label = "Transaksi", delayMs = 35),
    FanItem(id = NavTab.Accounts,     dxDp =   0f, dyDp = -180f, color = GreenPrimary, label = "Akun",     delayMs = 70),
    FanItem(id = NavTab.Analytics,    dxDp = 112f, dyDp = -112f, color = PurpleAccent, label = "Analisis", delayMs = 105),
)

// ── FanNav composable ─────────────────────────────────────────────────────────
/**
 * Renders:
 *  1. Bottom navigation bar (Beranda | [gap] | Profil)
 *  2. Floating FAB centered above the gap (pulse animation when closed)
 *  3. Backdrop overlay (white 70% + blur) when fan is open
 *  4. 4 fan action buttons with spring stagger animation
 *
 * @param currentTab    currently active tab
 * @param onTabChange   called when a nav tab is selected
 * @param onQuickAdd    called when the "Catat" FAB sub-button is tapped
 */
@Composable
fun FanNav(
    currentTab:  NavTab,
    onTabChange: (NavTab) -> Unit,
    onQuickAdd:  () -> Unit,
    onTransfer:  () -> Unit = {},
) {
    var open by remember { mutableStateOf(false) }

    // ── Pulse animation for FAB — only runs when fan is CLOSED ─────────────────
    // Using key(open) so the animation resets cleanly when fan opens/closes
    val infiniteTransition = rememberInfiniteTransition(label = "fabPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue   = if (open) 0f else 0.50f,
        targetValue    = 0f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseAlpha",
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = if (open) 1f else 1f,
        targetValue   = if (open) 1f else 1.65f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseScale",
    )

    // ── FAB icon cross-fade ───────────────────────────────────────────────────
    // iOS-like spring: high stiffness, low damping = snappy with slight overshoot
    val fabIconAlpha by animateFloatAsState(
        targetValue   = if (open) 0f else 1f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label         = "fabLogoAlpha",
    )
    val fabLogoScale by animateFloatAsState(
        targetValue   = if (open) 0.4f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label         = "fabLogoScale",
    )
    val fabXAlpha by animateFloatAsState(
        targetValue   = if (open) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label         = "fabXAlpha",
    )
    val fabXScale by animateFloatAsState(
        targetValue   = if (open) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label         = "fabXScale",
    )

    // ── FAB background color ──────────────────────────────────────────────────
    val fabBrushClosed = Brush.linearGradient(colors = listOf(GreenPrimary, GreenDark))
    val fabBrushOpen   = Brush.linearGradient(colors = listOf(Color(0xFF374151), Color(0xFF1F2937)))

    Box(modifier = Modifier.fillMaxSize()) {

        // ── 1. Backdrop ───────────────────────────────────────────────────────
        if (open) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(53f)
                    .background(Color.White.copy(alpha = 0.72f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) { open = false },
            )
        }

        // ── 2. Fan items ──────────────────────────────────────────────────────
        FAB_ITEMS.forEachIndexed { idx, item ->
            FanItemButton(
                item       = item,
                open       = open,
                closeDelay = (FAB_ITEMS.size - 1 - idx) * 35,
                onTap      = {
                    open = false
                    when {
                        item.isAdd      -> onQuickAdd()
                        item.isTransfer -> onTransfer()
                        item.id != null -> onTabChange(item.id)
                    }
                },
            )
        }

        // ── 3. Bottom nav bar ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(55f)
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding(),
        ) {
            // Top border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF1F5F9))
                    .align(Alignment.TopCenter),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Beranda
                NavTabButton(
                    label     = "Beranda",
                    icon      = { tint ->
                        Icon(PhosphorIcons.Regular.House, contentDescription = "Beranda", tint = tint, modifier = Modifier.size(24.dp))
                    },
                    active    = currentTab == NavTab.Home,
                    onClick   = { onTabChange(NavTab.Home); open = false },
                    modifier  = Modifier.weight(1f),
                )

                // Center spacer (FAB lives here)
                Spacer(modifier = Modifier.width(80.dp))

                // Profil
                NavTabButton(
                    label  = "Profil",
                    icon   = { tint ->
                        Icon(PhosphorIcons.Regular.UserCircle, contentDescription = "Profil", tint = tint, modifier = Modifier.size(24.dp))
                    },
                    active    = currentTab == NavTab.Profile,
                    onClick   = { onTabChange(NavTab.Profile); open = false },
                    modifier  = Modifier.weight(1f),
                )
            }
        }

        // ── 4. FAB ────────────────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(57f)
                .navigationBarsPadding()
                .padding(bottom = 22.dp),
        ) {
            // Pulse ring (only when closed)
            if (!open) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha  = pulseAlpha
                        }
                        .clip(CircleShape)
                        .background(GreenPrimary),
                )
            }

            // FAB body
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(70.dp)
                    .shadow(elevation = if (open) 12.dp else 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(brush = if (open) fabBrushOpen else fabBrushClosed)
                    .then(
                        // White border via outer padding trick
                        Modifier.padding(0.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) { open = !open },
            ) {
                // White border ring
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                )

                // Logo icon (shown when closed)
                DompetKuLogo(
                    size  = 32.dp,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer {
                        alpha  = fabIconAlpha
                        scaleX = fabLogoScale
                        scaleY = fabLogoScale
                        rotationZ = if (open) 45f else 0f
                    },
                )

                // X icon (shown when open)
                Icon(
                    imageVector         = PhosphorIcons.Regular.X,
                    contentDescription  = "Tutup",
                    tint                = Color.White,
                    modifier            = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            alpha  = fabXAlpha
                            scaleX = fabXScale
                            scaleY = fabXScale
                        },
                )
            }

            // White border overlay ring (3.5dp)
            Box(
                modifier = Modifier
                    .size(70.dp + 7.dp) // 70 + 2×3.5 border
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .padding(3.5.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent),
            ) {
                // This creates the white ring effect via a Box with border
            }
        }
    }
}

// ── Fan item button ────────────────────────────────────────────────────────────
@Composable
private fun BoxScope.FanItemButton(
    item:       FanItem,
    open:       Boolean,
    closeDelay: Int,
    onTap:      () -> Unit,
) {
    val density = LocalDensity.current

    // Open: iOS spring (snappy, slight overshoot like UIKit spring)
    // Close: fast ease-out with stagger
    val animSpec: FiniteAnimationSpec<Float> = if (open)
        spring(dampingRatio = 0.58f, stiffness = 380f)
    else
        tween(durationMillis = 200, easing = FastOutSlowInEasing, delayMillis = closeDelay)

    val targetOffsetX = if (open) item.dxDp else 0f
    val targetOffsetY = if (open) item.dyDp else 0f
    val targetAlpha   = if (open) 1f else 0f

    val offsetX by animateFloatAsState(targetOffsetX, animSpec, label = "fanX_${item.label}")
    val offsetY by animateFloatAsState(targetOffsetY, animSpec, label = "fanY_${item.label}")
    val alpha   by animateFloatAsState(
        targetValue   = targetAlpha,
        animationSpec = if (open)
            tween(durationMillis = 220, delayMillis = item.delayMs)
        else
            tween(durationMillis = 180, delayMillis = closeDelay),
        label = "fanAlpha_${item.label}",
    )

    val btnSize = if (item.isAdd) 58.dp else 56.dp

    // Label pill alpha: slightly delayed behind the button
    val labelAlpha by animateFloatAsState(
        targetValue   = targetAlpha,
        animationSpec = if (open)
            tween(durationMillis = 200, delayMillis = item.delayMs + 80)
        else
            tween(durationMillis = 150, delayMillis = closeDelay),
        label = "fanLabelAlpha_${item.label}",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .zIndex(56f)
            .navigationBarsPadding()
            .padding(bottom = 22.dp)               // same bottom as FAB
            .offset(
                x = with(density) { offsetX.dp },
                y = with(density) { offsetY.dp },
            )
            .graphicsLayer { this.alpha = alpha }
            .then(if (!open) Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = {},
            ) else Modifier),
    ) {
        // Label pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(8.dp))
                .graphicsLayer { this.alpha = labelAlpha }
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text       = item.label,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = TextDark,
            )
        }

        // Circle button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(btnSize)
                .shadow(
                    elevation = if (item.isAdd) 10.dp else 6.dp,
                    shape     = CircleShape,
                    ambientColor  = item.color.copy(alpha = 0.33f),
                    spotColor     = item.color.copy(alpha = 0.33f),
                )
                .clip(CircleShape)
                .background(
                    if (item.isAdd)
                        Brush.linearGradient(listOf(GreenPrimary, GreenDark))
                    else
                        Brush.linearGradient(listOf(Color.White, Color.White))
                )
                .clickable(onClick = onTap),
        ) {
            when {
                item.isAdd  -> Icon(
                    PhosphorIcons.Regular.Plus,
                    contentDescription = "Catat",
                    tint   = Color.White,
                    modifier = Modifier.size(22.dp),
                )
                item.id == NavTab.Transactions -> Icon(
                    PhosphorIcons.Regular.ListBullets,
                    contentDescription = "Transaksi",
                    tint   = BlueAccent,
                    modifier = Modifier.size(22.dp),
                )
                item.id == NavTab.Accounts -> Icon(
                    PhosphorIcons.Regular.Wallet,
                    contentDescription = "Akun",
                    tint   = GreenPrimary,
                    modifier = Modifier.size(22.dp),
                )
                item.id == NavTab.Analytics -> Icon(
                    PhosphorIcons.Regular.ChartPie,
                    contentDescription = "Analisis",
                    tint   = PurpleAccent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

// ── Bottom nav tab button ─────────────────────────────────────────────────────
@Composable
private fun NavTabButton(
    label:    String,
    icon:     @Composable (tint: Color) -> Unit,
    active:   Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (active) GreenPrimary else TextLight

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            )
            .padding(vertical = 4.dp),
    ) {
        // Active indicator strip (3dp, sits above icon)
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(if (active) GreenPrimary else Color.Transparent),
        )

        icon(tint)

        Text(
            text       = label,
            fontSize   = 11.sp,
            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
            color      = tint,
        )
    }
}
