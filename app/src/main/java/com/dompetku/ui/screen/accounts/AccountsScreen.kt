package com.dompetku.ui.screen.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.AccountType
import com.dompetku.ui.components.*
import com.dompetku.ui.theme.*
import com.dompetku.util.CurrencyFormatter

@Composable
fun AccountsScreen(
    onNavigateToDetail: (accountId: String) -> Unit = {},
    onOpenTransfer:     () -> Unit                  = {},
    viewModel:          AccountsViewModel = hiltViewModel(),
) {
    val accounts     by viewModel.accounts.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val hidden       by viewModel.hideBalance.collectAsStateWithLifecycle()

    var editMode     by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editTarget   by remember { mutableStateOf<Account?>(null) }
    var deleteTarget by remember { mutableStateOf<Account?>(null) }
    var showCurrency by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
    ) {
        AppHeader(
            title    = "Akun",
            showDate = false,
            trailingContent = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (hidden) GreenLight else Color(0xFFF1F5F9))
                        .clickable { viewModel.toggleHideBalance() },
                ) {
                    Icon(
                        imageVector = if (hidden) PhosphorIcons.Regular.EyeSlash else PhosphorIcons.Regular.Eye,
                        contentDescription = null,
                        tint     = if (hidden) GreenPrimary else TextLight,
                        modifier = Modifier.size(15.dp),
                    )
                }
            },
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {

            // ── Total balance card ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(GreenPrimary, GreenDark)))
                    .padding(horizontal = 22.dp, vertical = 20.dp)
                    .padding(bottom = 14.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text("Total Semua Akun", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(if (hidden) "Rp ••••••" else CurrencyFormatter.format(totalBalance), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { showCurrency = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(PhosphorIcons.Regular.CurrencyCircleDollar, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Text("Kurs", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Header row ────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            ) {
                Text("Daftar Akun", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallChipButton(if (editMode) "Selesai" else "Edit", PhosphorIcons.Regular.PencilSimple, onClick = { editMode = !editMode })
                    SmallChipButton("Transfer", PhosphorIcons.Regular.ArrowsLeftRight, onClick = onOpenTransfer, enabled = accounts.size >= 2, color = GreenPrimary)
                    SmallChipButton("Tambah", PhosphorIcons.Regular.Plus, onClick = { showAddSheet = true }, color = GreenPrimary)
                }
            }

            if (accounts.isEmpty()) {
                WhiteCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Belum ada akun. Tambah sekarang!", fontSize = 13.sp, color = TextLight,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp))
                }
            }

            // ── 2-column grid ─────────────────────────────────────────────────
            accounts.chunked(2).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                ) {
                    row.forEachIndexed { _, acc ->
                        AccountCard(
                            account      = acc,
                            accIndex     = accounts.indexOf(acc),
                            editMode     = editMode,
                            hidden       = hidden,
                            modifier     = Modifier.weight(1f),
                            onTap        = { if (!editMode) onNavigateToDetail(acc.id) },
                            onLongPress  = { editMode = true },
                            onEdit       = { editTarget = acc },
                            onDelete     = { deleteTarget = acc },
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    if (showAddSheet) {
        AccountFormSheet(
            onDismiss = { showAddSheet = false },
            onSave    = { acc, _ -> viewModel.addAccount(acc) },
        )
    }

    editTarget?.let { target ->
        AccountFormSheet(
            initial   = target,
            onDismiss = { editTarget = null },
            onSave    = { acc, adj -> viewModel.updateAccount(acc, adj) },
        )
    }

    deleteTarget?.let { target ->
        val txnCount = remember(target) {
            viewModel.transactions.value.count { it.accountId == target.id }
        }
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title  = { Text("Hapus Akun?", fontWeight = FontWeight.ExtraBold) },
            text   = { Text("Akun \"${target.name}\" dan $txnCount transaksi terkait akan dihapus permanen.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteAccount(target.id); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpense)) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } },
        )
    }

    if (showCurrency) {
        CurrencySheet(totalBalance = totalBalance, onDismiss = { showCurrency = false })
    }
}

// ── Account card ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountCard(
    account:     Account,
    accIndex:    Int,
    editMode:    Boolean,
    hidden:      Boolean = false,
    modifier:    Modifier = Modifier,
    onTap:       () -> Unit,
    onLongPress: () -> Unit = {},
    onEdit:      () -> Unit,
    onDelete:    () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
    val jiggleAngle by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue  =  1.5f,
        animationSpec = InfiniteRepeatableSpec(
            animation  = tween(100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "jiggleAngle",
    )
    val rotation = if (editMode) jiggleAngle + (if (accIndex % 2 == 0) -0.5f else 0.5f) else 0f

    Column(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .shadow(elevation = if (rotation != 0f) 6.dp else 2.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(Brush.linearGradient(listOf(Color(account.gradientStart.toInt()), Color(account.gradientEnd.toInt()))))
                .padding(14.dp),
        ) {
            // Decorative circle
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .offset(x = 20.dp, y = (-18).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .align(Alignment.TopEnd),
            )
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    Text(account.name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (account.balance < 0)
                        Icon(PhosphorIcons.Regular.Warning, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(11.dp))
                    Icon(
                        imageVector = when (account.type) {
                            AccountType.ewallet -> PhosphorIcons.Regular.DeviceMobile
                            AccountType.emoney  -> PhosphorIcons.Regular.WifiHigh
                            AccountType.cash    -> PhosphorIcons.Regular.Wallet
                            AccountType.savings -> PhosphorIcons.Regular.PiggyBank
                            else                -> PhosphorIcons.Regular.CreditCard
                        },
                        contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(15.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (hidden) "••••" else CurrencyFormatter.compact(account.balance),
                    fontSize = 16.sp, fontWeight = FontWeight.Black,
                    color = if (account.balance < 0) Color(0xFFFCA5A5) else Color.White,
                )
                Text(
                    if (!account.last4.isNullOrEmpty()) "•••• ${account.last4}" else account.type.name,
                    fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), letterSpacing = 2.sp,
                )
            }
        }

        // Edit / Delete row
        Row(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onEdit() }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(PhosphorIcons.Regular.PencilSimple, null, tint = GreenPrimary, modifier = Modifier.size(13.dp))
                    Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                }
            }
            VerticalDivider(color = Color(0xFFF1F5F9))
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onDelete() }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(PhosphorIcons.Regular.Trash, null, tint = RedExpense, modifier = Modifier.size(13.dp))
                    Text("Hapus", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RedExpense)
                }
            }
        }
    }
}

@Composable
private fun SmallChipButton(
    label:   String,
    icon:    ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color:   Color   = TextDark,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled && color == GreenPrimary) GreenLight else if (enabled) Color(0xFFF1F5F9) else Color(0xFFE5E7EB))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = if (enabled) color else TextLight, modifier = Modifier.size(13.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (enabled) color else TextLight)
        }
    }
}
