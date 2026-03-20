package com.dompetku.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dompetku.data.preferences.UserPreferences
import com.dompetku.data.repository.TransactionRepository
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.TransactionType
import com.dompetku.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ── Analytics data classes ────────────────────────────────────────────────────

data class PieDatum(val category: String, val amount: Long, val pct: Int)
data class BarDatum(val label: String, val income: Long, val expense: Long)
data class MonthDatum(val label: String, val income: Long, val expense: Long, val savings: Long)

data class LifestyleData(
    val personaTitle: String,
    val personaDesc:  String,
    val top3:         List<PieDatum>,
    val funFact:      String,
    val missionTitle: String,
    val missionDesc:  String,
    val missionBadge: String,
)

data class SalaryInsight(
    val livingCost:  Long,
    val idealSalary: Long,
    val jobLevel:    String,
    val careerPath:  String,
    val freeJobs:    String,
)

data class AnalyticsUiState(
    val allTxns:       List<Transaction> = emptyList(),
    val userJob:       String = "",
    val userAge:       Int    = 0,
    val typeFilter:    String = "all",
    val dateFilter:    String = "month",
    val customFrom:    String = "",
    val customTo:      String = DateUtils.todayStr(),
    // ── Pre-computed on Dispatchers.Default ──────────────────────────────────
    val filteredTxns:  List<Transaction> = emptyList(),
    val totalIncome:   Long = 0L,
    val totalExpense:  Long = 0L,
    val pieData:       List<PieDatum> = emptyList(),
    val barData:       List<BarDatum> = emptyList(),
    val lifestyle:     LifestyleData? = null,
    val salaryInsight: SalaryInsight? = null,
    val monthlyTrend:  List<MonthDatum> = emptyList(),
    val savingsRate:   Int = 0,   // (income-expense)/income*100, bisa negatif
    val trendMonths:   Int = 6,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val userPrefs:       UserPreferences,
) : ViewModel() {

    private val _typeFilter   = MutableStateFlow("all")
    private val _dateFilter   = MutableStateFlow("month")
    private val _customFrom   = MutableStateFlow("")
    private val _customTo     = MutableStateFlow(DateUtils.todayStr())
    private val _trendMonths  = MutableStateFlow(6)

    private val _filterState = combine(_typeFilter, _dateFilter, _customFrom, _customTo) {
        type, date, from, to -> arrayOf(type, date, from, to)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        // debounce: batch imports won't trigger 100 full recomputes
        transactionRepo.observeAll().debounce(300),
        userPrefs.appPrefsFlow,
        _filterState,
        _trendMonths,
    ) { txns, prefs, filters, trendMonths ->
        val typeFilter = filters[0]
        val dateFilter = filters[1]
        val customFrom = filters[2]
        val customTo   = filters[3]

        // ── Apply filters (all on Default thread) ─────────────────────────────
        val today      = DateUtils.todayStr()
        val monthStart = today.substring(0, 7) + "-01"
        val weekStart  = java.time.LocalDate.now().minusDays(6).toString()
        val yearStart  = today.substring(0, 4) + "-01-01"

        var list = txns.filter { it.category != "Penyesuaian Saldo" }
        list = when (typeFilter) {
            "income"   -> list.filter { it.type == TransactionType.income }
            "expense"  -> list.filter { it.type == TransactionType.expense }
            "transfer" -> list.filter { it.type == TransactionType.transfer }
            else       -> list
        }
        list = when (dateFilter) {
            "today"  -> list.filter { it.date == today }
            "week"   -> list.filter { it.date >= weekStart }
            "month"  -> list.filter { it.date >= monthStart }
            "year"   -> list.filter { it.date >= yearStart }
            "custom" -> if (customFrom.isNotEmpty()) list.filter { it.date >= customFrom && it.date <= customTo } else list
            else     -> list
        }

        val totalInc = list.filter { it.type == TransactionType.income  }.sumOf { it.amount }
        val totalExp = list.filter { it.type == TransactionType.expense }.sumOf { it.amount }

        // ── Pie data ──────────────────────────────────────────────────────────
        val pieData = list.filter { it.type == TransactionType.expense }
            .groupBy { it.category }
            .entries.sortedByDescending { e -> e.value.sumOf { t -> t.amount } }
            .map { (cat, txList) ->
                val value = txList.sumOf { it.amount }
                PieDatum(cat, value, if (totalExp > 0) (value * 100 / totalExp).toInt() else 0)
            }

        // ── Bar data (always current month, not affected by user filter) ──────
        val barWeeks = mutableMapOf(
            "Mg 1" to longArrayOf(0, 0),
            "Mg 2" to longArrayOf(0, 0),
            "Mg 3" to longArrayOf(0, 0),
            "Mg 4" to longArrayOf(0, 0),
        )
        txns.filter { it.date >= monthStart }.forEach { t ->
            val day = t.date.substringAfterLast("-").toIntOrNull() ?: 1
            val key = when { day <= 7 -> "Mg 1"; day <= 14 -> "Mg 2"; day <= 21 -> "Mg 3"; else -> "Mg 4" }
            barWeeks[key]?.let { arr ->
                when (t.type) {
                    TransactionType.income  -> arr[0] += t.amount
                    TransactionType.expense -> arr[1] += t.amount
                    else -> {}
                }
            }
        }
        val barData = barWeeks.entries.map { BarDatum(it.key, it.value[0], it.value[1]) }

        // ── Lifestyle + salary (skip if no expense data) ────────────────────
        // computeLifestyle & computeSalaryInsight do heavy string matching;
        // guard early so they are not called on every filter toggle.
        val lifestyle     = if (totalExp > 0) computeLifestyle(list, totalExp) else null
        val salaryInsight = if (totalExp > 0) computeSalaryInsight(list, prefs.userProfile.job) else null

        // ── Monthly trend (last N months, not affected by user filter) ────────
        val monthlyTrend = computeMonthlyTrend(txns, trendMonths)

        // ── Savings rate (based on filtered period) ───────────────────────────
        val savingsRate = if (totalInc > 0)
            ((totalInc - totalExp) * 100 / totalInc).toInt()
        else 0

        AnalyticsUiState(
            allTxns       = txns,
            userJob       = prefs.userProfile.job,
            userAge       = prefs.userProfile.age,
            typeFilter    = typeFilter,
            dateFilter    = dateFilter,
            customFrom    = customFrom,
            customTo      = customTo,
            filteredTxns  = list,
            totalIncome   = totalInc,
            totalExpense  = totalExp,
            pieData       = pieData,
            barData       = barData,
            lifestyle     = lifestyle,
            salaryInsight = salaryInsight,
            monthlyTrend  = monthlyTrend,
            savingsRate   = savingsRate,
            trendMonths   = trendMonths,
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun setTypeFilter(v: String) { _typeFilter.value = v }
    fun setDateFilter(v: String) { _dateFilter.value = v }
    fun setCustomFrom(v: String) { _customFrom.value = v }
    fun setCustomTo(v: String)   { _customTo.value   = v }
    fun setTrendMonths(v: Int)   { _trendMonths.value = v }
}

// ── Pure computation (runs on Dispatchers.Default inside combine) ─────────────

private fun computeLifestyle(txns: List<Transaction>, totalExp: Long): LifestyleData? {
    if (totalExp == 0L) return null
    val byCat = txns.filter { it.type == TransactionType.expense }
        .groupBy { it.category }
        .mapValues { e -> e.value.sumOf { it.amount } }
    val top3 = byCat.entries.sortedByDescending { it.value }.take(3)
        .map { PieDatum(it.key, it.value, if (totalExp > 0) (it.value * 100 / totalExp).toInt() else 0) }
    val topCat = top3.firstOrNull()?.category ?: "Lainnya"

    val (title, desc) = when (topCat) {
        "Makan & Minum"  -> "The Foodie" to "Kebahagiaanmu sering datang dari makan enak. Mantap, tapi tetap jaga limit."
        "Belanja Online" -> "The Online Hunter" to "Cepat, praktis, dan sering checkout. Kamu jago cari barang, tapi rawan impulsif."
        "Transportasi"   -> "The Road Runner" to "Mobilitas tinggi. Kamu punya ritme jalan-jalan yang konsisten."
        "Hiburan"        -> "The Entertainer" to "Kamu tahu cara recharge. Pastikan hiburan tetap jadi hadiah, bukan kebiasaan mahal."
        "Tagihan"        -> "The Adulting Pro" to "Tagihan aman, hidup stabil. Kamu serius dan bertanggung jawab."
        "Kesehatan"      -> "The Wellness Keeper" to "Kamu investasi ke kesehatan. Good choice."
        else             -> "The Minimalist" to "Kamu hemat dan terarah. Pengeluaranmu rapi dan fungsional."
    }

    val impulsive = listOf("Belanja Online","Hiburan","Perawatan")
        .sumOf { byCat[it] ?: 0L } * 100 / totalExp
    val funFact = when {
        impulsive >= 45 -> "🛒 $impulsive% pengeluaranmu masuk kategori impulsif! Coba tunggu 24 jam sebelum checkout."
        else -> "⚖️ Top pengeluaran: ${top3.getOrNull(0)?.category ?: "-"} (${top3.getOrNull(0)?.pct ?: 0}%). Sudah tahu ke mana uang pergi!"
    }
    val (mTitle, mDesc, mBadge) = if (impulsive >= 45)
        Triple("🚫 No-Checkout 7 Hari", "Boleh masukin ke wishlist, tapi jangan klik bayar dulu selama 7 hari.", "Penguasa Keinginan 🏆")
    else
        Triple("🔬 Audit Pengeluaran Kecil", "7 hari ke depan, catat SEMUA pengeluaran di bawah Rp20.000 — hasilnya sering mengejutkan!", "Micro-Tracker 🔍")

    return LifestyleData(title, desc, top3, funFact, mTitle, mDesc, mBadge)
}

private fun String.containsAny(vararg keywords: String) = keywords.any { this.contains(it) }

private val MONTH_ID = listOf("Jan","Feb","Mar","Apr","Mei","Jun","Jul","Ags","Sep","Okt","Nov","Des")

private fun computeMonthlyTrend(txns: List<Transaction>, months: Int): List<MonthDatum> {
    val today = java.time.LocalDate.now()
    return (months - 1 downTo 0).map { offset ->
        val month  = today.minusMonths(offset.toLong())
        val prefix = month.toString().substring(0, 7)          // "YYYY-MM"
        val label  = MONTH_ID[month.monthValue - 1]            // "Jan", "Feb", ...
        val mt = txns.filter { it.date.startsWith(prefix) && it.category != "Penyesuaian Saldo" }
        val inc = mt.filter { it.type == TransactionType.income  }.sumOf { it.amount }
        val exp = mt.filter { it.type == TransactionType.expense }.sumOf { it.amount }
        MonthDatum(label, inc, exp, inc - exp)
    }
}

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
    val jobLower = userJob.lowercase()
    val career = when {
        jobLower.containsAny("pelajar","mahasiswa","siswa","student") ->
            "Mulai part-time/freelance di bidang minatmu sambil kuliah. Bangun portofolio sejak dini."
        jobLower.containsAny("engineer","programmer","developer","software","backend","frontend","fullstack","devops","data scientist","ml engineer","android","ios") ->
            if (ideal < 15_000_000) "Dalami satu stack/domain (AI, cloud, fintech). Remote job internasional bisa 3–5x gaji lokal."
            else "Lead engineer atau tech lead. Pertimbangkan startup equity atau freelance internasional."
        jobLower.containsAny("desainer","designer","ui","ux","graphic","visual","ilustrator","motion") ->
            if (ideal < 10_000_000) "Bangun portofolio Behance/Dribbble. Klien internasional via Upwork bisa bayar 5–10x rate lokal."
            else "Pertimbangkan art direction, design lead, atau agency sendiri."
        jobLower.containsAny("dokter","perawat","bidan","apoteker","tenaga kesehatan","nakes","fisioterapi","radiologi") ->
            "Spesialisasi dan sertifikasi kompetensi adalah kunci. Pertimbangkan klinik swasta atau praktik mandiri."
        jobLower.containsAny("guru","dosen","pengajar","tutor","instruktur","pendidik") ->
            "Tambah passive income via kursus online (Udemy, Skill Academy). Sertifikasi profesi meningkatkan tunjangan."
        jobLower.containsAny("marketing","sales","penjualan","account executive","business development") ->
            "Bangun network dan track record closing. Sales dengan komisi tinggi sering outperform gaji tetap."
        jobLower.containsAny("content creator","youtuber","streamer","influencer","creator") ->
            "Diversifikasi platform dan monetisasi: sponsorship, merchandise, course. Konsistensi adalah aset terbesar."
        jobLower.containsAny("akuntan","accounting","finance","keuangan","auditor","pajak","tax") ->
            "Sertifikasi CPA, CFA, atau Brevet Pajak meningkatkan value signifikan. Pertimbangkan konsultan independen."
        jobLower.containsAny("freelancer","freelance","konsultan","consultant") ->
            "Naikkan rate per proyek, bangun personal brand, dan pertimbangkan niche khusus untuk bayaran premium."
        jobLower.containsAny("wirausaha","wiraswasta","pengusaha","entrepreneur","owner","ceo","founder") ->
            "Fokus pada satu revenue stream yang profitable sebelum scaling. Pisahkan keuangan pribadi dan bisnis."
        jobLower.containsAny("karyawan","staff","staf","pegawai","administrasi") ->
            if (ideal < 10_000_000) "Tingkatkan skill teknis. Negosiasi kenaikan gaji tahunan atau pindah ke perusahaan lebih besar."
            else "Incaran posisi manajerial atau spesialis senior."
        jobLower.containsAny("ibu rumah tangga","irt","homemaker") ->
            "Pertimbangkan peluang income dari rumah: jualan online, les privat, atau jasa freelance."
        else ->
            "Cari peluang naik jabatan, sertifikasi, atau pindah ke industri dengan rata-rata gaji lebih tinggi."
    }
    return SalaryInsight(living, ideal, level, career, free)
}
