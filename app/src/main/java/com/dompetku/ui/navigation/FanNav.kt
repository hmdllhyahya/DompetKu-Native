package com.dompetku.ui.navigation

import androidx.compose.animation.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.ui.components.DompetKuLogo
import com.dompetku.ui.theme.*
import kotlinx.coroutines.launch

// ── Fan item spec ─────────────────────────────────────────────────────────────
private data class FanItem(
    val id:    NavTab?,
    val dxDp:  Float,
    val dyDp:  Float,
    val color: Color,
    val label: String,
    val isAdd: Boolean = false,
    val isTransfer: Boolean = false,
    val delayMs: Int   = 0,
)

private val FAB_ITEMS = listOf(
    FanItem(id = null,                dxDp =   0f, dyDp =  -96f, color = GreenPrimary, label = "Catat",    isAdd = true,  delayMs = 0),
    FanItem(id = NavTab.Transactions, dxDp = -112f, dyDp = -112f, color = BlueAccent,  label = "Transaksi", delayMs = 30),
    FanItem(id = NavTab.Accounts,     dxDp =   0f, dyDp = -180f, color = GreenPrimary, label = "Akun",     delayMs = 60),
    FanItem(id = NavTab.Analytics,    dxDp = 112f, dyDp = -112f, color = PurpleAccent, label = "Analisis", delayMs = 90),
)

// ─────────────────────────────────────────────────────────────────────────────
// iOS-like spring presets
// ─────────────────────────────────────────────────────────────────────────────
private val IOS_SPRING_OPEN  = spring<Float>(dampingRatio = 0.62f, stiffness = 450f)
private val IOS_SPRING_FAB   = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
private val CLOSE_TWEEN      = { delayMs: Int -> tween<Float>(durationMillis = 180, easing = FastOutSlowInEasing, delayMillis = delayMs) }

