package com.dompetku.ui.screen.analytics

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.*
import com.dompetku.ui.components.*
import com.dompetku.ui.theme.*
import com.dompetku.util.CurrencyFormatter

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
    ) {
        AppHeader(title = "Analisis", showDate = false)

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))

            // ── Type filter ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("all" to "Semua", "expense" to "Pengeluaran", "income" to "Pemasukan", "transfer" to "Transfer")
                    .forEach { (id, label) ->
                        FilterChip(label, state.typeFilter == id, { viewModel.setTypeFilter(id) })
                    }
            }

            // ── Date filter ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("all" to "Semua", "today" to "Hari Ini", "week" to "Minggu Ini",
                       "month" to "Bulan Ini", "year" to "Tahun Ini", "custom" to "Custom")
                    .forEach { (id, label) ->
                        FilterChip(label, state.dateFilter == id, { viewModel.setDateFilter(id) }, activeColor = BlueAccent)
                    }
            }

            // ── Income / Expense summary cards ────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                SummaryCard("Pemasukan",   state.totalIncome,  GreenPrimary, Color(0xFFE8FAF0), Modifier.weight(1f))
                SummaryCard("Pengeluaran", state.totalExpense, RedExpense,   RedLight,           Modifier.weight(1f))
            }

            // ── Pie chart card ────────────────────────────────────────────────
            WhiteCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("Pengeluaran per Kategori", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    color = TextDark, modifier = Modifier.padding(bottom = 10.dp))
                if (state.pieData.isEmpty()) {
                    Text("Tidak ada data", color = TextLight, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                } else {
                    var activeIdx by remember { mutableIntStateOf(-1) }
                    PieDonut(pieData = state.pieData, activeIdx = activeIdx, onActiveChange = { activeIdx = it })
                    Spacer(Modifier.height(8.dp))
                    state.pieData.take(6).forEachIndexed { i, datum ->
                        val isActive = activeIdx == i
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeIdx = if (activeIdx == i) -1 else i }
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isActive) catConfig(datum.category).bgColor else Color.Transparent)
                                .padding(vertical = 8.dp, horizontal = if (isActive) 8.dp else 0.dp),
                        ) {
                            CategoryBubble(datum.category, size = 34.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(datum.category, fontSize = 12.sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold, color = TextDark)
                                Text("${datum.pct}%", fontSize = 10.sp, color = TextLight)
                            }
                            Text(CurrencyFormatter.compact(datum.amount), fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) catConfig(datum.category).color else TextDark)
                        }
                        if (i < minOf(state.pieData.size, 6) - 1)
                            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
                    }
                }
            }

            // ── Weekly bar chart ──────────────────────────────────────────────
            WhiteCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("Ringkasan Mingguan", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    color = TextDark, modifier = Modifier.padding(bottom = 12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.Bottom,
                    modifier              = Modifier.fillMaxWidth().height(120.dp),
                ) {
                    val maxVal = state.barData.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1L) ?: 1L
                    state.barData.forEach { bar ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier            = Modifier.weight(1f).fillMaxHeight(),
                        ) {
                            Box(modifier = Modifier.width(10.dp).height((bar.income.toFloat() / maxVal * 80).dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(GreenPrimary))
                            Spacer(Modifier.height(3.dp))
                            Box(modifier = Modifier.width(10.dp).height((bar.expense.toFloat() / maxVal * 80).dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(RedExpense))
                            Spacer(Modifier.height(6.dp))
                            Text(bar.label, fontSize = 9.sp, color = TextLight)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(GreenPrimary, "Pemasukan")
                    LegendDot(RedExpense,   "Pengeluaran")
                }
            }

            // ── Lifestyle card ────────────────────────────────────────────────
            state.lifestyle?.let { ls ->
                WhiteCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier              = Modifier.padding(bottom = 12.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(GreenLight)) {
                            Icon(PhosphorIcons.Regular.UserCircle, null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(ls.personaTitle, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                            Text(ls.personaDesc, fontSize = 11.sp, color = TextMedium, lineHeight = 16.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        ls.top3.forEach { datum ->
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(catConfig(datum.category).bgColor).padding(8.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    CategoryBubble(datum.category, 28.dp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${datum.pct}%", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                        color = catConfig(datum.category).color)
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(GreenLight).padding(12.dp).padding(bottom = 8.dp)) {
                        Column {
                            Text("💡 FUN FACT HARI INI", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                color = GreenPrimary, letterSpacing = 0.6.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(ls.funFact, fontSize = 12.sp, color = TextDark, lineHeight = 19.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF111827)).padding(12.dp)) {
                        Column {
                            Row(horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                Text("🎯 MISI MINGGU INI", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                    color = Color.White.copy(alpha = 0.85f), letterSpacing = 0.6.sp)
                                Text(ls.missionBadge, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    color = Color(0xFF86EFAC),
                                    modifier = Modifier.clip(RoundedCornerShape(99.dp))
                                        .background(GreenPrimary.copy(alpha = 0.25f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            Text(ls.missionTitle, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text(ls.missionDesc, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 17.sp)
                        }
                    }
                }
            }

            // ── Salary insight card ───────────────────────────────────────────
            state.salaryInsight?.let { si ->
                WhiteCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Text("Estimasi Gaji Ideal", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Color(0xFFF8FAFC))
                                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Icon(PhosphorIcons.Regular.Briefcase, null, tint = TextDark, modifier = Modifier.size(16.dp))
                            Text(si.jobLevel, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(GreenLight).padding(10.dp)) {
                            Column {
                                Text("BIAYA HIDUP (30H)", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                    color = GreenPrimary, letterSpacing = 0.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(CurrencyFormatter.compact(si.livingCost), fontSize = 16.sp,
                                    fontWeight = FontWeight.Black, color = TextDark)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF8FAFC)).padding(10.dp)) {
                            Column {
                                Text("GAJI IDEAL MIN.", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                    color = TextMedium, letterSpacing = 0.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(CurrencyFormatter.compact(si.idealSalary), fontSize = 16.sp,
                                    fontWeight = FontWeight.Black, color = TextDark)
                            }
                        }
                    }
                    if (si.careerPath.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(GreenLight).padding(10.dp).padding(bottom = 10.dp)) {
                            Column {
                                Text("JENJANG KARIR", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                    color = GreenPrimary, letterSpacing = 0.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(si.careerPath, fontSize = 12.sp, color = TextDark, lineHeight = 19.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFC)).padding(10.dp)) {
                        Column {
                            Text("REKOMENDASI KARIR", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                color = TextMedium, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(si.freeJobs, fontSize = 12.sp, color = TextDark, lineHeight = 19.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Donut chart ───────────────────────────────────────────────────────────────
@Composable
private fun PieDonut(
    pieData:        List<PieDatum>,
    activeIdx:      Int,
    onActiveChange: (Int) -> Unit,
) {
    if (pieData.isEmpty()) return

    val total   = pieData.sumOf { it.amount }.coerceAtLeast(1L)
    val colors  = pieData.take(6).map { catConfig(it.category).color }
    val sweeps  = pieData.take(6).map { (it.amount.toFloat() / total * 360f).coerceAtLeast(2f) }
    val density = LocalDensity.current

    var drawn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { drawn = true }
    val progress by animateFloatAsState(
        targetValue   = if (drawn) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label         = "donut",
    )

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier              = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
            Canvas(modifier = Modifier.size(160.dp).clickable { onActiveChange(-1) }) {
                val strokeWidth = with(density) { 28.dp.toPx() }
                val gap         = with(density) { 3.dp.toPx() }
                val radius      = (min(size.width, size.height) - strokeWidth) / 2f
                val center      = Offset(size.width / 2f, size.height / 2f)

                var startAngle = -90f
                sweeps.forEachIndexed { i, rawSweep ->
                    val sweep    = rawSweep * progress
                    val isActive = activeIdx == i
                    val arcStroke = if (isActive) strokeWidth * 1.18f else strokeWidth
                    val arcR      = if (isActive) radius + (arcStroke - strokeWidth) / 2f else radius
                    val arcOval   = Size(arcR * 2, arcR * 2)
                    val arcTL     = Offset(center.x - arcR, center.y - arcR)

                    if (isActive) {
                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                asFrameworkPaint().apply {
                                    isAntiAlias = true
                                    color = android.graphics.Color.TRANSPARENT
                                    setShadowLayer(with(density) { 10.dp.toPx() }, 0f, 0f,
                                        colors[i].copy(alpha = 0.55f).toArgb())
                                }
                            }
                            canvas.drawArc(arcTL.x, arcTL.y,
                                arcTL.x + arcOval.width, arcTL.y + arcOval.height,
                                startAngle + gap / 2, sweep - gap, false, paint)
                        }
                    }
                    drawArc(
                        color      = colors[i].copy(alpha = if (activeIdx == -1 || isActive) 1f else 0.35f),
                        startAngle = startAngle + gap / 2,
                        sweepAngle = (sweep - gap).coerceAtLeast(0f),
                        useCenter  = false,
                        topLeft    = arcTL,
                        size       = arcOval,
                        style      = Stroke(width = arcStroke, cap = StrokeCap.Round),
                    )
                    startAngle += rawSweep
                }
            }

            if (activeIdx >= 0 && activeIdx < pieData.size) {
                val datum = pieData[activeIdx]
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(88.dp)) {
                    CategoryBubble(datum.category, 24.dp)
                    Spacer(Modifier.height(2.dp))
                    Text("${datum.pct}%", fontSize = 14.sp, fontWeight = FontWeight.Black,
                        color = catConfig(datum.category).color)
                    Text(CurrencyFormatter.compact(datum.amount), fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, color = TextMedium, maxLines = 1)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", fontSize = 10.sp, color = TextLight)
                    Text(CurrencyFormatter.compact(total), fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold, color = TextDark)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            pieData.take(6).forEachIndexed { i, datum ->
                val isActive = activeIdx == i
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) catConfig(datum.category).bgColor else Color.Transparent)
                        .clickable { onActiveChange(if (isActive) -1 else i) }
                        .padding(horizontal = if (isActive) 6.dp else 0.dp, vertical = 3.dp),
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp))
                        .background(colors[i].copy(alpha = if (activeIdx == -1 || isActive) 1f else 0.35f)))
                    Text(datum.category, fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isActive) catConfig(datum.category).color else TextMedium,
                        maxLines = 1, modifier = Modifier.weight(1f))
                    Text("${datum.pct}%", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = if (isActive) catConfig(datum.category).color else TextLight)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, amount: Long, textColor: Color, bg: Color, modifier: Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(18.dp)).background(bg).padding(16.dp)) {
        Column {
            Text(label, fontSize = 12.sp, color = textColor.copy(alpha = 0.75f))
            Spacer(Modifier.height(4.dp))
            Text(CurrencyFormatter.compact(amount), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, fontSize = 10.sp, color = TextMedium)
    }
}
