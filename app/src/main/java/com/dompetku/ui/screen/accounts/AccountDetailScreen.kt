package com.dompetku.ui.screen.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.AccountType
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.TransactionType
import androidx.hilt.navigation.compose.hiltViewModel
import com.dompetku.ui.components.*
import com.dompetku.ui.screen.transactions.TransactionDetailSheet
import com.dompetku.ui.screen.transactions.TransactionFormSheet
import com.dompetku.ui.screen.transactions.TransactionsViewModel
import com.dompetku.ui.theme.*
import com.dompetku.util.CurrencyFormatter
import com.dompetku.util.DeepLinkHelper

@Composable
fun AccountDetailScreen(
    account:      Account,
    accIndex:     Int,
    transactions: List<Transaction>,
    accounts:     List<Account> = emptyList(),
    onBack:       () -> Unit,
    onEdit:       (Account) -> Unit,
    onTxnClick:   (Transaction) -> Unit = {},
) {
    val context     = LocalContext.current
    val txnVm: TransactionsViewModel = hiltViewModel()
    var detailTxn    by remember { mutableStateOf<Transaction?>(null) }
    var editingTxn   by remember { mutableStateOf<Transaction?>(null) }

    val txns     = remember(transactions, account.id) {
        transactions.filter { it.accountId == account.id || it.toId == account.id }
            .sortedByDescending { it.date + it.time }
    }
    val income   = remember(txns) { txns.filter { it.type == TransactionType.income  }.sumOf { it.amount } }
    val expense  = remember(txns) { txns.filter { it.type == TransactionType.expense }.sumOf { it.amount } }

    val gradStart = Color(account.gradientStart.toInt())
    val gradEnd   = Color(account.gradientEnd.toInt())

    val brandInfo   = remember(account.brandKey) { account.brandKey?.let { com.dompetku.util.BrandDetector.byKey(it) } }
    val isLinkable  = account.type == AccountType.ewallet || account.type == AccountType.bank

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        // ── Gradient header ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(gradStart, gradEnd)))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 24.dp),
        ) {
            // Back button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onBack),
            ) {
                Icon(PhosphorIcons.Regular.CaretLeft, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // Edit button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { onEdit(account) }
                    .align(Alignment.TopEnd),
            ) {
                Icon(PhosphorIcons.Regular.PencilSimple, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }

            // Center content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                // Account icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                ) {
                    Icon(
                        imageVector = when (account.type) {
                            AccountType.ewallet -> PhosphorIcons.Regular.DeviceMobile
                            AccountType.emoney  -> PhosphorIcons.Regular.WifiHigh
                            AccountType.cash    -> PhosphorIcons.Regular.Wallet
                            AccountType.savings -> PhosphorIcons.Regular.PiggyBank
                            else                -> PhosphorIcons.Regular.CreditCard
                        },
                        contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(account.name, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

                // Balance row — negative badge
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.padding(vertical = 14.dp),
                ) {
                    Text(
                        "${if (account.balance < 0) "-" else ""}${CurrencyFormatter.format(Math.abs(account.balance))}",
                        color    = if (account.balance < 0) Color(0xFFFCA5A5) else Color.White,
                        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                    )
                    if (account.balance < 0) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFBBF24))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Icon(PhosphorIcons.Regular.Warning, null, tint = Color(0xFF78350F), modifier = Modifier.size(12.dp))
                            Text("NEGATIF", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF78350F))
                        }
                    }
                }

                if (!account.last4.isNullOrEmpty()) {
                    Text("•••• ${account.last4}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, letterSpacing = 3.sp)
                    Spacer(Modifier.height(14.dp))
                }

                // Income / expense stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    listOf("Pemasukan" to income, "Pengeluaran" to expense).forEach { (label, value) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(3.dp))
                                Text(CurrencyFormatter.compact(value), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                // Deep link button
                if (isLinkable && brandInfo != null) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { DeepLinkHelper.openApp(context, brandInfo.key) }
                            .padding(vertical = 11.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(PhosphorIcons.Regular.ArrowSquareOut, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Text("Buka ${brandInfo.displayName}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Transaction list ───────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text("Riwayat Transaksi", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark,
                modifier = Modifier.padding(bottom = 10.dp))

            WhiteCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                if (txns.isEmpty()) {
                    Text("Belum ada transaksi", color = TextLight, fontSize = 13.sp,
                        modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                } else {
                    txns.forEachIndexed { i, txn ->
                        // Resolve from/to names using accounts list
                        val fromAcc = accounts.find { it.id == txn.accountId }
                        val toAcc   = txn.toId?.let { id -> accounts.find { it.id == id } }
                        TransactionRow(
                            note          = txn.note,
                            category      = txn.category,
                            accountName   = fromAcc?.name ?: account.name,
                            toAccountName = toAcc?.name,
                            date          = txn.date,
                            time          = txn.time,
                            type          = txn.type,
                            amount        = txn.amount,
                            isLast        = i == txns.lastIndex,
                            onClick       = { detailTxn = txn },
                        )
                    }
                }
            }
        }
    }

    // ── Transaction detail sheet (local) ──────────────────────────────────────
    detailTxn?.let { txn ->
        TransactionDetailSheet(
            txn       = txn,
            accounts  = accounts,
            onDismiss = { detailTxn = null },
            onEdit    = { t -> editingTxn = t; detailTxn = null },
            onDelete  = { t -> txnVm.deleteTransaction(t); detailTxn = null },
        )
    }

    editingTxn?.let { old ->
        TransactionFormSheet(
            initial   = old,
            accounts  = accounts,
            onDismiss = { editingTxn = null },
            onSave    = { new -> txnVm.updateTransaction(old, new); editingTxn = null },
        )
    }
}
