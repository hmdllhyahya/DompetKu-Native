package com.dompetku.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Currency ──────────────────────────────────────────────────────────────────
object CurrencyFormatter {
    private val idrLocale = Locale("id", "ID")
    private val fmt = NumberFormat.getNumberInstance(idrLocale)

    /** "Rp 1.500.000" */
    fun format(amount: Long, showSymbol: Boolean = true): String {
        val number = fmt.format(amount)
        return if (showSymbol) "Rp $number" else number
    }

    /** "1.5jt", "750rb", "500" */
    fun compact(amount: Long): String = when {
        amount >= 1_000_000_000 -> "${amount / 1_000_000_000}M"
        amount >= 1_000_000     -> {
            val m = amount / 1_000_000.0
            if (m == m.toLong().toDouble()) "${m.toLong()}jt" else "${"%.1f".format(m)}jt"
        }
        amount >= 1_000         -> "${amount / 1_000}rb"
        else                    -> amount.toString()
    }

    /** Parse "1.500.000" or "1500000" → Long */
    fun parse(input: String): Long =
        input.replace(".", "").replace(",", "").trim().toLongOrNull() ?: 0L
}

// ── Date / Time ───────────────────────────────────────────────────────────────
object DateUtils {
    private val isoFmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFmt  = DateTimeFormatter.ofPattern("HH:mm")
    private val displayFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID"))
    private val headerFmt  = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    private val shortFmt   = DateTimeFormatter.ofPattern("dd MMM", Locale("id", "ID"))

    fun todayStr(): String = LocalDate.now().format(isoFmt)
    fun nowTimeStr(): String = LocalTime.now().format(timeFmt)

    fun formatDisplay(dateStr: String): String =
        runCatching { LocalDate.parse(dateStr, isoFmt).format(displayFmt) }.getOrDefault(dateStr)

    fun formatHeader(dateStr: String): String =
        runCatching { LocalDate.parse(dateStr, isoFmt).format(headerFmt) }.getOrDefault(dateStr)

    fun formatShort(dateStr: String): String =
        runCatching { LocalDate.parse(dateStr, isoFmt).format(shortFmt) }.getOrDefault(dateStr)

    fun isToday(dateStr: String): Boolean = dateStr == todayStr()

    fun daysRemainingInMonth(): Int {
        val today = LocalDate.now()
        val lastDay = today.withDayOfMonth(today.month.length(today.isLeapYear))
        return (lastDay.toEpochDay() - today.toEpochDay()).toInt()
    }

    /** Returns list of ISO date strings for the current week (Mon–Sun) */
    fun currentWeekDates(): List<String> {
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        return (0..6).map { monday.plusDays(it.toLong()).format(isoFmt) }
    }

    /** Greeting based on hour: Pagi / Siang / Sore / Malam */
    fun greeting(): String = when (LocalTime.now().hour) {
        in 5..10  -> "Selamat Pagi"
        in 11..14 -> "Selamat Siang"
        in 15..17 -> "Selamat Sore"
        else      -> "Selamat Malam"
    }
}