@Composable
fun FanNav(
    currentTab:  NavTab,
    onTabChange: (NavTab) -> Unit,
    onQuickAdd:  () -> Unit,
    onTransfer:  () -> Unit = {},
) {
    var open by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ── Pulse: use Animatable so we can STOP it when fan opens ───────────────
    // When open → instantly set to 0, stop looping
    // When closed → loop pulse
    val pulseAlpha = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(open) {
        if (open) {
            pulseAlpha.snapTo(0f)
            pulseScale.snapTo(1f)
        } else {
            // Infinite pulse loop
            while (true) {
                pulseAlpha.snapTo(0.50f)
                pulseScale.snapTo(1f)
                launch { pulseAlpha.animateTo(0f, tween(2200, easing = FastOutSlowInEasing)) }
                pulseScale.animateTo(1.65f, tween(2200, easing = FastOutSlowInEasing))
            }
        }
    }

    // ── FAB icon cross-fade ───────────────────────────────────────────────────
    val fabIconAlpha by animateFloatAsState(
        targetValue   = if (open) 0f else 1f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label         = "fabLogoAlpha",
    )
    val fabLogoScale by animateFloatAsState(
        targetValue   = if (open) 0.35f else 1f,
        animationSpec = IOS_SPRING_FAB,
        label         = "fabLogoScale",
    )
    val fabXAlpha by animateFloatAsState(
        targetValue   = if (open) 1f else 0f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label         = "fabXAlpha",
    )
    val fabXScale by animateFloatAsState(
        targetValue   = if (open) 1f else 0.35f,
        animationSpec = IOS_SPRING_FAB,
        label         = "fabXScale",
    )

    // ── Backdrop alpha — smooth fade in/out ───────────────────────────────────
    val backdropAlpha by animateFloatAsState(
        targetValue   = if (open) 0.72f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label         = "backdropAlpha",
    )
    // Keep backdrop in composition when fading out (removes at 0 to save memory)
    val showBackdrop = open || backdropAlpha > 0.01f

    val fabBrushClosed = Brush.linearGradient(colors = listOf(GreenPrimary, GreenDark))
    val fabBrushOpen   = Brush.linearGradient(colors = listOf(Color(0xFF374151), Color(0xFF1F2937)))

    Box(modifier = Modifier.fillMaxSize()) {

        // ── 1. Backdrop (animated fade) ───────────────────────────────────────
        if (showBackdrop) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(53f)
                    .graphicsLayer { alpha = backdropAlpha }
                    .background(Color.White)
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
                closeDelay = (FAB_ITEMS.size - 1 - idx) * 30,
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
                NavTabButton(
                    label   = "Beranda",
                    icon    = { tint -> Icon(PhosphorIcons.Regular.House, null, tint = tint, modifier = Modifier.size(24.dp)) },
                    active  = currentTab == NavTab.Home,
                    onClick = { onTabChange(NavTab.Home); open = false },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(80.dp))
                NavTabButton(
                    label   = "Profil",
                    icon    = { tint -> Icon(PhosphorIcons.Regular.UserCircle, null, tint = tint, modifier = Modifier.size(24.dp)) },
                    active  = currentTab == NavTab.Profile,
                    onClick = { onTabChange(NavTab.Profile); open = false },
                    modifier = Modifier.weight(1f),
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
            // Pulse ring — graphicsLayer reads Animatable value without recomposition
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .graphicsLayer {
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                        alpha  = pulseAlpha.value
                    }
                    .clip(CircleShape)
                    .background(GreenPrimary),
            )

            // FAB body
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(70.dp)
                    .shadow(elevation = if (open) 12.dp else 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(brush = if (open) fabBrushOpen else fabBrushClosed)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) { open = !open },
            ) {
                // Logo icon (closed state)
                DompetKuLogo(
                    size  = 32.dp,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer {
                        alpha     = fabIconAlpha
                        scaleX    = fabLogoScale
                        scaleY    = fabLogoScale
                    },
                )
                // X icon (open state)
                Icon(
                    imageVector        = PhosphorIcons.Regular.X,
                    contentDescription = "Tutup",
                    tint               = Color.White,
                    modifier           = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            alpha  = fabXAlpha
                            scaleX = fabXScale
                            scaleY = fabXScale
                        },
                )
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

    val posSpec: FiniteAnimationSpec<Float> = if (open)
        spring(dampingRatio = 0.62f, stiffness = 450f)
    else
        tween(durationMillis = 180, easing = FastOutSlowInEasing, delayMillis = closeDelay)

    val alphaSpec: FiniteAnimationSpec<Float> = if (open)
        tween(durationMillis = 200, delayMillis = item.delayMs, easing = FastOutSlowInEasing)
    else
        tween(durationMillis = 160, delayMillis = closeDelay, easing = FastOutSlowInEasing)

    val offsetX by animateFloatAsState(if (open) item.dxDp else 0f, posSpec,   label = "fanX_${item.label}")
    val offsetY by animateFloatAsState(if (open) item.dyDp else 0f, posSpec,   label = "fanY_${item.label}")
    val alpha   by animateFloatAsState(if (open) 1f else 0f,         alphaSpec, label = "fanA_${item.label}")
    val labelAlpha by animateFloatAsState(
        targetValue   = if (open) 1f else 0f,
        animationSpec = if (open)
            tween(durationMillis = 180, delayMillis = item.delayMs + 60, easing = FastOutSlowInEasing)
        else
            tween(durationMillis = 140, delayMillis = closeDelay, easing = FastOutSlowInEasing),
        label = "fanLA_${item.label}",
    )

    val btnSize = if (item.isAdd) 58.dp else 56.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .zIndex(56f)
            .navigationBarsPadding()
            .padding(bottom = 22.dp)
            .graphicsLayer {
                translationX = with(density) { offsetX.dp.toPx() }
                translationY = with(density) { offsetY.dp.toPx() }
                this.alpha   = alpha
            },
    ) {
        // Label pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(8.dp))
                .background(Color.White)
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

        // Circle button — only clickable when open
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(btnSize)
                .shadow(
                    elevation    = if (item.isAdd) 10.dp else 6.dp,
                    shape        = CircleShape,
                    ambientColor = item.color.copy(alpha = 0.3f),
                    spotColor    = item.color.copy(alpha = 0.3f),
                )
                .clip(CircleShape)
                .background(
                    if (item.isAdd)
                        Brush.linearGradient(listOf(GreenPrimary, GreenDark))
                    else
                        Brush.linearGradient(listOf(Color.White, Color.White))
                )
                .then(if (open) Modifier.clickable(onClick = onTap) else Modifier),
        ) {
            when {
                item.isAdd -> Icon(PhosphorIcons.Regular.Plus, "Catat", tint = Color.White, modifier = Modifier.size(22.dp))
                item.id == NavTab.Transactions -> Icon(PhosphorIcons.Regular.ListBullets, "Transaksi", tint = BlueAccent,   modifier = Modifier.size(22.dp))
                item.id == NavTab.Accounts     -> Icon(PhosphorIcons.Regular.Wallet,      "Akun",      tint = GreenPrimary, modifier = Modifier.size(22.dp))
                item.id == NavTab.Analytics    -> Icon(PhosphorIcons.Regular.ChartPie,    "Analisis",  tint = PurpleAccent, modifier = Modifier.size(22.dp))
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
    val tint by animateColorAsState(
        targetValue   = if (active) GreenPrimary else TextLight,
        animationSpec = tween(200),
        label         = "navTint_$label",
    )

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
