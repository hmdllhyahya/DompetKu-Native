package com.dompetku.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.AccountType
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// ── Result types ──────────────────────────────────────────────────────────────

data class ImportResult(
    val transactions: List<Transaction>,
    val accounts:     List<Account>,
    val errors:       List<String>,
)

// ── Smart Import Data Classes ─────────────────────────────────────────────────

data class SmartImportResult(
    val transactions:     List<SmartTransaction>,
    val detectedAccounts: List<DetectedAccount>,
    val categoryMappings: Map<String, String>,   // rawCategory -> dompetKu category
    val autoSignCount:    Int,                   // txns where type inferred from sign
    val extraFieldCount:  Int,                   // txns with unknown cols merged to note
    val errors:           List<String>,
    val fileInfo:         String,
)

data class SmartTransaction(
    val tempId:           String,
    val suggestedType:    TransactionType,
    val amount:           Long,
    val category:         String,
    val originalCategory: String,
    val note:             String,
    val date:             String,
    val time:             String,
    val rawAccountName:   String,
    val wasSignFlipped:   Boolean,
    val detected:         Boolean = false,  // true = SmartCategoryDetector matched
)

data class DetectedAccount(
    val rawName:          String,
    val transactionCount: Int,
    val suggestedMatch:   Account?,
    val matchScore:       Float,
)

sealed class AccountResolution {
    data class UseExisting(val account: Account) : AccountResolution()
    data class CreateNew(val name: String)       : AccountResolution()
    object Skip                                  : AccountResolution()
}

// ── Manager ───────────────────────────────────────────────────────────────────

