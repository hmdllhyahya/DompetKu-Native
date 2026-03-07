package com.dompetku.ui.screen.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.dompetku.domain.model.Transaction
import com.dompetku.ui.components.*
import com.dompetku.ui.navigation.NavTab
import com.dompetku.ui.theme.*
import com.dompetku.util.CurrencyFormatter
import com.dompetku.util.DateUtils
import kotlin.math.min

@Composable
fun HomeScreen(
    onTabChange:    (NavTab) -> Unit,
    onTxnClick:     (Transaction) -> Unit,
    onAccountClick: (Account) -> Unit,
    viewModel:      HomeViewModel = hiltViewModel(),
) {
    val state  by viewModel.uiState.collectAsStateWithLifecycle()
    val hidden  = state.prefs.hideBalance

    val budgetPct   = if (state.monthlyBudget > 0) min(100, (state.monthExpense * 100 / state.monthlyBudget).toInt()) else 0
    val budgetLeft  = state.monthlyBudget - state.monthExpense
    val daysLeft    = DateUtils.daysRemainingInMonth()
    val todayRemain = if (daysLeft > 0) budgetLeft / daysLeft - state.todayExpense else 0L

    var showBudgetSheet by remember { mutableStateOf(false) }

    fun fmt(v: Long)  = if (hidden) "••••••" else CurrencyFormatter.format(v)
    fun fmtC(v: Long) = if (hidden) "••••"   else CurrencyFormatter.compact(v)

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(PageBg),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        item {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(CardWhite)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(GreenPrimary, GreenDark))),
                ) {
                    Icon(PhosphorIcons.Regular.UserCircle, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(DateUtils.greeting() + ",", fontSize = 11.sp, color = TextLight)
                    Text(
                        state.prefs.userProfile.name.ifEmpty { "Pengguna" },
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextDark,
                    )
                }
            }
        }
        } // end item: greeting

        item {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

            // Total balance card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(GreenPrimary, GreenDark)))
                    .padding(horizontal = 20.dp, vertical = 22.dp),
            ) {
                Box(modifier = Modifier.size(140.dp).offset(40.dp, (-40).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)).align(Alignment.TopEnd))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Total Saldo", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.2f)).clickable { viewModel.toggleHideBalance() },
                        ) {
                            Icon(if (hidden) PhosphorIcons.Regular.EyeSlash else PhosphorIcons.Regular.Eye, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(fmt(state.totalBalance), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            Triple("Income",  state.monthIncome,  PhosphorIcons.Regular.ArrowDownLeft),
                            Triple("Expense", state.monthExpense, PhosphorIcons.Regular.ArrowUpRight),
                        ).forEach { (label, value, icon) ->
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.15f)).clickable { onTabChange(NavTab.Transactions) }.padding(horizontal = 13.dp, vertical = 11.dp),
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.2f))) {
                                            Icon(icon, null, tint = Color.White, modifier = Modifier.size(11.dp))
                                        }
                                        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(fmtC(value), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Budget + Predictor carousel
            var carouselPage  by remember { mutableIntStateOf(0) }
            var showPredictor by remember { mutableStateOf(false) }
            val scope          = rememberCoroutineScope()
            val offsetPx       = remember { Animatable(0f) }
            var cardWidthPx    by remember { mutableIntStateOf(0) }
            // BUG-07 fix: only render peek card when actually dragging
            val isDragging     by remember { derivedStateOf { offsetPx.value != 0f } }

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { cardWidthPx = it.width }
                        .pointerInput(carouselPage) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    val threshold = cardWidthPx * 0.25f
                                    if (offsetPx.value < -threshold && carouselPage < 1) {
                                        carouselPage = 1
                                    } else if (offsetPx.value > threshold && carouselPage > 0) {
                                        carouselPage = 0
                                    }
                                    scope.launch { offsetPx.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                                },
                                onDragCancel = {
                                    scope.launch { offsetPx.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                                },
                                onHorizontalDrag = { _, delta ->
                                    val newOffset = (offsetPx.value + delta)
                                        .coerceIn(
                                            if (carouselPage >= 1) -cardWidthPx.toFloat() else -cardWidthPx * 0.4f,
                                            if (carouselPage <= 0) cardWidthPx.toFloat()  else  cardWidthPx * 0.4f,
                                        )
                                    scope.launch { offsetPx.snapTo(newOffset) }
                                },
                            )
                        },
                ) {
                    // Previous card: only peek when dragging (BUG-07)
                    if (carouselPage == 1 && isDragging) {
                        Box(
                            modifier = Modifier
                                .offset { androidx.compose.ui.unit.IntOffset((offsetPx.value - cardWidthPx).toInt(), 0) }
                                .fillMaxWidth(),
                        ) {
                            BudgetCard(
                                budgetPct    = budgetPct,
                                monthExp     = state.monthExpense,
                                budget       = state.monthlyBudget,
                                todayRemain  = todayRemain,
                                todayExpense = state.todayExpense,
                                daysLeft     = daysLeft,
                                hidden       = hidden,
                                onEdit       = { showBudgetSheet = true },
                                modifier     = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    // Current card
                    Box(
                        modifier = Modifier
                            .offset { androidx.compose.ui.unit.IntOffset(offsetPx.value.toInt(), 0) }
                            .fillMaxWidth(),
                    ) {
                        if (carouselPage == 0) {
                            BudgetCard(
                                budgetPct    = budgetPct,
                                monthExp     = state.monthExpense,
                                budget       = state.monthlyBudget,
                                todayRemain  = todayRemain,
                                todayExpense = state.todayExpense,
                                daysLeft     = daysLeft,
                                hidden       = hidden,
                                onEdit       = { showBudgetSheet = true },
                                modifier     = Modifier.fillMaxWidth(),
                            )
                        } else {
                            PredictorCard(
                                monthExpense = state.monthExpense,
                                daysLeft     = daysLeft,
                                onTap        = { showPredictor = true },
                                modifier     = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    // Next card: only peek when dragging (BUG-07)
                    if (carouselPage == 0 && isDragging) {
                        Box(
                            modifier = Modifier
                                .offset { androidx.compose.ui.unit.IntOffset((offsetPx.value + cardWidthPx).toInt(), 0) }
                                .fillMaxWidth(),
                        ) {
                            PredictorCard(
                                monthExpense = state.monthExpense,
                                daysLeft     = daysLeft,
                                onTap        = { showPredictor = true },
                                modifier     = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                // Dot indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    repeat(2) { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (i == carouselPage) 18.dp else 6.dp, 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (i == carouselPage) GreenPrimary else Color(0xFFD1D5DB))
                                .clickable {
                                    carouselPage = i
                                    scope.launch { offsetPx.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                                },
                        )
                    }
                }
            }

            if (showPredictor) {
                PredictorDetailSheet(
                    monthExpense = state.monthExpense,
                    daysLeft     = daysLeft,
                    onDismiss    = { showPredictor = false },
                )
            }

            // Akun Saya
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            ) {
                Text("Akun Saya", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                TextButton(onClick = { onTabChange(NavTab.Accounts) }) {
                    Text("Lihat semua", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (state.accounts.isEmpty()) {
                WhiteCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Belum ada akun", color = TextLight, fontSize = 12.sp)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.accounts.forEach { acc -> MiniAccountCard(acc, hidden, onClick = { onAccountClick(acc) }) }
                }
            }

            // Transaksi Terbaru
            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            ) {
                Text("Transaksi Terbaru", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                TextButton(onClick = { onTabChange(NavTab.Transactions) }) {
                    Text("Lihat semua", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            WhiteCard(modifier = Modifier.fillMaxWidth(), padding = 0.dp) {
                if (state.recentTxns.isEmpty()) {
                    Text("Belum ada transaksi", color = TextLight, fontSize = 13.sp,
                        modifier = Modifier.padding(20.dp).align(Alignment.CenterHorizontally))
                } else {
                    state.recentTxns.forEachIndexed { i, txn ->
                        val toAcc = txn.toId?.let { state.accountMap[it] }           // O(1)
                        TransactionRow(
                            note          = txn.note,
                            category      = txn.category,
                            accountName   = state.accountMap[txn.accountId]?.name ?: "?", // O(1)
                            toAccountName = toAcc?.name,
                            date          = txn.date,
                            time          = txn.time,
                            type          = txn.type,
                            amount        = txn.amount,
                            autoDetected  = txn.detected == true,
                            hidden        = hidden,
                            isLast        = i == state.recentTxns.lastIndex,
                            onClick       = { onTxnClick(txn) },
                        )
                    }
                }
            }
        }
        } // end item: main content
    } // end LazyColumn

    if (showBudgetSheet) {
        BudgetSheet(
            current      = state.monthlyBudget,
            savedPct     = state.prefs.savedPct,
            totalBalance = state.totalBalance,
            daysLeft     = daysLeft,
            onSave      = { budget, pct ->
                viewModel.setMonthlyBudget(budget)
                viewModel.setSavedPct(pct)
                showBudgetSheet = false
            },
            onDismiss = { showBudgetSheet = false },
        )
    }
}

// ── Budget card ───────────────────────────────────────────────────────────────
@Composable
private fun BudgetCard(
    budgetPct:    Int,
    monthExp:     Long,
    budget:       Long,
    todayRemain:  Long,
    todayExpense: Long,
    daysLeft:     Int,
    hidden:       Boolean,
    onEdit:       () -> Unit,
    modifier:     Modifier = Modifier,
) {
    WhiteCard(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text("Anggaran Bulanan", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
            TextButton(onClick = onEdit) { Text("Edit", color = GreenPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val ringColor = when { budgetPct >= 90 -> RedExpense; budgetPct >= 70 -> Color(0xFFF59E0B); else -> GreenPrimary }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(color = Color(0xFFF1F5F9), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 7.dp.toPx()))
                    drawArc(color = ringColor, startAngle = -90f, sweepAngle = 360f * budgetPct / 100f, useCenter = false, style = Stroke(width = 7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
                Text("$budgetPct%", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
            }
            Column(modifier = Modifier.weight(1f)) {
                BudgetRow("Terpakai", CurrencyFormatter.format(monthExp))
                BudgetRow("Limit",    CurrencyFormatter.format(budget))
                BudgetRow("Sisa/hari", if (budgetPct >= 100) "Habis!" else CurrencyFormatter.compact(if (daysLeft > 0) (budget - monthExp) / daysLeft else 0L), if (budgetPct >= 100) RedExpense else GreenPrimary)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (todayRemain < 0) RedLight else GreenLight).padding(horizontal = 12.dp, vertical = 9.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("SISA HARI INI", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = if (todayRemain < 0) RedExpense else GreenPrimary, letterSpacing = 0.5.sp)
                    Text(if (todayRemain < 0) "Lewat batas" else CurrencyFormatter.format(todayRemain), fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (todayRemain < 0) RedExpense else GreenPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Keluar hari ini", fontSize = 9.sp, color = TextLight)
                    Text(CurrencyFormatter.compact(todayExpense), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
            }
        }
    }
}

// ── Predictor card ────────────────────────────────────────────────────────────
@Composable
private fun PredictorCard(monthExpense: Long, daysLeft: Int, onTap: () -> Unit = {}, modifier: Modifier = Modifier) {
    val elapsed  = (30 - daysLeft).coerceAtLeast(1)
    val avgDaily = monthExpense / elapsed
    val estMonth = avgDaily * 30
    Box(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0A4F38), GreenDark)))
            .clickable(onClick = onTap)
            .padding(16.dp),
    ) {
        Column {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text("Prediksi Keuangan", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(PhosphorIcons.Regular.TrendUp, null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(16.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 3.dp)
                    ) { Text("Detail ›", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFA7F3D0)) }
                }
            }
            Text("Estimasi pengeluaran bulan ini", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkStat("AVG/HARI",   CurrencyFormatter.compact(avgDaily), Color(0xFFA7F3D0), Modifier.weight(1f))
                DarkStat("EST. BULAN", CurrencyFormatter.compact(estMonth),  Color.White,        Modifier.weight(1f))
                DarkStat("SISA HARI",  "$daysLeft hr",                        Color.White,        Modifier.weight(1f))
            }
        }
    }
}

// ── Predictor detail sheet ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PredictorDetailSheet(monthExpense: Long, daysLeft: Int, onDismiss: () -> Unit) {
    val elapsed     = (30 - daysLeft).coerceAtLeast(1)
    val avgDaily    = monthExpense / elapsed
    val estMonth    = avgDaily * 30
    val daysInMonth = 30

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = PageBg,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 24.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) {
                Column {
                    Text("Prediksi Keuangan", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                    Text("Estimasi pengeluaran bulanmu", fontSize = 12.sp, color = TextLight)
                }
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFE5E7EB)),
                ) {
                    Icon(PhosphorIcons.Regular.X, null, tint = TextDark, modifier = Modifier.size(14.dp))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0A4F38), GreenDark)))
                    .padding(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkStat("AVG/HARI",   CurrencyFormatter.compact(avgDaily), Color(0xFFA7F3D0), Modifier.weight(1f))
                    DarkStat("EST. BULAN", CurrencyFormatter.compact(estMonth),  Color.White,        Modifier.weight(1f))
                    DarkStat("SISA HARI",  "$daysLeft hr",                        Color.White,        Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
            listOf(
                Triple("Estimasi Pengeluaran Bulanan", PhosphorIcons.Regular.ChartLine,
                    "Berdasarkan rata-rata harian pengeluaranmu selama $elapsed hari terakhir, diproyeksikan total pengeluaran bulan ini akan mencapai ${CurrencyFormatter.format(estMonth)}."),
                Triple("Rata-Rata per Hari (AVG/HARI)", PhosphorIcons.Regular.CalendarBlank,
                    "Rata-rata kamu mengeluarkan ${CurrencyFormatter.format(avgDaily)} per hari bulan ini. Dihitung dari total ${CurrencyFormatter.format(monthExpense)} dibagi $elapsed hari."),
                Triple("Estimasi Bulan (EST. BULAN)", PhosphorIcons.Regular.TrendUp,
                    "Proyeksi total: ${CurrencyFormatter.format(avgDaily)} × $daysInMonth hari = ${CurrencyFormatter.format(estMonth)}."),
                Triple("Sisa Hari", PhosphorIcons.Regular.Timer,
                    "Masih ada $daysLeft hari tersisa. Kamu punya kesempatan menyesuaikan pola pengeluaran."),
            ).forEach { (title, icon, desc) ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardWhite).padding(14.dp),
                ) {
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(GreenLight)) {
                        Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                        Spacer(Modifier.height(3.dp))
                        Text(desc, fontSize = 11.sp, color = TextMedium, lineHeight = 16.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DarkStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.12f)).padding(8.dp)) {
        Column {
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.65f), letterSpacing = 0.4.sp)
            Spacer(Modifier.height(3.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
private fun BudgetRow(label: String, value: String, valueColor: Color = TextDark) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp)) {
        Text(label, fontSize = 11.sp, color = TextLight)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ── Mini account card ─────────────────────────────────────────────────────────
@Composable
private fun MiniAccountCard(account: Account, hidden: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(155.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(account.gradientStart.toInt()), Color(account.gradientEnd.toInt()))))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(modifier = Modifier.size(50.dp).offset(15.dp, (-15).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).align(Alignment.TopEnd))
        Column {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text(account.name, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(
                    when (account.type) {
                        AccountType.ewallet -> PhosphorIcons.Regular.DeviceMobile
                        AccountType.cash    -> PhosphorIcons.Regular.Wallet
                        AccountType.emoney  -> PhosphorIcons.Regular.WifiHigh
                        else                -> PhosphorIcons.Regular.CreditCard
                    }, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(15.dp),
                )
            }
            Text(if (hidden) "••••" else CurrencyFormatter.compact(account.balance), color = if (account.balance < 0) Color(0xFFFCA5A5) else Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (!account.last4.isNullOrEmpty()) "•••• ${account.last4}" else account.type.name, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, letterSpacing = 2.sp)
        }
    }
}

