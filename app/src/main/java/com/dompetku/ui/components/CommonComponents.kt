package com.dompetku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.domain.model.TransactionType
import com.dompetku.ui.theme.*
import com.dompetku.util.CurrencyFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Category config (mirrors JSX CAT_COLORS / CAT_BG / CAT_ICON) ──────────────

data class CatConfig(
    val icon:    ImageVector,
    val color:   Color,
    val bgColor: Color,
)

val CATEGORY_CONFIGS: Map<String, CatConfig> = mapOf(
    "Makan & Minum"       to CatConfig(PhosphorIcons.Regular.Coffee,          Color(0xFFEF4444), Color(0xFFFEE2E2)),
    "Belanja Harian"      to CatConfig(PhosphorIcons.Regular.ShoppingCart,    Color(0xFFF97316), Color(0xFFFFEDD5)),
    "Belanja Online"      to CatConfig(PhosphorIcons.Regular.ShoppingBag,     Color(0xFFEC4899), Color(0xFFFCE7F3)),
    "Transportasi"        to CatConfig(PhosphorIcons.Regular.Car,             Color(0xFF3B82F6), Color(0xFFDBEAFE)),
    "Hiburan"             to CatConfig(PhosphorIcons.Regular.Television,      Color(0xFFF59E0B), Color(0xFFFEF3C7)),
    "Tagihan"             to CatConfig(PhosphorIcons.Regular.Lightning,       Color(0xFF8B5CF6), Color(0xFFEDE9FE)),
    "Kesehatan"           to CatConfig(PhosphorIcons.Regular.Heart,           Color(0xFF10B981), Color(0xFFD1FAE5)),
    "Pendidikan"          to CatConfig(PhosphorIcons.Regular.BookOpen,        Color(0xFF06B6D4), Color(0xFFCFFAFE)),
    "Tempat Tinggal"      to CatConfig(PhosphorIcons.Regular.House,           Color(0xFF6366F1), Color(0xFFE0E7FF)),
    "Perawatan"           to CatConfig(PhosphorIcons.Regular.Scissors,        Color(0xFFF472B6), Color(0xFFFCE7F3)),
    "Gaji"                to CatConfig(PhosphorIcons.Regular.Briefcase,       Color(0xFF22C55E), Color(0xFFDCFCE7)),
    "Freelance"           to CatConfig(PhosphorIcons.Regular.Laptop,          Color(0xFF84CC16), Color(0xFFECFCCB)),
    "Hadiah"              to CatConfig(PhosphorIcons.Regular.Gift,            Color(0xFFA78BFA), Color(0xFFEDE9FE)),
    "Investasi"           to CatConfig(PhosphorIcons.Regular.TrendUp,         Color(0xFF34D399), Color(0xFFD1FAE5)),
    "Penyesuaian Saldo"   to CatConfig(PhosphorIcons.Regular.Sliders,        Color(0xFF9CA3AF), Color(0xFFF3F4F6)),
    "Transfer"            to CatConfig(PhosphorIcons.Regular.ArrowsLeftRight,  Color(0xFF6366F1), Color(0xFFE0E7FF)),
    "Lainnya"             to CatConfig(PhosphorIcons.Regular.DotsThree,       Color(0xFF9CA3AF), Color(0xFFF3F4F6)),
)

private val DEFAULT_CAT = CatConfig(PhosphorIcons.Regular.DotsThree, Color(0xFF9CA3AF), Color(0xFFF3F4F6))

fun catConfig(category: String) = CATEGORY_CONFIGS[category] ?: DEFAULT_CAT

// ── CategoryBubble ─────────────────────────────────────────────────────────────
@Composable
fun CategoryBubble(
    category: String,
    size:     Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val cfg = catConfig(category)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(cfg.bgColor),
    ) {
        Icon(
            imageVector        = cfg.icon,
            contentDescription = category,
            tint               = cfg.color,
            modifier           = Modifier.size(size * 0.44f),
        )
    }
}

// ── GradientCard ──────────────────────────────────────────────────────────────
@Composable
fun GradientCard(
    startColor: Color,
    endColor:   Color,
    modifier:   Modifier = Modifier,
    content:    @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(startColor, endColor)))
            .padding(20.dp),
        content = content,
    )
}

// ── WhiteCard ─────────────────────────────────────────────────────────────────
@Composable
fun WhiteCard(
    modifier: Modifier = Modifier,
    padding:  Dp = 16.dp,
    content:  @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .padding(padding),
        content = content,
    )
}

// ── AppHeader ─────────────────────────────────────────────────────────────────
@Composable
fun AppHeader(
    title:        String,
    showDate:     Boolean = true,
    showSearch:   Boolean = false,
    showLock:     Boolean = false,
    onSearchClick: () -> Unit = {},
    onLockClick:   () -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val today = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale("id", "ID")))
    }
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        color = CardWhite,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text       = title,
                fontSize   = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = TextDark,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                if (showDate) {
                    Text(
                        text       = today,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextLight,
                        modifier   = Modifier
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                if (showSearch) {
                    IconButton(
                        onClick  = onSearchClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(9.dp)),
                    ) {
                        Icon(PhosphorIcons.Regular.MagnifyingGlass, null, tint = TextDark, modifier = Modifier.size(15.dp))
                    }
                }
                if (showLock) {
                    IconButton(
                        onClick  = onLockClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp)),
                    ) {
                        Icon(PhosphorIcons.Regular.Lock, null, tint = TextDark, modifier = Modifier.size(15.dp))
                    }
                }
                trailingContent?.invoke()
            }
        }
    }
}

