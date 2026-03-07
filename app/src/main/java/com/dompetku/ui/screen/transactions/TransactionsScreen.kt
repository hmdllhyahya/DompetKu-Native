package com.dompetku.ui.screen.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    initialTypeFilter: TypeFilter?    = null,
    onTxnClick:        (Transaction) -> Unit,
    viewModel:         TransactionsViewModel = hiltViewModel(),
) {
    val state       by viewModel.uiState.collectAsStateWithLifecycle()
    // BUG-01 fix: bind TextField to instant searchQuery, not state.filters.search
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    LaunchedEffect(initialTypeFilter) {
        if (initialTypeFilter != null) viewModel.setTypeFilter(initialTypeFilter)
    }

    val net        = state.totalIncome - state.totalExpense
    var showSearch by remember { mutableStateOf(false) }

    // BUG-05: date picker state
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker   by remember { mutableStateOf(false) }

    // DatePickerDialog helpers
    if (showFromPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.filters.customFrom.toEpochMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.setCustomFrom(millisToDateStr(millis))
                    }
                    showFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text("Batal") } },
        ) { DatePicker(state = pickerState) }
    }
    if (showToPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.filters.customTo.toEpochMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.setCustomTo(millisToDateStr(millis))
                    }
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Batal") } },
        ) { DatePicker(state = pickerState) }
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(PageBg),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {

        // ── App header (BUG-06: hide button moved to summary card) ────────────
        item {
            AppHeader(
                title         = "Transaksi",
                showSearch    = true,
                showDate      = false,
                onSearchClick = { showSearch = !showSearch },
            )
        }

        // ── Filters + summary ─────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))

                // Search bar (BUG-01: value = searchQuery, not state.filters.search)
                AnimatedVisibility(visible = showSearch || searchQuery.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
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
                            value         = searchQuery,
                            onValueChange = viewModel::setSearch,
                            placeholder   = { Text("Cari transaksi, kategori...", fontSize = 14.sp) },
                            singleLine    = true,
                            colors        = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor    = Color.Transparent,
                                focusedBorderColor      = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor   = Color.Transparent,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        if (searchQuery.isNotEmpty() || showSearch) {
                            IconButton(
                                onClick  = { viewModel.setSearch(""); showSearch = false },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(PhosphorIcons.Regular.X, null, tint = TextLight, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Type filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip("Semua",       state.filters.type == TypeFilter.ALL,      { viewModel.setTypeFilter(TypeFilter.ALL) })
                    FilterChip("Pengeluaran", state.filters.type == TypeFilter.EXPENSE,  { viewModel.setTypeFilter(TypeFilter.EXPENSE) }, activeColor = RedExpense)
                    FilterChip("Pemasukan",   state.filters.type == TypeFilter.INCOME,   { viewModel.setTypeFilter(TypeFilter.INCOME) })
                    FilterChip("Transfer",    state.filters.type == TypeFilter.TRANSFER, { viewModel.setTypeFilter(TypeFilter.TRANSFER) }, activeColor = Color(0xFF6366F1))
                }

                // Date filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip("Semua",      state.filters.date == DateFilter.ALL,    { viewModel.setDateFilter(DateFilter.ALL) },    activeColor = BlueAccent)
                    FilterChip("Bulan Ini",  state.filters.date == DateFilter.MONTH,  { viewModel.setDateFilter(DateFilter.MONTH) },  activeColor = BlueAccent)
                    FilterChip("Hari Ini",   state.filters.date == DateFilter.TODAY,  { viewModel.setDateFilter(DateFilter.TODAY) },  activeColor = BlueAccent)
                    FilterChip("Minggu Ini", state.filters.date == DateFilter.WEEK,   { viewModel.setDateFilter(DateFilter.WEEK) },   activeColor = BlueAccent)
                    FilterChip("Custom",     state.filters.date == DateFilter.CUSTOM, { viewModel.setDateFilter(DateFilter.CUSTOM) }, activeColor = BlueAccent)
                }

                // BUG-05: Custom date pickers — DatePickerDialog, bukan keyboard
                if (state.filters.date == DateFilter.CUSTOM) {
                    WhiteCard(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("DARI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .clickable { showFromPicker = true }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                ) {
                                    Text(
                                        text      = state.filters.customFrom.ifEmpty { "Pilih tanggal" },
                                        fontSize  = 13.sp,
                                        color     = if (state.filters.customFrom.isEmpty()) TextLight else TextDark,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SAMPAI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .clickable { showToPicker = true }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                ) {
                                    Text(
                                        text      = state.filters.customTo.ifEmpty { "Pilih tanggal" },
                                        fontSize  = 13.sp,
                                        color     = if (state.filters.customTo.isEmpty()) TextLight else TextDark,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary banner (BUG-06: hide button dipindah ke sini, sebelah jumlah transaksi)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(GreenPrimary, GreenDark)))
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .padding(bottom = 14.dp),
                ) {
                    Column {
                        // Count + hide button row
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier              = Modifier.fillMaxWidth(),
                        ) {
                            Text("${state.totalCount} transaksi", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { viewModel.toggleHideBalance() },
                            ) {
                                Icon(
                                    imageVector        = if (state.hidden) PhosphorIcons.Regular.EyeSlash else PhosphorIcons.Regular.Eye,
                                    contentDescription = null,
                                    tint               = Color.White,
                                    modifier           = Modifier.size(13.dp),
                                )
                            }
                        }
                        Text(
                            text  = "${if (net >= 0) "+" else ""}${CurrencyFormatter.format(net)}",
                            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Text("↓ Pemasukan", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                Text(CurrencyFormatter.format(state.totalIncome), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("↑ Pengeluaran", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                Text(CurrencyFormatter.format(state.totalExpense), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }
        } // end item: filters + summary

        // ── Grouped transaction list (virtualized) ────────────────────────────
        if (state.grouped.isEmpty()) {
            item {
                WhiteCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(
                        "Tidak ada transaksi", color = TextLight, fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(12.dp),
                    )
                }
            }
        } else {
            state.grouped.forEach { (date, txns) ->
                val dayInc = txns.filter { it.type == TransactionType.income  && it.category != "Penyesuaian Saldo" }.sumOf { it.amount }
                val dayExp = txns.filter { it.type == TransactionType.expense && it.category != "Penyesuaian Saldo" }.sumOf { it.amount }

                item(key = "hdr_$date") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DateGroupHeader(dateStr = date, income = dayInc, expense = dayExp)
                    }
                }
                item(key = "grp_$date") {
                    WhiteCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                        padding  = 0.dp,
                    ) {
                        txns.forEachIndexed { i, txn ->
                            val acc   = state.accountMap[txn.accountId]
                            val toAcc = txn.toId?.let { state.accountMap[it] }
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

// ── Date helpers ──────────────────────────────────────────────────────────────

private fun String.toEpochMillisOrNull(): Long? = runCatching {
    java.time.LocalDate.parse(this)
        .atStartOfDay(java.time.ZoneId.of("UTC"))
        .toInstant()
        .toEpochMilli()
}.getOrNull()

private fun millisToDateStr(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.of("UTC"))
        .toLocalDate()
        .toString()
