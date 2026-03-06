package com.dompetku.ui.screen.analytics

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
fun AnalyticsScreen(viewModel: AnalyticsViewModel = hiltViewModel()) {
    val state    by viewModel.uiState.collectAsStateWithLifecycle()
    val filtered  = remember(state) { viewModel.filtered(state) }
    val totalInc  = filtered.filter { it.type == TransactionType.income }.sumOf { it.amount }
    val totalExp  = filtered.filter { it.type == TransactionType.expense }.sumOf { it.amount }

    // Pie data: expense by category
    val pieData = remember(filtered) {
        filtered.filter { it.type == TransactionType.expense }
            .groupBy { it.category }
            .entries.sortedByDescending { it.value.sumOf { t -> t.amount } }
            .map { (cat, txns) ->
                val value = txns.sumOf { it.amount }
                Triple(cat, value, if (totalExp > 0) (value * 100 / totalExp).toInt() else 0)
            }
    }

    // Weekly bar data (current month, grouped into 4 weeks)
    val barData = remember(state.allTxns) {
        val wk = mapOf("Mg 1" to longArrayOf(0, 0), "Mg 2" to longArrayOf(0, 0),
            "Mg 3" to longArrayOf(0, 0), "Mg 4" to longArrayOf(0, 0))
        val monthStart = java.time.LocalDate.now().withDayOfMonth(1).toString()
        state.allTxns.filter { it.date >= monthStart }.forEach { t ->
            val day = t.date.substringAfterLast("-").toIntOrNull() ?: 1
            val key = when { day <= 7 -> "Mg 1"; day <= 14 -> "Mg 2"; day <= 21 -> "Mg 3"; else -> "Mg 4" }
            wk[key]?.let { arr ->
                when (t.type) {
                    TransactionType.income  -> arr[0] += t.amount
                    TransactionType.expense -> arr[1] += t.amount
                    else -> {}
                }
            }
        }
        wk.entries.map { Triple(it.key, it.value[0], it.value[1]) }
    }

    // Lifestyle / persona
    val lifestyle = remember(filtered) { computeLifestyle(filtered, totalExp) }
    val salaryInsight = remember(filtered, state.userJob) { computeSalaryInsight(filtered, state.userJob) }

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
                listOf("all" to "Semua", "today" to "Hari Ini", "week" to "Minggu Ini", "month" to "Bulan Ini", "year" to "Tahun Ini", "custom" to "Custom")
                    .forEach { (id, label) ->
                        FilterChip(label, state.dateFilter == id, { viewModel.setDateFilter(id) }, activeColor = BlueAccent)
                    }
            }

            // ── Income / Expense summary cards ────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                SummaryCard("Pemasukan",   totalInc, GreenPrimary, Color(0xFFE8FAF0), Modifier.weight(1f))
                SummaryCard("Pengeluaran", totalExp, RedExpense,   RedLight,           Modifier.weight(1f))
            }

            // ── Pie chart card ────────────────────────────────────────────────
            WhiteCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("Pengeluaran per Kategori", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark, modifier = Modifier.padding(bottom = 10.dp))
                if (pieData.isEmpty()) {
                    Text("Tidak ada data", color = TextLight, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                } else {
                    var activeIdx by remember { mutableIntStateOf(-1) }
                    // Simple donut representation using boxes (no external chart library dependency)
                    PieDonut(pieData = pieData, activeIdx = activeIdx, onActiveChange = { activeIdx = it })
                    Spacer(Modifier.height(8.dp))
                    // Category list
                    pieData.take(6).forEachIndexed { i, (cat, value, pct) ->
                        val isActive = activeIdx == i
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeIdx = if (activeIdx == i) -1 else i }
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isActive) (catConfig(cat).bgColor) else Color.Transparent)
                                .padding(vertical = 8.dp, horizontal = if (isActive) 8.dp else 0.dp),
                        ) {
                            CategoryBubble(cat, size = 34.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat, fontSize = 12.sp, fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold, color = TextDark)
                                Text("$pct%", fontSize = 10.sp, color = TextLight)
                            }
                            Text(CurrencyFormatter.compact(value), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isActive) catConfig(cat).color else TextDark)
                        }
                        if (i < minOf(pieData.size, 6) - 1) HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
                    }
                }
            }

            // ── Weekly bar chart ──────────────────────────────────────────────
            WhiteCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("Ringkasan Mingguan", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark, modifier = Modifier.padding(bottom = 12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.Bottom,
                    modifier              = Modifier.fillMaxWidth().height(120.dp),
                ) {
                    val maxVal = barData.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1L)
                    barData.forEach { (week, inc, exp) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier            = Modifier.weight(1f).fillMaxHeight(),
                        ) {
                            // Income bar
                            Box(modifier = Modifier.width(10.dp).height((inc.toFloat() / maxVal * 80).dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(GreenPrimary))
                            Spacer(Modifier.height(3.dp))
                            // Expense bar
                            Box(modifier = Modifier.width(10.dp).height((exp.toFloat() / maxVal * 80).dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(RedExpense))
                            Spacer(Modifier.height(6.dp))
                            Text(week, fontSize = 9.sp, color = TextLight)
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
            lifestyle?.let { ls ->
                WhiteCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    // Persona header
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier              = Modifier.padding(bottom = 12.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(GreenLight),
                        ) {
                            Icon(PhosphorIcons.Regular.UserCircle, null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(ls.personaTitle, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                            Text(ls.personaDesc, fontSize = 11.sp, color = TextMedium, lineHeight = 16.sp)
                        }
                    }
                    // Top 3 categories
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        ls.top3.forEach { (cat, _, pct) ->
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(catConfig(cat).bgColor).padding(8.dp),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    CategoryBubble(cat, 28.dp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("$pct%", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = catConfig(cat).color)
                                }
                            }
                        }
                    }
                    // Fun fact
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GreenLight).padding(12.dp).padding(bottom = 8.dp),
                    ) {
                        Column {
                            Text("💡 FUN FACT HARI INI", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = GreenPrimary, letterSpacing = 0.6.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(ls.funFact, fontSize = 12.sp, color = TextDark, lineHeight = 19.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Mission card
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF111827)).padding(12.dp),
                    ) {
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier              = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            ) {
                                Text("🎯 MISI MINGGU INI", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.85f), letterSpacing = 0.6.sp)
                                Text(ls.missionBadge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF86EFAC),
                                    modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(GreenPrimary.copy(alpha = 0.25f)).padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            Text(ls.missionTitle, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text(ls.missionDesc, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 17.sp)
                        }
                    }
                }
            }

            // ── Salary insight card ───────────────────────────────────────────
            salaryInsight?.let { si ->
                WhiteCard(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    ) {
                        Text("Estimasi Gaji Ideal", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Color(0xFFF8FAFC)).padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Icon(PhosphorIcons.Regular.Briefcase, null, tint = TextDark, modifier = Modifier.size(16.dp))
                            Text(si.jobLevel, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(GreenLight).padding(10.dp)) {
                            Column {
                                Text("BIAYA HIDUP (30H)", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = GreenPrimary, letterSpacing = 0.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(CurrencyFormatter.compact(si.livingCost), fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextDark)
                            }
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF8FAFC)).padding(10.dp)) {
                            Column {
                                Text("GAJI IDEAL MIN.", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = TextMedium, letterSpacing = 0.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(CurrencyFormatter.compact(si.idealSalary), fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextDark)
                            }
                        }
                    }
                    if (si.careerPath.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GreenLight).padding(10.dp).padding(bottom = 10.dp)) {
                            Column {
                                Text("JENJANG KARIR", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = GreenPrimary, letterSpacing = 0.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(si.careerPath, fontSize = 12.sp, color = TextDark, lineHeight = 19.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF8FAFC)).padding(10.dp)) {
                        Column {
                            Text("REKOMENDASI KARIR", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = TextMedium, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(si.freeJobs, fontSize = 12.sp, color = TextDark, lineHeight = 19.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Mini donut visualization ──────────────────────────────────────────────────
@Composable
private fun PieDonut(
    pieData:        List<Triple<String, Long, Int>>,
    activeIdx:      Int,
    onActiveChange: (Int) -> Unit,
) {
    val total = pieData.sumOf { it.second }.coerceAtLeast(1L)
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier              = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
        // Simplified visual: colored bars proportional to amount
        Column(modifier = Modifier.weight(1f)) {
            pieData.take(6).forEachIndexed { i, (cat, value, _) ->
                val width = (value.toFloat() / total * 100).coerceIn(2f, 100f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(width / 100f)
                        .height(8.dp)
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(catConfig(cat).color.copy(alpha = if (activeIdx == i || activeIdx == -1) 1f else 0.3f)),
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        // Active category detail
        if (activeIdx >= 0 && activeIdx < pieData.size) {
            val (cat, value, pct) = pieData[activeIdx]
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(catConfig(cat).bgColor)
                    .padding(10.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CategoryBubble(cat, 28.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(cat, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = catConfig(cat).color, maxLines = 1)
                    Text(CurrencyFormatter.compact(value), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                    Text("$pct%", fontSize = 10.sp, color = TextLight)
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

// ── Lifestyle compute ─────────────────────────────────────────────────────────
data class LifestyleData(
    val personaTitle: String, val personaDesc: String,
    val top3: List<Triple<String, Long, Int>>,
    val funFact: String, val missionTitle: String, val missionDesc: String, val missionBadge: String,
)

private fun computeLifestyle(txns: List<Transaction>, totalExp: Long): LifestyleData? {
    if (totalExp == 0L) return null
    val byCat = txns.filter { it.type == TransactionType.expense }
        .groupBy { it.category }
        .mapValues { e -> e.value.sumOf { it.amount } }
    val top3  = byCat.entries.sortedByDescending { it.value }.take(3)
        .map { Triple(it.key, it.value, if (totalExp > 0) (it.value * 100 / totalExp).toInt() else 0) }
    val topCat = top3.firstOrNull()?.first ?: "Lainnya"

    val (title, desc) = when (topCat) {
        "Makan & Minum"  -> "The Foodie" to "Kebahagiaanmu sering datang dari makan enak. Mantap, tapi tetap jaga limit."
        "Belanja Online" -> "The Online Hunter" to "Cepat, praktis, dan sering checkout. Kamu jago cari barang, tapi rawan impulsif."
        "Transportasi"   -> "The Road Runner" to "Mobilitas tinggi. Kamu punya ritme jalan-jalan yang konsisten."
        "Hiburan"        -> "The Entertainer" to "Kamu tahu cara recharge. Pastikan hiburan tetap jadi hadiah, bukan kebiasaan mahal."
        "Tagihan"        -> "The Adulting Pro" to "Tagihan aman, hidup stabil. Kamu serius dan bertanggung jawab."
        "Kesehatan"      -> "The Wellness Keeper" to "Kamu investasi ke kesehatan. Good choice."
        else             -> "The Minimalist" to "Kamu hemat dan terarah. Pengeluaranmu rapi dan fungsional."
    }

    val impulsive = listOf("Belanja Online","Hiburan","Perawatan").sumOf { byCat[it] ?: 0L } * 100 / totalExp
    val funFact = when {
        impulsive >= 45 -> "🛒 $impulsive% pengeluaranmu masuk kategori impulsif! Coba tunggu 24 jam sebelum checkout."
        else -> "⚖️ Top pengeluaran: ${top3.getOrNull(0)?.first ?: "-"} (${top3.getOrNull(0)?.third ?: 0}%). Sudah tahu ke mana uang pergi!"
    }
    val (mTitle, mDesc, mBadge) = if (impulsive >= 45)
        Triple("🚫 No-Checkout 7 Hari","Boleh masukin ke wishlist, tapi jangan klik bayar dulu selama 7 hari.","Penguasa Keinginan 🏆")
    else
        Triple("🔬 Audit Pengeluaran Kecil","7 hari ke depan, catat SEMUA pengeluaran di bawah Rp20.000 — hasilnya sering mengejutkan!","Micro-Tracker 🔍")

    return LifestyleData(title, desc, top3, funFact, mTitle, mDesc, mBadge)
}

// ── Salary insight compute ────────────────────────────────────────────────────
data class SalaryInsight(val livingCost: Long, val idealSalary: Long, val jobLevel: String, val careerPath: String, val freeJobs: String)

private fun computeSalaryInsight(txns: List<Transaction>, userJob: String): SalaryInsight? {
    val living = txns.filter { it.type == TransactionType.expense }.sumOf { it.amount }
    if (living == 0L) return null
    val ideal = (living / 0.7).toLong()
    val (level, free) = when {
        ideal < 3_000_000  -> "Entry level / Part-time" to "Barista, kasir ritel, customer service junior, admin entry."
        ideal < 6_000_000  -> "Fresh graduate / Junior"  to "Admin kantor, customer service, junior designer, staf operasional."
        ideal < 10_000_000 -> "Mid-level profesional"    to "Account executive, software developer, analis data, creative specialist."
        ideal < 20_000_000 -> "Senior profesional"       to "Senior engineer, product designer, project manager, konsultan."
        else               -> "Lead / Manajer"           to "Engineering lead, product manager, direktur, konsultan spesialis."
    }
    val career = when {
        userJob.contains("pelajar", ignoreCase = true) || userJob.contains("mahasiswa", ignoreCase = true) ->
            "Mulai part-time/freelance di bidang minatmu sambil kuliah. Bangun portofolio sejak dini."
        userJob.contains("engineer", ignoreCase = true) || userJob.contains("programmer", ignoreCase = true) ->
            if (ideal < 15_000_000) "Dalami satu stack/domain (AI, cloud, fintech). Remote job internasional bisa 3–5x gaji lokal."
            else "Lead engineer atau tech lead. Pertimbangkan startup equity atau freelance internasional."
        userJob.contains("freelancer", ignoreCase = true) ->
            "Naikkan rate per proyek, bangun personal brand, dan pertimbangkan niche khusus untuk bayaran premium."
        userJob.contains("karyawan", ignoreCase = true) ->
            if (ideal < 10_000_000) "Tingkatkan skill teknis. Negosiasi kenaikan gaji tahunan atau pindah ke perusahaan lebih besar."
            else "Incaran posisi manajerial atau spesialis senior."
        else -> "Cari peluang naik jabatan, sertifikasi, atau pindah ke industri dengan rata-rata gaji lebih tinggi."
    }
    return SalaryInsight(living, ideal, level, career, free)
}