// ── FilterChip ────────────────────────────────────────────────────────────────
@Composable
fun FilterChip(
    label:    String,
    active:   Boolean,
    onClick:  () -> Unit,
    activeColor: Color = GreenPrimary,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (active) activeColor else CardWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (active) Color.White else TextMedium,
        )
    }
}

// ── Toggle switch (exact port of JSX Tog) ─────────────────────────────────────
@Composable
fun Toggle(
    checked:  Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 24.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (checked) GreenPrimary else Color(0xFFD1D5DB))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onToggle,
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .size(18.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color.White)
                .shadow(1.dp, CircleShape),
        )
    }
}

// ── TransactionRow (reused in Home + TransactionList + AccountDetail) ──────────
@Composable
fun TransactionRow(
    note:          String,
    category:      String,
    accountName:   String,
    date:          String,
    time:          String,
    type:          TransactionType,
    amount:        Long,
    toAccountName: String? = null,   // for transfer: destination account
    autoDetected:  Boolean = false,
    hidden:        Boolean = false,
    isLast:        Boolean = false,
    onClick:       () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .then(
                if (!isLast) Modifier.padding(bottom = 0.dp) else Modifier
            ),
    ) {
        CategoryBubble(category = category, size = 40.dp)

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = note.ifEmpty { if (type == TransactionType.transfer) "Transfer" else "Transaksi" },
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.padding(top = 2.dp),
            ) {
                // For transfers: show "From → To", otherwise just account name
                val pillLabel = if (type == TransactionType.transfer && toAccountName != null)
                    "$accountName → $toAccountName" else accountName
                Text(
                    text     = pillLabel,
                    fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextMedium,
                    modifier = Modifier
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(99.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                if (time.isNotEmpty()) {
                    Text(text = time, fontSize = 9.sp, color = TextLight)
                }
                if (autoDetected) {
                    Text(
                        text     = "auto",
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GreenPrimary,
                        modifier = Modifier
                            .background(GreenLight, RoundedCornerShape(99.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            val amtColor = when (type) {
                TransactionType.income   -> GreenPrimary
                TransactionType.expense  -> RedExpense
                TransactionType.transfer -> Color(0xFF6366F1)
            }
            val prefix = when (type) {
                TransactionType.income   -> "+"
                TransactionType.expense  -> "-"
                TransactionType.transfer -> "↔"
            }
            Text(
                text       = if (hidden) "••••" else "$prefix ${CurrencyFormatter.format(amount)}",
                fontSize   = 13.sp, fontWeight = FontWeight.ExtraBold, color = amtColor,
                textAlign  = TextAlign.End,
            )
        }
    }

    if (!isLast) {
        HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
    }
}

// ── DateHeader for grouped list ───────────────────────────────────────────────
@Composable
fun DateGroupHeader(
    dateStr:  String,
    income:   Long,
    expense:  Long,
    modifier: Modifier = Modifier,
) {
    val label = remember(dateStr) {
        runCatching {
            val d = LocalDate.parse(dateStr)
            val today = LocalDate.now()
            when (d) {
                today           -> "Hari Ini"
                today.minusDays(1) -> "Kemarin"
                else -> d.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale("id", "ID")))
                    .replaceFirstChar { it.uppercase() }
            }
        }.getOrDefault(dateStr)
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp),
    ) {
        Text(
            text       = label.uppercase(),
            fontSize   = 10.sp, fontWeight = FontWeight.Bold,
            color      = TextLight, letterSpacing = 1.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (income > 0) {
                Text(
                    text     = "+${CurrencyFormatter.compact(income)}",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenPrimary,
                    modifier = Modifier
                        .background(GreenLight, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            if (expense > 0) {
                Text(
                    text     = "-${CurrencyFormatter.compact(expense)}",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RedExpense,
                    modifier = Modifier
                        .background(RedLight, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
    }
}

// ── Section row used in Profile (SRow port) ───────────────────────────────────
@Composable
fun SectionRow(
    icon:         ImageVector,
    iconBg:       Color    = GreenLight,
    iconTint:     Color    = GreenPrimary,
    title:        String,
    subtitle:     String?  = null,
    isDanger:     Boolean  = false,
    isLast:       Boolean  = false,
    onClick:      (() -> Unit)? = null,
    rightContent: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDanger) RedLight else iconBg),
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = if (isDanger) RedExpense else iconTint,
                modifier           = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                fontSize   = 13.sp, fontWeight = FontWeight.SemiBold,
                color      = if (isDanger) RedExpense else TextDark,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(subtitle, fontSize = 11.sp, color = TextLight, modifier = Modifier.padding(top = 2.dp))
            }
        }
        rightContent()
    }
}

// ── GreenButton (BtnG port) ───────────────────────────────────────────────────
@Composable
fun GreenButton(
    text:     String,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
    enabled:  Boolean  = true,
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = GreenPrimary,
            contentColor           = Color.White,
            disabledContainerColor = Color(0xFFD1D5DB),
            disabledContentColor   = Color.White,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

// ── SettingsLabel (section header in Profile) ─────────────────────────────────
@Composable
fun SectionLabel(text: String) {
    Text(
        text     = text.uppercase(),
        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp),
    )
}
