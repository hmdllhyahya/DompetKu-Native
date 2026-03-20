package com.dompetku.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.dompetku.DompetKuApp
import com.dompetku.MainActivity
import com.dompetku.R
import com.dompetku.data.preferences.UserPreferences
import com.dompetku.data.repository.TransactionRepository
import com.dompetku.domain.model.TransactionType
import com.dompetku.util.CurrencyFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Smart budget reminder worker.
 * Runs 5 times per day at distinct slots, each with context-aware analysis:
 *   SLOT_MORNING  (06:00) — daily budget briefing + transport estimate
 *   SLOT_LUNCH    (12:00) — mid-day check: how much spent, budget remaining
 *   SLOT_RETURN   (13:00) — back to work: spending so far + transport reserve reminder
 *   SLOT_COMMUTE  (17:00) — commute time: budget vs. transport cost check
 *   SLOT_SUMMARY  (21:00) — end-of-day summary
 *
 * Transport pattern is derived from the last 60 days of transaction history:
 * any category matching TRANSPORT_KEYWORDS counts as a transport expense.
 * Frequency ≥ 20% of days → user is considered a regular commuter.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userPrefs: UserPreferences,
    private val txnRepo: TransactionRepository,
) : CoroutineWorker(context, workerParams) {

    /**
     * Required for setExpedited() — provides a foreground service notification
     * as fallback on older Android versions that need it.
     */
    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        val notif = androidx.core.app.NotificationCompat.Builder(context, DompetKuApp.CHANNEL_BUDGET_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("DompetKu")
            .setContentText("Menyiapkan notifikasi...")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()
        return androidx.work.ForegroundInfo(9999, notif)
    }

    override suspend fun doWork(): Result {
        val prefs = userPrefs.appPrefsFlow.first()
        if (!prefs.notifEnabled) return Result.success()

        val slot  = inputData.getInt(KEY_SLOT, SLOT_SUMMARY)
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        // ── Today's transactions ──────────────────────────────────────────────
        val todayTxns   = txnRepo.transactionsInRange(today, today).first()
        val todayExpense = todayTxns.filter { it.type == TransactionType.expense }.sumOf { it.amount }
        val todayIncome  = todayTxns.filter { it.type == TransactionType.income  }.sumOf { it.amount }

        // ── Daily budget ──────────────────────────────────────────────────────
        val monthlyBudget = prefs.monthlyBudget
        val daysInMonth   = LocalDate.now().lengthOfMonth()
        val dailyBudget   = if (monthlyBudget > 0) monthlyBudget / daysInMonth else 0L
        val budgetLeft    = (dailyBudget - todayExpense).coerceAtLeast(0L)

        // ── Transport pattern (last 60 days) ──────────────────────────────────
        val sixtyAgo    = LocalDate.now().minusDays(60).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val recentTxns  = txnRepo.transactionsInRange(sixtyAgo, today).first()
        val transportTxns = recentTxns.filter {
            it.type == TransactionType.expense && isTransport(it.category)
        }
        val transportDays      = transportTxns.map { it.date }.toSet().size
        val isRegularCommuter  = transportDays.toFloat() / 60f >= 0.20f   // ≥20% of days
        val avgTransport       = if (transportTxns.isNotEmpty())
            transportTxns.sumOf { it.amount } / transportTxns.size else 0L

        // ── Build notification ────────────────────────────────────────────────
        val name    = prefs.userProfile.name.trim().takeIf { it.isNotEmpty() }
        val hi      = if (name != null) "Hei, $name!" else "Hei!"

        val (title, body) = when (slot) {
            SLOT_MORNING -> morning(hi, dailyBudget, isRegularCommuter, avgTransport)
            SLOT_LUNCH   -> lunch(hi, dailyBudget, todayExpense, budgetLeft)
            SLOT_RETURN  -> returnWork(todayExpense, dailyBudget, budgetLeft, isRegularCommuter, avgTransport)
            SLOT_COMMUTE -> commute(hi, dailyBudget, todayExpense, budgetLeft, isRegularCommuter, avgTransport)
            else         -> summary(hi, todayExpense, todayIncome, dailyBudget)
        }

        post(notifId = NOTIF_BASE + slot, title = title, body = body)
        return Result.success()
    }

    // ── Slot builders ─────────────────────────────────────────────────────────

    /** 06:00 — Morning briefing: daily budget + transport estimate */
    private fun morning(
        hi: String, dailyBudget: Long,
        isCommuter: Boolean, avgTransport: Long,
    ): Pair<String, String> {
        val title = "Selamat pagi! ☀️"
        val body = when {
            dailyBudget <= 0L -> {
                "$hi Belum ada budget bulanan yang disetel. Atur di halaman Budget dulu ya!"
            }
            isCommuter && avgTransport > 0 -> {
                val afterTransport = dailyBudget - avgTransport
                val budgetStr    = CurrencyFormatter.format(dailyBudget)
                val transportStr = CurrencyFormatter.format(avgTransport)
                if (afterTransport >= 0) {
                    val afterStr = CurrencyFormatter.format(afterTransport)
                    "$hi Budget harimu $budgetStr. Biasanya kamu pakai ~$transportStr untuk transport, " +
                    "jadi sisa untuk kebutuhan lain $afterStr. Semangat! 💪"
                } else {
                    "$hi Budget harimu $budgetStr, tapi transport biasanya $transportStr. " +
                    "Pertimbangkan naikkan budget bulananmu ya!"
                }
            }
            else -> {
                val budgetStr = CurrencyFormatter.format(dailyBudget)
                "$hi Budget harimu $budgetStr. Semoga hari ini produktif dan pengeluaran terkontrol! 💪"
            }
        }
        return title to body
    }

    /** 12:00 — Lunch: mid-day spending check */
    private fun lunch(
        hi: String, dailyBudget: Long,
        todayExpense: Long, budgetLeft: Long,
    ): Pair<String, String> {
        val title = "Waktu istirahat! 🍱"
        val body = when {
            dailyBudget <= 0L -> {
                "Lagi istirahat? Jangan lupa catat pengeluaran makan siangmu di DompetKu!"
            }
            todayExpense == 0L -> {
                val budgetStr = CurrencyFormatter.format(dailyBudget)
                "$hi Budget harian $budgetStr, belum ada pengeluaran tercatat. Jangan lupa catat makan siang ya!"
            }
            budgetLeft <= 0L -> {
                val overStr   = CurrencyFormatter.format(todayExpense - dailyBudget)
                val budgetStr = CurrencyFormatter.format(dailyBudget)
                "$hi Budget harian $budgetStr sudah terlampaui $overStr. Pertimbangkan hemat untuk sisa hari ini! ⚠️"
            }
            else -> {
                val spentStr  = CurrencyFormatter.format(todayExpense)
                val leftStr   = CurrencyFormatter.format(budgetLeft)
                "$hi Sudah terpakai $spentStr, sisa budget hari ini $leftStr. Tetap bijak saat makan siang ya!"
            }
        }
        return title to body
    }

    /** 13:00 — Back to work: spending recap + transport reserve reminder */
    private fun returnWork(
        todayExpense: Long, dailyBudget: Long, budgetLeft: Long,
        isCommuter: Boolean, avgTransport: Long,
    ): Pair<String, String> {
        val title = "Kembali produktif! 💼"
        val spentStr = CurrencyFormatter.format(todayExpense)
        val body = when {
            dailyBudget <= 0L -> {
                "Pengeluaran sejauh ini $spentStr. Catat semua transaksi agar keuangan tetap rapi!"
            }
            else -> {
                val leftStr = CurrencyFormatter.format(budgetLeft)
                val base    = "Pengeluaran pagi–siang ini $spentStr. Sisa budget $leftStr."
                val transportNote = if (isCommuter && avgTransport > 0) {
                    val tStr = CurrencyFormatter.format(avgTransport)
                    " Sisihkan ~$tStr untuk transport pulang nanti."
                } else ""
                base + transportNote
            }
        }
        return title to body
    }

    /** 17:00 — Commute home: can the remaining budget cover transport? */
    private fun commute(
        hi: String, dailyBudget: Long,
        todayExpense: Long, budgetLeft: Long,
        isCommuter: Boolean, avgTransport: Long,
    ): Pair<String, String> {
        val title = "Saatnya pulang! 🏠"
        val body = when {
            dailyBudget <= 0L -> {
                val spentStr = CurrencyFormatter.format(todayExpense)
                "Pengeluaran hari ini $spentStr. Perjalanan pulang yang aman!"
            }
            budgetLeft <= 0L -> {
                val overStr = CurrencyFormatter.format(todayExpense - dailyBudget)
                "Budget harian sudah terlampaui $overStr. Coba hemat untuk keperluan transport pulang ya! ⚠️"
            }
            isCommuter && avgTransport > 0 -> {
                val leftStr      = CurrencyFormatter.format(budgetLeft)
                val transportStr = CurrencyFormatter.format(avgTransport)
                val diff         = budgetLeft - avgTransport
                val statusEmoji  = when {
                    diff >  avgTransport -> "✅ Masih ada sisa!"
                    diff >= 0            -> "✅ Cukup pas."
                    else                 -> "⚠️ Kurang, siapkan dari pos lain ya."
                }
                "$hi Sisa budget $leftStr, transport pulang biasanya $transportStr. $statusEmoji"
            }
            else -> {
                val leftStr = CurrencyFormatter.format(budgetLeft)
                "$hi Sisa budget hari ini $leftStr. Perjalanan pulang yang aman! 🙌"
            }
        }
        return title to body
    }

    /** 21:00 — Daily summary */
    private fun summary(
        hi: String, todayExpense: Long,
        todayIncome: Long, dailyBudget: Long,
    ): Pair<String, String> {
        val title   = "Ringkasan hari ini 📊"
        val expStr  = CurrencyFormatter.format(todayExpense)
        val incNote = if (todayIncome > 0) " Pemasukan: ${CurrencyFormatter.format(todayIncome)}." else ""
        val body = when {
            dailyBudget <= 0L -> {
                "Pengeluaran hari ini $expStr.$incNote Tetap catat semua transaksi ya!"
            }
            todayExpense == 0L -> {
                "$hi Belum ada pengeluaran tercatat hari ini.$incNote Jangan lupa catat kalau ada yang terlewat!"
            }
            todayExpense <= dailyBudget * 0.79 -> {
                "$hi Pengeluaran $expStr — hemat banget hari ini! 🌟$incNote"
            }
            todayExpense <= dailyBudget -> {
                "$hi Pengeluaran $expStr — masih dalam budget 👍$incNote"
            }
            else -> {
                val budgetStr = CurrencyFormatter.format(dailyBudget)
                "$hi Pengeluaran $expStr melewati budget harian $budgetStr ⚠️$incNote Besok lebih hati-hati ya!"
            }
        }
        return title to body
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isTransport(category: String): Boolean {
        val lower = category.lowercase()
        return TRANSPORT_KEYWORDS.any { lower.contains(it) }
    }

    private fun post(notifId: Int, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, DompetKuApp.CHANNEL_BUDGET_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notifId, notif)
        } catch (_: SecurityException) { /* POST_NOTIFICATIONS not granted */ }
    }

    // ── Companion: schedule / cancel ──────────────────────────────────────────

    companion object {
        private const val KEY_SLOT  = "slot"
        const val SLOT_MORNING = 0   // 06:00
        const val SLOT_LUNCH   = 1   // 12:00
        const val SLOT_RETURN  = 2   // 13:00
        const val SLOT_COMMUTE = 3   // 17:00
        const val SLOT_SUMMARY = 4   // 21:00
        private val SLOT_HOURS = listOf(6, 12, 13, 17, 21)

        private const val NOTIF_BASE = 2001
        const val NOTIF_ID = 2001   // legacy compat

        private val WORK_NAMES = listOf(
            "dk_notif_0600", "dk_notif_1200",
            "dk_notif_1300", "dk_notif_1700", "dk_notif_2100",
        )

        private val TRANSPORT_KEYWORDS = listOf(
            "transport", "ojek", "grab", "gojek", "taksi", "taxi",
            "bus", "krl", "mrt", "lrt", "kereta", "angkot",
            "bensin", "bbm", "parkir", "tol", "commute",
            "sepeda", "motor", "brt", "damri",
        )

        /**
         * Schedule (enabled=true) or cancel (enabled=false) all 5 daily workers.
         * Also cancels the old single-worker by its legacy WORK_NAME for clean migration.
         */
        fun schedule(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            // Cancel legacy single-slot worker
            wm.cancelUniqueWork("dompetku_daily_reminder")

            if (!enabled) {
                WORK_NAMES.forEach { wm.cancelUniqueWork(it) }
                return
            }

            SLOT_HOURS.forEachIndexed { idx, hour ->
                val now    = Calendar.getInstance()
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
                }
                val delay = target.timeInMillis - now.timeInMillis

                val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(KEY_SLOT to idx))
                    .build()

                wm.enqueueUniquePeriodicWork(
                    WORK_NAMES[idx],
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            }
        }
    }
}
