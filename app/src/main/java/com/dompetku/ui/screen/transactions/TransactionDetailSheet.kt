package com.dompetku.ui.screen.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.TransactionType
import com.dompetku.ui.components.CategoryBubble
import com.dompetku.ui.components.catConfig
import com.dompetku.ui.theme.*
import com.dompetku.util.CurrencyFormatter
import com.dompetku.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    txn:       Transaction,
    accounts:  List<Account>,
    onDismiss: () -> Unit,
    onEdit:    (Transaction) -> Unit,
    onDelete:  (Transaction) -> Unit,
) {
    val cfg      = catConfig(txn.category)
    val account  = accounts.find { it.id == txn.accountId }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val amountColor = when (txn.type) {
        TransactionType.income   -> GreenPrimary
        TransactionType.expense  -> RedExpense
        TransactionType.transfer -> BlueAccent
    }
    val amountPrefix = when (txn.type) {
        TransactionType.income   -> "+"
        TransactionType.expense  -> "-"
        TransactionType.transfer -> ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color.Transparent,
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(PageBg),
        ) {
            // ── Gradient header ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Brush.linearGradient(listOf(cfg.color.copy(alpha = 0.85f), cfg.color)))
                    .padding(20.dp)
                    .padding(bottom = 24.dp),
            ) {
                // Back button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.2f)).clickable(onClick = onDismiss),
                ) {
                    Icon(PhosphorIcons.Regular.X, null, tint = Color.White, modifier = Modifier.size(15.dp))
                }
                // Edit + Delete
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.2f)).clickable { onEdit(txn); onDismiss() }) {
                        Icon(PhosphorIcons.Regular.PencilSimple, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.2f)).clickable { showDeleteConfirm = true }) {
                        Icon(PhosphorIcons.Regular.Trash, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                // Center content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    CategoryBubble(txn.category, size = 64.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(txn.category, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$amountPrefix${CurrencyFormatter.format(txn.amount)}",
                        color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                    )
                    if (txn.type == TransactionType.transfer && txn.adminFee > 0) {
                        Text("+ ${CurrencyFormatter.format(txn.adminFee)} biaya admin", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                    }
                }
            }

            // ── Detail rows ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                DetailCard {
                    DetailRow(PhosphorIcons.Regular.CalendarBlank, "Tanggal", DateUtils.formatDisplay(txn.date))
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    DetailRow(PhosphorIcons.Regular.Clock, "Waktu", txn.time)
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    if (account != null) {
                        DetailRow(PhosphorIcons.Regular.Wallet, "Akun", account.name)
                        HorizontalDivider(color = Color(0xFFF8FAFC))
                    }
                    if (txn.type == TransactionType.transfer) {
                        val fromAcc = accounts.find { it.id == txn.fromId }
                        val toAcc   = accounts.find { it.id == txn.toId }
                        if (fromAcc != null) { DetailRow(PhosphorIcons.Regular.ArrowUpRight,   "Dari", fromAcc.name); HorizontalDivider(color = Color(0xFFF8FAFC)) }
                        if (toAcc   != null) { DetailRow(PhosphorIcons.Regular.ArrowDownLeft,  "Ke",   toAcc.name);   HorizontalDivider(color = Color(0xFFF8FAFC)) }
                    }
                    DetailRow(PhosphorIcons.Regular.Tag, "Tipe",
                        when (txn.type) { TransactionType.income -> "Pemasukan"; TransactionType.expense -> "Pengeluaran"; TransactionType.transfer -> "Transfer" })
                }

                // Note
                if (txn.note.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardWhite).padding(14.dp)) {
                        Text("CATATAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 6.dp))
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(PhosphorIcons.Regular.Note, null, tint = TextLight, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                            Text(txn.note, fontSize = 14.sp, color = TextDark, lineHeight = 20.sp)
                        }
                        if (txn.detected == true) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GreenLight).padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(PhosphorIcons.Regular.MagicWand, null, tint = GreenPrimary, modifier = Modifier.size(11.dp))
                                Text("Kategori otomatis terdeteksi", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title  = { Text("Hapus Transaksi?", fontWeight = FontWeight.ExtraBold) },
            text   = { Text("Transaksi ini akan dihapus permanen.") },
            confirmButton = {
                Button(onClick = { onDelete(txn); showDeleteConfirm = false; onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpense)) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardWhite), content = content)
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFF8FAFC))) {
            Icon(icon, null, tint = TextMedium, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 12.sp, color = TextLight, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    }
}