@Singleton
class ExportImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // ── EXPORT ────────────────────────────────────────────────────────────────

    suspend fun exportXlsx(
        transactions: List<Transaction>,
        accounts:     List<Account>,
    ): Uri = withContext(Dispatchers.IO) {
        val workbook   = XSSFWorkbook()
        val accountMap = accounts.associateBy { it.id }

        // Sheet 1 — Transaksi
        // Kolom: Tanggal, Waktu, Jenis Transaksi, Nominal, Nama Transaksi,
        //        Kategori, Akun, Dari Akun, Ke Akun, Biaya Admin
        //
        // Format nominal (accounting-style):
        //   Pengeluaran → (14.500)     — kurung = keluar
        //   Pemasukan   → 500.000
        //   Transfer    → ↔ 50.000     — ↔ = perpindahan antar akun
        val txnSheet = workbook.createSheet("Transaksi")
        val txnHeaders = listOf(
            "Tanggal", "Waktu", "Jenis Transaksi", "Nominal", "Nama Transaksi",
            "Kategori", "Akun", "Dari Akun", "Ke Akun", "Biaya Admin",
        )
        txnSheet.createRow(0).writeHeaders(txnHeaders)
        transactions.forEachIndexed { i, t ->
            val accName  = accountMap[t.accountId]?.name ?: ""
            val fromName = t.fromId?.let { accountMap[it]?.name } ?: ""
            val toName   = t.toId?.let   { accountMap[it]?.name } ?: ""
            val typeLabel = when (t.type) {
                TransactionType.income   -> "Pemasukan"
                TransactionType.expense  -> "Pengeluaran"
                TransactionType.transfer -> "Transfer"
            }
            val nominalStr = when (t.type) {
                TransactionType.expense  -> "(${formatRupiah(t.amount)})"
                TransactionType.income   -> formatRupiah(t.amount)
                TransactionType.transfer -> "\u21D4 ${formatRupiah(t.amount)}"
            }
            txnSheet.createRow(i + 1).apply {
                createCell(0).setCellValue(t.date)                           // Tanggal
                createCell(1).setCellValue(t.time)                           // Waktu
                createCell(2).setCellValue(typeLabel)                        // Jenis Transaksi
                createCell(3).setCellValue(nominalStr)                       // Nominal
                createCell(4).setCellValue(t.note)                           // Nama Transaksi
                createCell(5).setCellValue(t.category)                       // Kategori
                createCell(6).setCellValue(accName)                          // Akun
                createCell(7).setCellValue(fromName)                         // Dari Akun
                createCell(8).setCellValue(toName)                           // Ke Akun
                createCell(9).setCellValue(
                    if (t.adminFee > 0) formatRupiah(t.adminFee) else ""    // Biaya Admin
                )
            }
        }

        // Sheet 2 — Akun
        // Hanya info yang berguna untuk manusia — ID internal tidak diekspor
        val accSheet = workbook.createSheet("Akun")
        val accHeaders = listOf("Nama", "Tipe", "Saldo", "Nomor Akhir", "Brand")
        accSheet.createRow(0).writeHeaders(accHeaders)
        accounts.forEachIndexed { i, a ->
            accSheet.createRow(i + 1).apply {
                createCell(0).setCellValue(a.name)
                createCell(1).setCellValue(a.type.name)
                createCell(2).setCellValue(formatRupiah(a.balance))
                createCell(3).setCellValue(a.last4 ?: "")
                createCell(4).setCellValue(a.brandKey ?: "")
            }
        }

        // Write to file
        val outFile = exportFile()
        FileOutputStream(outFile).use { workbook.write(it) }
        workbook.close()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }

    // ── IMPORT ────────────────────────────────────────────────────────────────

    suspend fun importXlsx(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val transactions = mutableListOf<Transaction>()
        val accounts     = mutableListOf<Account>()
        val errors       = mutableListOf<String>()

        val stream = context.contentResolver.openInputStream(uri)
            ?: return@withContext ImportResult(emptyList(), emptyList(), listOf("Gagal membuka file"))

        runCatching {
            val workbook = XSSFWorkbook(stream)

            // ── Parse Transaksi ───────────────────────────────────────────────
            workbook.getSheet("Transaksi")?.let { sheet ->
                val headerRow = sheet.getRow(0) ?: return@let
                val hMap = headerRow.buildHeaderMap()

                for (rowIdx in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIdx) ?: continue
                    if (row.isBlank()) continue
                    runCatching {
                        val id        = row.str(hMap["id"])        ?: error("kolom 'id' kosong")
                        val typeStr   = row.str(hMap["type"])      ?: error("kolom 'type' kosong")
                        val amount    = row.lng(hMap["amount"])    ?: error("kolom 'amount' kosong")
                        val date      = row.str(hMap["date"])      ?: error("kolom 'date' kosong")
                        val accountId = row.str(hMap["accountId"]) ?: error("kolom 'accountId' kosong")
                        val type = runCatching { TransactionType.valueOf(typeStr) }
                            .getOrElse { error("type tidak valid: $typeStr") }
                        transactions.add(
                            Transaction(
                                id        = id,
                                type      = type,
                                amount    = amount,
                                adminFee  = row.lng(hMap["adminFee"]) ?: 0L,
                                category  = row.str(hMap["category"]) ?: "",
                                note      = row.str(hMap["note"])     ?: "",
                                date      = date,
                                time      = row.str(hMap["time"])     ?: "00:00",
                                accountId = accountId,
                                fromId    = row.str(hMap["fromId"])?.takeIf { it.isNotBlank() },
                                toId      = row.str(hMap["toId"])?.takeIf { it.isNotBlank() },
                            )
                        )
                    }.onFailure { e ->
                        errors.add("Transaksi baris ${rowIdx + 1}: ${e.message}")
                    }
                }
            } ?: errors.add("Sheet 'Transaksi' tidak ditemukan")

            // ── Parse Akun ────────────────────────────────────────────────────
            workbook.getSheet("Akun")?.let { sheet ->
                val headerRow = sheet.getRow(0) ?: return@let
                val hMap = headerRow.buildHeaderMap()

                for (rowIdx in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIdx) ?: continue
                    if (row.isBlank()) continue
                    runCatching {
                        val id      = row.str(hMap["id"])   ?: error("kolom 'id' kosong")
                        val typeStr = row.str(hMap["type"]) ?: error("kolom 'type' kosong")
                        val name    = row.str(hMap["name"]) ?: error("kolom 'name' kosong")
                        val type = runCatching { AccountType.valueOf(typeStr) }
                            .getOrElse { error("type tidak valid: $typeStr") }
                        accounts.add(
                            Account(
                                id            = id,
                                type          = type,
                                name          = name,
                                balance       = row.lng(hMap["balance"])       ?: 0L,
                                last4         = row.str(hMap["last4"])?.takeIf { it.isNotBlank() },
                                brandKey      = row.str(hMap["brandKey"])?.takeIf { it.isNotBlank() },
                                gradientStart = row.lng(hMap["gradientStart"]) ?: 0L,
                                gradientEnd   = row.lng(hMap["gradientEnd"])   ?: 0L,
                                sortOrder     = row.lng(hMap["sortOrder"])?.toInt() ?: 0,
                            )
                        )
                    }.onFailure { e ->
                        errors.add("Akun baris ${rowIdx + 1}: ${e.message}")
                    }
                }
            } ?: errors.add("Sheet 'Akun' tidak ditemukan")

            workbook.close()
        }.onFailure { e ->
            errors.add("Error membaca file: ${e.message}")
        }

        stream.close()
        ImportResult(transactions, accounts, errors)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Format Long ke string ribuan Indonesia tanpa prefix Rp. Contoh: 14500 → "14.500" */
    private fun formatRupiah(amount: Long): String {
        if (amount == 0L) return "0"
        val s  = amount.toString()
        val sb = StringBuilder()
        s.reversed().forEachIndexed { i, c ->
            if (i > 0 && i % 3 == 0) sb.append('.')
            sb.append(c)
        }
        return sb.reverse().toString()
    }

    private fun exportFile(): File {
        val dir = (context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir)
            .let { File(it, "DompetKu") }
            .also { it.mkdirs() }
        val date = LocalDate.now().toString().replace("-", "")
        return File(dir, "dompetku_export_$date.xlsx")
    }

    private fun Row.buildHeaderMap(): Map<String, Int> = buildMap {
        (0 until lastCellNum).forEach { i ->
            getCell(i)?.stringCellValue?.trim()?.takeIf { it.isNotBlank() }?.let { put(it, i) }
        }
    }

    private fun Row.isBlank(): Boolean =
        (0 until lastCellNum).all { i ->
            val cell = getCell(i) ?: return@all true
            cell.cellType == CellType.BLANK ||
                (cell.cellType == CellType.STRING && cell.stringCellValue.isBlank())
        }

    private fun Row.str(colIdx: Int?): String? {
        colIdx ?: return null
        val cell = getCell(colIdx) ?: return null
        return when (cell.cellType) {
            CellType.STRING  -> cell.stringCellValue.trim().takeIf { it.isNotBlank() }
            CellType.NUMERIC -> cell.numericCellValue.toLong().toString()
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else             -> null
        }
    }

    private fun Row.lng(colIdx: Int?): Long? {
        colIdx ?: return null
        val cell = getCell(colIdx) ?: return null
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue.toLong()
            CellType.STRING  -> cell.stringCellValue.trim().toLongOrNull()
            else             -> null
        }
    }

    private fun Row.writeHeaders(headers: List<String>) {
        headers.forEachIndexed { i, h -> createCell(i).setCellValue(h) }
    }

    // ── SMART IMPORT ──────────────────────────────────────────────────────────────

    suspend fun smartImportXlsx(
        uri:              Uri,
        existingAccounts: List<Account>,
    ): SmartImportResult = withContext(Dispatchers.IO) {
        val transactions    = mutableListOf<SmartTransaction>()
        val errors          = mutableListOf<String>()
        val accountRawNames = mutableMapOf<String, Int>()
        var autoSignCount   = 0
        var extraFieldCount = 0
        val sheetInfoParts  = mutableListOf<String>()

        val stream = context.contentResolver.openInputStream(uri)
            ?: return@withContext SmartImportResult(
                emptyList(), emptyList(), emptyMap(), 0, 0,
                listOf("Gagal membuka file"), "Error"
            )

        runCatching {
            val workbook = XSSFWorkbook(stream)
            for (sheetIdx in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIdx)

                // Find first header-like row (scan up to row 5)
                val headerRowIdx = (0..minOf(5, sheet.lastRowNum)).firstOrNull { idx ->
                    val r = sheet.getRow(idx) ?: return@firstOrNull false
                    (0 until r.lastCellNum).any { i ->
                        r.getCell(i)?.cellType == CellType.STRING &&
                            !r.getCell(i)?.stringCellValue.isNullOrBlank()
                    }
                } ?: continue

                val headerRow = sheet.getRow(headerRowIdx) ?: continue
                val colMap    = SmartImportEngine.detectColumns(headerRow)

                val hasAmount = colMap.amount != null ||
                    (colMap.debit != null && colMap.credit != null)
                if (!hasAmount) {
                    sheetInfoParts.add("'${sheet.sheetName}': dilewati (tidak ada kolom nominal)")
                    continue
                }

                var rowCount = 0
                for (rowIdx in (headerRowIdx + 1)..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIdx) ?: continue
                    val allBlank = (0 until row.lastCellNum).all { i ->
                        val c = row.getCell(i)
                        c == null || c.cellType == CellType.BLANK ||
                            (c.cellType == CellType.STRING && c.stringCellValue.isBlank())
                    }
                    if (allBlank) continue

                    runCatching {
                        val txn = SmartImportEngine.parseRow(row, colMap, rowIdx)
                            ?: return@runCatching
                        transactions.add(txn)
                        accountRawNames[txn.rawAccountName] =
                            (accountRawNames[txn.rawAccountName] ?: 0) + 1
                        if (txn.wasSignFlipped) autoSignCount++
                        if (txn.note.contains("[") && txn.note.contains(": ")) extraFieldCount++
                        rowCount++
                    }.onFailure { e -> errors.add("Baris ${rowIdx + 1}: ${e.message}") }
                }
                sheetInfoParts.add("'${sheet.sheetName}': $rowCount baris")
            }
            workbook.close()
        }.onFailure { e -> errors.add("Gagal baca file: ${e.message}") }
        stream.close()

        // BUG-04: merge Transfer In/Out pairs dari Money Manager
        val mergedTransactions = SmartImportEngine.mergeTransferPairs(transactions)

        val detectedAccounts = accountRawNames.map { (rawName, count) ->
            val (match, score) = SmartImportEngine.matchAccount(rawName, existingAccounts)
            DetectedAccount(rawName, count, match, score)
        }
        val categoryMappings = transactions
            .map { it.originalCategory }.filter { it.isNotBlank() }.distinct()
            .associateWith { SmartImportEngine.mapCategory(it) }

        SmartImportResult(
            transactions     = mergedTransactions,
            detectedAccounts = detectedAccounts,
            categoryMappings = categoryMappings,
            autoSignCount    = autoSignCount,
            extraFieldCount  = extraFieldCount,
            errors           = errors,
            fileInfo         = sheetInfoParts.joinToString(" | ")
                .ifBlank { "${transactions.size} transaksi ditemukan" },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Smart Import Engine — stateless pure logic
// ─────────────────────────────────────────────────────────────────────────────

internal object SmartImportEngine {

    // ── Column map ────────────────────────────────────────────────────────────

    data class ColumnMap(
        val date:     Int? = null,
        val time:     Int? = null,
        val amount:   Int? = null,
        val debit:    Int? = null,
        val credit:   Int? = null,
        val category: Int? = null,
        val txnName:  Int? = null,   // Nama Transaksi — maps to Transaction.note
        val note:     Int? = null,   // Catatan / keterangan — appended as extra info
        val account:  Int? = null,
        val txnType:  Int? = null,
        val extras:   Map<Int, String> = emptyMap(),
    )

    // Kolom yang dikenal tapi sengaja diabaikan — TIDAK di-append ke note
    private val IGNORED_HEADERS = setOf(
        "currency", "mata uang", "currencies", "foreign currency",
        "id", "uuid", "ref", "reference", "no", "num", "number", "#",
        "subkategori", "subcategory", "sub category", "sub-category",
        "creator", "created_by", "modified", "modified_by",
        "recurring", "repeat", "attachment", "image", "photo",
        "exchange rate", "kurs", "foreign amount", "converted amount",
        "status", "cleared", "reconciled",
    )

    // Kategori yang selalu income
    private val INCOME_CATEGORIES = setOf(
        "gaji", "salary", "wage", "payroll", "upah", "honorarium",
        "freelance", "project income", "jasa",
        "hadiah", "gift", "bonus",
        "investasi", "invest", "dividend", "dividen", "profit", "return",
        "pemasukan", "income", "pendapatan", "revenue",
        "cashback", "refund", "reimburse",
    )

    // Keyword yang menandakan transfer
    private val TRANSFER_KEYWORDS = setOf(
        "transfer", "transfer out", "transfer in", "pindah", "kirim",
        "top up", "topup", "isi saldo", "tarik tunai", "withdraw",
        "antar rekening", "antar akun",
    )

    private enum class Role { DATE, TIME, AMOUNT, DEBIT, CREDIT, CATEGORY, TXNNAME, NOTE, ACCOUNT, TYPE }

    private val ROLE_KEYWORDS: Map<Role, Set<String>> = mapOf(
        Role.DATE     to setOf("date","tanggal","tgl","waktu","datetime","created","timestamp","created_at"),
        Role.TIME     to setOf("time","jam","hour","pukul"),
        Role.AMOUNT   to setOf("amount","nominal","jumlah","total","nilai","uang","harga","money","sum"),
        Role.DEBIT    to setOf("debit","expense","pengeluaran","keluar","out","minus","withdraw","spent","debet"),
        Role.CREDIT   to setOf("credit","income","pemasukan","masuk","in","plus","deposit","received","kredit"),
        Role.CATEGORY to setOf("category","kategori","kat","grup","group","subcategory","class"),
        // TXNNAME: nama/judul transaksi — sebelumnya salah masuk ke NOTE
        Role.TXNNAME  to setOf("nama transaksi","transaction name","nama","judul","title",
                               "merchant","toko","store","payee","penerima","nama item","item","barang"),
        // NOTE: murni catatan/keterangan — tidak termasuk nama transaksi
        Role.NOTE     to setOf("note","catatan","keterangan","memo","description","deskripsi",
                               "remark","ket","detail","narasi"),
        Role.ACCOUNT  to setOf("account","akun","wallet","dompet","source","from","sumber","rekening","account_name"),
        Role.TYPE     to setOf("type","tipe","direction","flow","in_out","income_expense",
                              "jenis_transaksi","income/expense","expense/income","transaction type",
                              "jenis transaksi","jenis","kind","inout","in/out"),
    )

    fun detectColumns(headerRow: Row): ColumnMap {
        val n       = headerRow.lastCellNum.toInt().coerceAtLeast(0)
        val headers = (0 until n).map { i ->
            // Safely handle non-string header cells
            val cell = headerRow.getCell(i) ?: return@map ""
            runCatching { cell.stringCellValue?.trim()?.lowercase() ?: "" }.getOrDefault("")
        }

        fun scoreFor(h: String, role: Role): Float {
            if (h.isBlank()) return 0f
            val kws = ROLE_KEYWORDS[role] ?: return 0f
            if (h in kws) return 1.0f
            // Bug fix: only do substring check for keywords >= 3 chars.
            // Short keywords like "in" / "out" cause false positives (e.g. nom*in*al → CREDIT)
            if (kws.any { it.length >= 3 && (h.contains(it) || it.contains(h)) }) return 0.7f
            if (h.split(Regex("[_\\s\\-/]+")).any { it in kws }) return 0.6f
            return 0f
        }

        val assigned = mutableMapOf<Role, Int>()
        val claimed  = mutableSetOf<Int>()
        // TXNNAME sebelum NOTE agar "Nama Transaksi" tidak salah diklaim sebagai catatan
        val priority = listOf(Role.DATE, Role.TXNNAME, Role.DEBIT, Role.CREDIT, Role.AMOUNT,
                              Role.CATEGORY, Role.NOTE, Role.ACCOUNT, Role.TYPE, Role.TIME)
        for (role in priority) {
            val best = (0 until n).filter { it !in claimed }
                .maxByOrNull { scoreFor(headers[it], role) }
            if (best != null && scoreFor(headers[best], role) > 0.3f) {
                assigned[role] = best
                claimed.add(best)
            }
        }

        val hasDebitCredit = assigned.containsKey(Role.DEBIT) && assigned.containsKey(Role.CREDIT)
        val extras = (0 until n).filter { it !in claimed }
            .associateWith { i ->
                headerRow.getCell(i)?.stringCellValue?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Kolom${i + 1}"
            }

        return ColumnMap(
            date     = assigned[Role.DATE],
            time     = assigned[Role.TIME],
            amount   = if (hasDebitCredit) null else assigned[Role.AMOUNT],
            debit    = if (hasDebitCredit) assigned[Role.DEBIT] else null,
            credit   = if (hasDebitCredit) assigned[Role.CREDIT] else null,
            category = assigned[Role.CATEGORY],
            txnName  = assigned[Role.TXNNAME],
            note     = assigned[Role.NOTE],
            account  = assigned[Role.ACCOUNT],
            txnType  = assigned[Role.TYPE],
            extras   = extras,
        )
    }

    // ── Row parsing ───────────────────────────────────────────────────────────

    fun parseRow(row: Row, colMap: ColumnMap, rowIdx: Int): SmartTransaction? {
        val amount: Long
        val type:   TransactionType
        var wasSignFlipped = false

        if (colMap.debit != null && colMap.credit != null) {
            val dv = row.dbl(colMap.debit)  ?: 0.0
            val cv = row.dbl(colMap.credit) ?: 0.0
            when {
                dv > 0 -> { amount = dv.toLong(); type = TransactionType.expense }
                cv > 0 -> { amount = cv.toLong(); type = TransactionType.income  }
                else   -> return null
            }
        } else {
            val rawAmt = row.dbl(colMap.amount ?: return null) ?: return null
            if (rawAmt == 0.0) return null
            if (rawAmt < 0) {
                amount = (-rawAmt).toLong()
                type   = TransactionType.expense
                wasSignFlipped = true
            } else {
                amount = rawAmt.toLong()
                val ts = colMap.txnType?.let { row.str(it) }?.lowercase()?.trim()
                    ?.takeIf { it != "-" && it != "—" }  // treat dash as null
                val rawCatForType = colMap.category?.let { row.str(it) }?.lowercase()?.trim() ?: ""
                type = when {
                    // Explicit TYPE column values
                    ts != null && ts in setOf("expense","pengeluaran","keluar","out","debit","e","dr","expenses") ->
                        TransactionType.expense
                    ts != null && ts in setOf("income","pemasukan","masuk","in","credit","+","i","cr","incomes") ->
                        TransactionType.income
                    ts != null && ts.contains("transfer") -> TransactionType.transfer
                    // Fallback: infer from category name (BUG-02 fix)
                    TRANSFER_KEYWORDS.any { rawCatForType.contains(it) } -> TransactionType.transfer
                    INCOME_CATEGORIES.any { rawCatForType.contains(it) || rawCatForType == it } -> TransactionType.income
                    // Final fallback
                    else -> TransactionType.expense
                }
            }
        }

        val rawDateStr  = colMap.date?.let { row.str(it) } ?: return null
        val parsedDate  = parseDate(rawDateStr) ?: return null
        // Extract time: prefer dedicated time column, else extract from combined datetime string
        val parsedTime  = colMap.time?.let { row.str(it) }?.let { parseTime(it) }
            ?: extractTimeFromDateStr(rawDateStr)
            ?: "00:00"
        val rawCategory = colMap.category?.let { row.str(it) }?.let { if (it == "-" || it == "—") null else it } ?: ""
        // Nama transaksi — field utama yang menjadi Transaction.note
        val rawTxnName  = colMap.txnName?.let { row.str(it) }?.let { if (it == "-" || it == "—") null else it } ?: ""
        // Catatan murni — appended ke nama transaksi jika ada
        val rawCatatan  = colMap.note?.let { row.str(it) }?.let { if (it == "-" || it == "—") null else it } ?: ""
        val rawAccount  = colMap.account?.let { row.str(it) }
            ?.let { if (it == "-" || it == "—") null else it }
            ?.ifBlank { null } ?: "Akun Impor"

        val extraParts = colMap.extras.mapNotNull { (ci, hdr) ->
            // BUG-03 fix: skip kolom yang ada di ignore list
            if (IGNORED_HEADERS.any { hdr.lowercase().trim() == it || hdr.lowercase().trim().contains(it) })
                return@mapNotNull null
            val v = row.str(ci)?.takeIf { it.isNotBlank() && it != "-" && it != "—" } ?: return@mapNotNull null
            "[$hdr: $v]"
        }

        // Bangun note final:
        // 1. Nama transaksi (primer)
        // 2. Catatan murni (jika ada, dipisah —)
        // 3. Extra fields (kolom yang tidak dikenal)
        val fullNote = buildString {
            if (rawTxnName.isNotBlank()) append(rawTxnName)
            if (rawCatatan.isNotBlank()) {
                if (isNotEmpty()) append(" — ")
                append(rawCatatan)
            }
            if (extraParts.isNotEmpty()) {
                if (isNotEmpty()) append(" ")
                append(extraParts.joinToString(" "))
            }
        }

        // Auto-detect kategori via SmartCategoryDetector (sama persis seperti input manual)
        // Deteksi dijalankan terhadap nama transaksi, bukan catatan
        val sourceForDetection = rawTxnName.ifBlank { fullNote }
        val detectionResult    = SmartCategoryDetector.detect(sourceForDetection)
        val mappedCategory     = mapCategory(rawCategory)
        val finalCategory: String
        val isDetected: Boolean
        if (detectionResult != null) {
            // SmartCategoryDetector menang jika:
            // - kategori asli tidak ada (kosong / Lainnya), ATAU
            // - confidence tinggi (≥ 0.7)
            val useDetected = mappedCategory == "Lainnya" || detectionResult.confidence >= 0.7f
            finalCategory = if (useDetected) detectionResult.category else mappedCategory
            isDetected    = useDetected
        } else {
            finalCategory = mappedCategory
            isDetected    = false
        }

        return SmartTransaction(
            tempId           = "imp_${rowIdx}_${System.currentTimeMillis()}",
            suggestedType    = type,
            amount           = amount,
            category         = finalCategory,
            originalCategory = rawCategory,
            note             = fullNote,
            date             = parsedDate,
            time             = parsedTime,
            rawAccountName   = rawAccount.trim(),
            wasSignFlipped   = wasSignFlipped,
            detected         = isDetected,
        )
    }

    // ── Date parsing ──────────────────────────────────────────────────────────

    fun parseDate(raw: String): String? {
        val s = raw.trim()
        // ISO: 2025-03-07 or with time suffix
        if (s.matches(Regex("\\d{4}-\\d{2}-\\d{2}.*"))) return s.substring(0, 10)
        // Compact: 20250307
        if (s.matches(Regex("\\d{8}"))) return "${s.substring(0,4)}-${s.substring(4,6)}-${s.substring(6,8)}"
        // Separator-based: DD/MM/YYYY, MM/DD/YYYY, YYYY/MM/DD, etc.
        val parts = s.split(Regex("[/\\.\\-]")).map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size >= 3) {
            val a = parts[0].toIntOrNull() ?: return null
            val b = parts[1].toIntOrNull() ?: return null
            val c = parts[2].take(4).toIntOrNull() ?: return null
            return when {
                c > 1900 -> String.format("%04d-%02d-%02d", c, b, a)          // DD/MM/YYYY
                a > 1900 -> String.format("%04d-%02d-%02d", a, b, c)          // YYYY/MM/DD
                a > 31   -> String.format("%04d-%02d-%02d", a + 2000, b, c)   // YY first
                b > 12   -> String.format("%04d-%02d-%02d", c + 2000, a, b)   // day in middle
                else     -> String.format("%04d-%02d-%02d", c + 2000, b, a)   // assume DD/MM/YY
            }
        }
        // Month name: "7 Mar 2025" or "Mar 7, 2025"
        val months = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
            "may" to 5, "mei" to 5, "jun" to 6, "jul" to 7,
            "aug" to 8, "agu" to 8, "sep" to 9, "oct" to 10,
            "okt" to 10, "nov" to 11, "dec" to 12, "des" to 12,
        )
        val lc = s.lowercase()
        for ((name, num) in months) {
            if (lc.contains(name)) {
                val nums = Regex("\\d+").findAll(s).map { it.value.toInt() }.toList()
                val year = nums.firstOrNull { it > 1000 } ?: continue
                val day  = nums.firstOrNull { it in 1..31 && it != year } ?: continue
                return String.format("%04d-%02d-%02d", year, num, day)
            }
        }
        return null
    }

    /**
     * Try to extract a time component from a combined datetime string like
     * "06 Mar 2026 21:25" or "2026-03-06T21:25:00".
     * Returns null if no time is found.
     */
    private fun extractTimeFromDateStr(raw: String): String? {
        // Match HH:mm anywhere in the string (but not a year like 2026)
        val match = Regex("(?<!\\d)(\\d{1,2}):(\\d{2})(?!:\\d{2}|\\d)").find(raw) ?: return null
        val h = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: return null
        if (h > 23 || m > 59) return null
        return String.format("%02d:%02d", h, m)
    }

    private fun parseTime(raw: String): String {
        Regex("(\\d{1,2}):(\\d{2})").find(raw)?.let {
            return String.format("%02d:%02d",
                it.groupValues[1].toIntOrNull() ?: 0,
                it.groupValues[2].toIntOrNull() ?: 0)
        }
        Regex("(\\d{1,2})\\.(\\d{2})").find(raw)?.let {
            val h = it.groupValues[1].toIntOrNull() ?: return "00:00"
            val m = it.groupValues[2].toIntOrNull() ?: return "00:00"
            if (m < 60) return String.format("%02d:%02d", h, m)
        }
        return "00:00"
    }

    // ── Category mapping ──────────────────────────────────────────────────────

    private val CATEGORY_MAP = mapOf(
        // Makan & Minum
        "food" to "Makan & Minum","meal" to "Makan & Minum","makan" to "Makan & Minum",
        "minum" to "Makan & Minum","restaurant" to "Makan & Minum","resto" to "Makan & Minum",
        "cafe" to "Makan & Minum","kafe" to "Makan & Minum","snack" to "Makan & Minum",
        "makanan" to "Makan & Minum","minuman" to "Makan & Minum","drink" to "Makan & Minum",
        "coffee" to "Makan & Minum","kopi" to "Makan & Minum","warung" to "Makan & Minum",
        "kuliner" to "Makan & Minum","dining" to "Makan & Minum","beverages" to "Makan & Minum",
        "groceries" to "Makan & Minum","breakfast" to "Makan & Minum","lunch" to "Makan & Minum",
        "dinner" to "Makan & Minum","sarapan" to "Makan & Minum","makan siang" to "Makan & Minum",
        "makan malam" to "Makan & Minum",
        // Belanja Harian
        "shopping" to "Belanja Harian","belanja" to "Belanja Harian",
        "grocery" to "Belanja Harian","supermarket" to "Belanja Harian",
        "daily" to "Belanja Harian","harian" to "Belanja Harian",
        "pasar" to "Belanja Harian","market" to "Belanja Harian",
        "minimarket" to "Belanja Harian","indomaret" to "Belanja Harian",
        "alfamart" to "Belanja Harian","kebutuhan" to "Belanja Harian",
        // Belanja Online
        "online" to "Belanja Online","ecommerce" to "Belanja Online",
        "marketplace" to "Belanja Online","tokopedia" to "Belanja Online",
        "shopee" to "Belanja Online","lazada" to "Belanja Online",
        // Transportasi
        "transport" to "Transportasi","transportasi" to "Transportasi",
        "commute" to "Transportasi","taxi" to "Transportasi",
        "ojek" to "Transportasi","grab" to "Transportasi","gojek" to "Transportasi",
        "bensin" to "Transportasi","fuel" to "Transportasi","parkir" to "Transportasi",
        "parking" to "Transportasi","bus" to "Transportasi","train" to "Transportasi",
        "krl" to "Transportasi","mrt" to "Transportasi","lrt" to "Transportasi",
        "toll" to "Transportasi","tol" to "Transportasi",
        "flight" to "Transportasi","pesawat" to "Transportasi",
        "vehicle" to "Transportasi","kendaraan" to "Transportasi",
        "commuting" to "Transportasi","travel" to "Transportasi",
        // Hiburan
        "entertainment" to "Hiburan","hiburan" to "Hiburan",
        "movie" to "Hiburan","film" to "Hiburan","cinema" to "Hiburan","bioskop" to "Hiburan",
        "game" to "Hiburan","gaming" to "Hiburan","concert" to "Hiburan","konser" to "Hiburan",
        "music" to "Hiburan","musik" to "Hiburan","hobby" to "Hiburan","hobi" to "Hiburan",
        "recreation" to "Hiburan","rekreasi" to "Hiburan","leisure" to "Hiburan",
        // Tagihan
        "bill" to "Tagihan","tagihan" to "Tagihan","utility" to "Tagihan",
        "utilities" to "Tagihan","electricity" to "Tagihan","listrik" to "Tagihan",
        "water" to "Tagihan","internet" to "Tagihan","phone" to "Tagihan",
        "telepon" to "Tagihan","subscription" to "Tagihan","langganan" to "Tagihan",
        "netflix" to "Tagihan","spotify" to "Tagihan","pulsa" to "Tagihan",
        // Kesehatan
        "health" to "Kesehatan","kesehatan" to "Kesehatan","medical" to "Kesehatan",
        "medis" to "Kesehatan","doctor" to "Kesehatan","dokter" to "Kesehatan",
        "medicine" to "Kesehatan","obat" to "Kesehatan","pharmacy" to "Kesehatan",
        "apotek" to "Kesehatan","hospital" to "Kesehatan","gym" to "Kesehatan",
        "olahraga" to "Kesehatan","sport" to "Kesehatan","fitness" to "Kesehatan",
        // Pendidikan
        "education" to "Pendidikan","pendidikan" to "Pendidikan",
        "school" to "Pendidikan","sekolah" to "Pendidikan","course" to "Pendidikan",
        "kursus" to "Pendidikan","buku" to "Pendidikan","book" to "Pendidikan",
        "kuliah" to "Pendidikan","training" to "Pendidikan","pelatihan" to "Pendidikan",
        // Tempat Tinggal
        "housing" to "Tempat Tinggal","rent" to "Tempat Tinggal","sewa" to "Tempat Tinggal",
        "kost" to "Tempat Tinggal","rumah" to "Tempat Tinggal","home" to "Tempat Tinggal",
        "house" to "Tempat Tinggal","apartment" to "Tempat Tinggal",
        "maintenance" to "Tempat Tinggal","renovasi" to "Tempat Tinggal",
        // Perawatan
        "beauty" to "Perawatan","perawatan" to "Perawatan","salon" to "Perawatan",
        "haircut" to "Perawatan","skincare" to "Perawatan","grooming" to "Perawatan",
        // Income categories
        "salary" to "Gaji","gaji" to "Gaji","wage" to "Gaji","paycheck" to "Gaji",
        "payroll" to "Gaji","upah" to "Gaji","honorarium" to "Gaji",
        "freelance" to "Freelance","project" to "Freelance","jasa" to "Freelance",
        "gift" to "Hadiah","hadiah" to "Hadiah","bonus" to "Hadiah","reward" to "Hadiah",
        "invest" to "Investasi","investasi" to "Investasi","dividend" to "Investasi",
        "dividen" to "Investasi","profit" to "Investasi","saham" to "Investasi",
        "stock" to "Investasi","reksadana" to "Investasi",
    )

    // BUG-04: Detect dan merge pasangan Transfer In/Out dari Money Manager
    // Money Manager catat 2 baris: expense (from account) + income (to account)
    // Kalau amount sama + tanggal sama + keduanya punya transfer keyword di category → merge jadi 1 transfer
    fun mergeTransferPairs(txns: List<SmartTransaction>): List<SmartTransaction> {
        if (txns.isEmpty()) return txns

        // Pisahkan kandidat transfer (punya transfer keyword di category)
        val transferCandidates = txns.filter { t ->
            SmartImportEngine.TRANSFER_KEYWORDS.any {
                t.category.lowercase().contains(it) ||
                t.originalCategory.lowercase().contains(it)
            }
        }.toMutableList()

        if (transferCandidates.isEmpty()) return txns

        val consumed = mutableSetOf<String>()
        val merged   = mutableListOf<SmartTransaction>()

        for (candidate in transferCandidates) {
            if (candidate.tempId in consumed) continue
            // Cari pasangan: amount sama, tanggal sama, tipe berlawanan
            val partner = transferCandidates.firstOrNull { other ->
                other.tempId != candidate.tempId &&
                other.tempId !in consumed &&
                other.amount == candidate.amount &&
                other.date   == candidate.date &&
                other.suggestedType != candidate.suggestedType
            }
            if (partner != null) {
                // expense = from account, income = to account
                val fromTxn = if (candidate.suggestedType == TransactionType.expense) candidate else partner
                val toTxn   = if (candidate.suggestedType == TransactionType.income)  candidate else partner
                merged.add(
                    fromTxn.copy(
                        suggestedType  = TransactionType.transfer,
                        category       = "Transfer",
                        note           = listOfNotNull(
                            fromTxn.note.takeIf { it.isNotBlank() },
                            toTxn.rawAccountName.takeIf { it.isNotBlank() && it != "Akun Impor" }
                                ?.let { "→ $it" },
                        ).joinToString(" "),
                        // rawAccountName tetap dari account sumber (fromTxn)
                    )
                )
                consumed += candidate.tempId
                consumed += partner.tempId
            }
        }

        // Kumpulkan semua: non-transfer + transfer yang tidak punya pair + hasil merge
        val result = txns.filter { it.tempId !in consumed }.toMutableList()
        result.addAll(merged)
        return result.sortedByDescending { it.date + it.time }
    }

    fun mapCategory(raw: String): String {
        if (raw.isBlank()) return "Lainnya"
        // Strip emoji and leading symbols (e.g. "🍜 Food" → "food", "🚖 Transport" → "transport")
        val cleaned = raw.replace(Regex("[\\p{So}\\p{Sm}\\p{Sc}\\p{Sk}\\p{Cs}]+"), " ")
            .replace(Regex("[^\\p{L}\\p{Nd}\\s&/]"), " ")
            .trim()
        val lc = cleaned.lowercase().trim()
        if (lc.isBlank()) return "Lainnya"
        CATEGORY_MAP[lc]?.let { return it }
        lc.split(Regex("[\\s/&_,]+")).forEach { token ->
            CATEGORY_MAP[token]?.let { return it }
        }
        CATEGORY_MAP.entries.firstOrNull { (k, _) ->
            lc.contains(k) || k.contains(lc)
        }?.let { return it.value }
        return "Lainnya"
    }

    // ── Account fuzzy matching ────────────────────────────────────────────────

    fun matchAccount(rawName: String, existing: List<Account>): Pair<Account?, Float> {
        if (existing.isEmpty()) return null to 0f
        val rawTokens = tokenize(rawName)
        var best: Account? = null
        var bestScore = 0f
        for (acc in existing) {
            val score = jaccardWithBoost(rawTokens, tokenize(acc.name))
            if (score > bestScore) { bestScore = score; best = acc }
        }
        return if (bestScore >= 0.25f) best to bestScore else null to 0f
    }

    private fun tokenize(s: String): Set<String> =
        s.lowercase().replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+")).filter { it.length > 1 }.toSet()

    private fun jaccardWithBoost(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val inter = (a intersect b).size.toFloat()
        val union = (a union b).size.toFloat()
        val boost = if (a.containsAll(b) || b.containsAll(a)) 0.25f else 0f
        return minOf(1f, inter / union + boost)
    }

    // ── Cell helper extensions ────────────────────────────────────────────────

    fun Row.str(colIdx: Int): String? {
        val cell = getCell(colIdx) ?: return null
        return when (cell.cellType) {
            CellType.STRING  -> cell.stringCellValue?.trim()?.takeIf { it.isNotBlank() }
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    val d = cell.localDateTimeCellValue
                    String.format("%04d-%02d-%02d", d.year, d.monthValue, d.dayOfMonth)
                } else {
                    val v = cell.numericCellValue
                    if (v == Math.floor(v)) v.toLong().toString() else v.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> runCatching { cell.stringCellValue?.trim()?.takeIf { it.isNotBlank() } }
                .getOrNull() ?: runCatching { cell.numericCellValue.toLong().toString() }.getOrNull()
            else -> null
        }
    }

    fun Row.dbl(colIdx: Int): Double? {
        val cell = getCell(colIdx) ?: return null
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING  -> parseAmountString(cell.stringCellValue)
            CellType.FORMULA -> runCatching { cell.numericCellValue }.getOrNull()
                ?: runCatching { parseAmountString(cell.stringCellValue) }.getOrNull()
            else -> null
        }
    }

    /**
     * Parse amount strings robustly, handling both Indonesian format (1.500.000 = 1.5jt)
     * and international format (1,500,000 or 1500.50).
     *
     * Strategy:
     * - Count dots and commas to determine which is the decimal separator.
     * - If there are multiple dots and no comma → dots are thousand separators (Indonesian: "14.500")
     * - If there are multiple commas and no dot  → commas are thousand separators
     * - If there is exactly one comma AND one dot, and dot comes last → comma is thousand sep
     * - If there is exactly one dot and no comma, and the part after the dot has ≠ 3 digits → decimal dot
     * - Otherwise treat last separator as decimal
     */
    private fun parseAmountString(raw: String): Double? {
        val s = raw.trim()
        // Kurung = pengeluaran/negatif: (14.500) atau (14,500)
        val isParenNegative = s.startsWith('(') && s.endsWith(')')
        // Tanda ⇔ atau ↔ = transfer, ambil positif
        val withoutPrefix = s
            .removePrefix("⇔")
            .removePrefix("↔")
            .removeSurrounding("(", ")")
            .trim()
        val cleanedForSign = withoutPrefix.trimStart('-')
        val negative = isParenNegative || withoutPrefix.startsWith('-')
        val (trimmed) = cleanedForSign to negative
        // Strip currency symbols, spaces, Rp prefix
        val stripped = trimmed
            .replace(Regex("^[Rr][Pp]\\.?\\s*"), "")
            .replace(Regex("[^\\d.,]"), "")
            .trim()
        if (stripped.isBlank()) return null

        val dotCount   = stripped.count { it == '.' }
        val commaCount = stripped.count { it == ',' }
        val result = when {
            dotCount == 0 && commaCount == 0 -> stripped.toLongOrNull()?.toDouble()
            // Multiple dots, no comma → all dots are thousand separators (Indonesian "1.500.000")
            dotCount > 1 && commaCount == 0  -> stripped.replace(".", "").toLongOrNull()?.toDouble()
            // Multiple commas, no dot → all commas are thousand separators
            commaCount > 1 && dotCount == 0  -> stripped.replace(",", "").toLongOrNull()?.toDouble()
            // One dot only: if 3 digits after dot → treat as thousand separator
            dotCount == 1 && commaCount == 0 -> {
                val afterDot = stripped.substringAfter('.')
                if (afterDot.length == 3) stripped.replace(".", "").toLongOrNull()?.toDouble()
                else stripped.toDoubleOrNull()
            }
            // One comma only: if 3 digits after comma → treat as thousand separator
            commaCount == 1 && dotCount == 0 -> {
                val afterComma = stripped.substringAfter(',')
                if (afterComma.length == 3) stripped.replace(",", "").toLongOrNull()?.toDouble()
                else stripped.replace(',', '.').toDoubleOrNull()
            }
            // Both present: last one wins as decimal separator
            else -> {
                val lastDot   = stripped.lastIndexOf('.')
                val lastComma = stripped.lastIndexOf(',')
                if (lastDot > lastComma) {
                    // dot is decimal: remove commas (thousand sep)
                    stripped.replace(",", "").toDoubleOrNull()
                } else {
                    // comma is decimal: remove dots (thousand sep), replace comma with dot
                    stripped.replace(".", "").replace(',', '.').toDoubleOrNull()
                }
            }
        }
        return if (result != null && negative) -result else result
    }
}