// ── Budget sheet ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetSheet(
    current:      Long,
    savedPct:     Int,
    totalBalance: Long,
    daysLeft:     Int,
    onSave:       (budget: Long, pct: Int) -> Unit,
    onDismiss:    () -> Unit,
) {
    var targetPct   by remember { mutableFloatStateOf(savedPct.coerceIn(0, 70).toFloat()) }
    var useCustom   by remember { mutableStateOf(current > 0 && savedPct == 0) }
    var customInput by remember {
        mutableStateOf(
            if (current > 0 && savedPct == 0)
                current.toString().reversed().chunked(3).joinToString(".").reversed()
            else ""
        )
    }

    val ditabung         = if (totalBalance > 0) (totalBalance * targetPct / 100).toLong() else 0L
    val bisaDibelanjakan = (totalBalance - ditabung).coerceAtLeast(0L)
    val budgetFinal      = if (useCustom) (customInput.replace(".", "").toLongOrNull() ?: 0L)
                           else bisaDibelanjakan
    val limitHarian      = if (daysLeft > 0 && budgetFinal > 0) budgetFinal / daysLeft else 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = PageBg,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Atur Budget Bulanan", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                color = TextDark, modifier = Modifier.padding(bottom = 16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(GreenPrimary, GreenDark)))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Column {
                    Text("Total Saldo Saat Ini", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    Text(CurrencyFormatter.format(totalBalance), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Sisa hari bulan ini", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                        Text("$daysLeft hari", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            WhiteCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                ) {
                    Text("TARGET TABUNGAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp)
                    Text("${targetPct.toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GreenPrimary)
                }
                Slider(
                    value = targetPct, onValueChange = { targetPct = it; useCustom = false },
                    valueRange = 0f..70f, steps = 13,
                    colors = SliderDefaults.colors(thumbColor = GreenPrimary, activeTrackColor = GreenPrimary, inactiveTrackColor = Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("0%", fontSize = 10.sp, color = TextLight)
                    Text("70%", fontSize = 10.sp, color = TextLight)
                }
            }
            Spacer(Modifier.height(10.dp))
            WhiteCard(modifier = Modifier.fillMaxWidth()) {
                Text("ANALISIS REKOMENDASI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 10.dp))
                listOf(
                    "Ditabung"             to CurrencyFormatter.format(ditabung),
                    "Bisa Dibelanjakan"    to CurrencyFormatter.format(bisaDibelanjakan),
                    "Budget Final Dipakai" to CurrencyFormatter.format(budgetFinal),
                    "Limit Harian Ideal"   to if (limitHarian > 0) CurrencyFormatter.format(limitHarian) else "Rp 0",
                ).forEach { (label, value) ->
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(label, fontSize = 13.sp, color = TextDark)
                        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("ATAU MASUKKAN NOMINAL SENDIRI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (useCustom) GreenLight else Color(0xFFF8FAFC))
                    .clickable { useCustom = !useCustom }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)).background(if (useCustom) GreenPrimary else Color(0xFFE5E7EB)),
                ) {
                    if (useCustom) Icon(PhosphorIcons.Regular.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
                Text("Pakai nominal kustom", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (useCustom) GreenPrimary else TextDark)
            }
            if (useCustom) {
                Spacer(Modifier.height(8.dp))
                WhiteCard(modifier = Modifier.fillMaxWidth()) {
                    Text("BUDGET KUSTOM (RP)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextLight, letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rp", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { raw ->
                                val digits = raw.replace("[^0-9]".toRegex(), "")
                                customInput = if (digits.isEmpty()) "" else digits.toLong().toString().reversed().chunked(3).joinToString(".").reversed()
                            },
                            placeholder = { Text("0", fontSize = 22.sp, color = TextLight) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextDark),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent,
                            ),
                            singleLine = true, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            val buttonEnabled = if (useCustom) customInput.replace(".", "").toLongOrNull()?.let { it > 0 } == true else budgetFinal > 0
            GreenButton(
                text    = "Terapkan Budget: ${CurrencyFormatter.compact(budgetFinal)}",
                enabled = buttonEnabled,
                onClick = {
                    val budget = if (useCustom) customInput.replace(".", "").toLongOrNull() ?: 0L else budgetFinal
                    onSave(budget, if (useCustom) 0 else targetPct.toInt())
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
