package com.dompetku.ui.screen.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.TransactionType
import com.dompetku.ui.components.*
import com.dompetku.ui.theme.*
import com.dompetku.util.CurrencyFormatter

@Composable
fun TransactionsScreen(
    initialTypeFilter: TypeFilter?    = null,
    onTxnClick:        (Transaction) -> Unit,
    viewModel:         TransactionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Consume external filter
    LaunchedEffect(initialTypeFilter) {
        if (initialTypeFilter != null) viewModel.setTypeFilter(initialTypeFilter)
    }

    val filtered  = remember(state) { viewModel.filtered(state) }
    val totalInc  = filtered.filter { it.type == TransactionType.income  && it.category != "Penyesuaian Saldo" }.sumOf { it.amount }
    val totalExp  = filtered.filter { it.type == TransactionType.expense && it.category != "Penyesuaian Saldo" }.sumOf { it.amount }
    val net       = totalInc - totalExp

    // Group by date
    val grouped   = remember(filtered) {
        filtered.groupBy { it.date }.entries.sortedByDescending { it.key }
    }

    var showSearch by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
    ) {
        AppHeader(title = "Transaksi", showSearch = true, showDate = false, onSearchClick = { showSearch = !showSearch })

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))

            // ── Search bar ────────────────────────────────────────────────────
            AnimatedVisibility(visible = showSearch || state.filters.search.isNotEmpty()) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardWhite)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .padding(bottom = 4.dp),
                ) {
                    Icon(PhosphorIcons.Regular.MagnifyingGlass, null, tint = TextLight, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value         = state.filters.search,
                        onValueChange = viewModel::setSearch,
                        placeholder   = { Text("Cari transaksi, kategori...", fontSize = 14.sp) },
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor   = Color.Transparent,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    if (state.filters.search.isNotEmpty() || showSearch) {
                        IconButton(onClick = { viewModel.setSearch(""); showSearch = false }, modifier = Modifier.size(24.dp)) {
                            Icon(PhosphorIcons.Regular.X, null, tint = TextLight, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ── Type filter chips ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip("Semua",       state.filters.type == TypeFilter.ALL,      { viewModel.setTypeFilter(TypeFilter.ALL) })
                FilterChip("Pengeluaran", state.filters.type == TypeFilter.EXPENSE,  { viewModel.setTypeFilter(TypeFilter.EXPENSE) },  activeColor = RedExpense)
                FilterChip("Pemasukan",   state.filters.type == TypeFilter.INCOME,   { viewModel.setTypeFilter(TypeFilter.INCOME) })
                FilterChip("Transfer",    state.filters.type == TypeFilter.TRANSFER, { viewModel.setTypeFilter(TypeFilter.TRANSFER) }, activeColor = Color(0xFF6366F1))
            }

            // ── Date filter chips ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip("Semua",      state.filters.date == DateFilter.ALL,    { viewModel.setDateFilter(DateFilter.ALL) },   activeColor = BlueAccent)
                FilterChip("Bulan Ini",  state.filters.date == DateFilter.MONTH,  { viewModel.setDateFilter(DateFilter.MONTH) }, activeColor = BlueAccent)
                FilterChip("Hari Ini",   state.filters.date == DateFilter.TODAY,  { viewModel.setDateFilter(DateFilter.TODAY) }, activeColor = BlueAccent)
                FilterChip("Minggu Ini", state.filters.date == DateFilter.WEEK,   { viewModel.setDateFilter(DateFilter.WEEK) },  activeColor = BlueAccent)
                FilterChip("Custom",     state.filters.date == DateFilter.CUSTOM, { viewModel.setDateFilter(DateFilter.CUSTOM) },activeColor = BlueAccent)
            }

            // ── Custom date pickers ────────────────────────────────────────────
            if (state.filters.date == DateFilter.CUSTOM) {
                WhiteCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DARI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.5.sp)
                            OutlinedTextField(
                                value = state.filters.customFrom,
                                onValueChange = viewModel::setCustomFrom,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SAMPAI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.5.sp)
                            OutlinedTextField(
                                value = state.filters.customTo,
                                onValueChange = viewModel::setCustomTo,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // ── Summary banner ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(GreenPrimary, GreenDark)))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .padding(bottom = 14.dp),
            ) {
                Column {
                    Text("${filtered.size} transaksi", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    Text(
                        text  = "${if (net >= 0) "+" else ""}${CurrencyFormatter.format(net)}",
                        color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text("↓ Pemasukan", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            Text(CurrencyFormatter.format(totalInc), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("↑ Pengeluaran", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            Text(CurrencyFormatter.format(totalExp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Grouped transaction list ───────────────────────────────────────
            if (grouped.isEmpty()) {
                WhiteCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Tidak ada transaksi", color = TextLight, fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(12.dp))
                }
            } else {
                grouped.forEach { (date, txns) ->
                    val dayInc = txns.filter { it.type == TransactionType.income  && it.category != "Penyesuaian Saldo" }.sumOf { it.amount }
                    val dayExp = txns.filter { it.type == TransactionType.expense && it.category != "Penyesuaian Saldo" }.sumOf { it.amount }

                    DateGroupHeader(dateStr = date, income = dayInc, expense = dayExp)

                    WhiteCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), padding = 0.dp) {
                        txns.forEachIndexed { i, txn ->
                            val acc   = state.accounts.find { it.id == txn.accountId }
                            val toAcc = txn.toId?.let { id -> state.accounts.find { it.id == id } }
                            TransactionRow(
                                note          = txn.note,
                                category      = txn.category,
                                accountName   = acc?.name ?: "?",
                                toAccountName = toAcc?.name,
                                date          = txn.date,
                                time          = txn.time,
                                type          = txn.type,
                                amount        = txn.amount,
                                autoDetected  = txn.detected == true,
                                hidden        = state.hidden,
                                isLast        = i == txns.lastIndex,
                                onClick       = { onTxnClick(txn) },
                            )
                        }
                    }
                }
            }
        }
    }
}
