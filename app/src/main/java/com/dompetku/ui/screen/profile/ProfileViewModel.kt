package com.dompetku.ui.screen.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dompetku.data.preferences.UserPreferences
import com.dompetku.worker.ReminderWorker
import com.dompetku.data.repository.AccountRepository
import com.dompetku.data.repository.TransactionRepository
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.AccountType
import com.dompetku.domain.model.TransactionType
import com.dompetku.domain.model.AppPreferences
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.UserProfile
import com.dompetku.util.AccountResolution
import com.dompetku.util.BrandDetector
import com.dompetku.util.ExportImportManager
import com.dompetku.util.ImportResult
import com.dompetku.util.SmartImportResult
import com.dompetku.util.SmartTransaction
import java.util.UUID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val prefs:        AppPreferences       = AppPreferences(),
    val txnCount:     Int                  = 0,
    val accountCount: Int                  = 0,
    val accounts:     List<Account>        = emptyList(),
)

// ── Export/Import one-shot events ─────────────────────────────────────────────
sealed class EiEvent {
    data class ExportSuccess(val uri: Uri)                                              : EiEvent()
    data class ImportPreviewReady(val result: ImportResult)                             : EiEvent()
    data class SmartImportPreviewReady(val result: SmartImportResult)                   : EiEvent()
    data class ImportCommitted(val txnCount: Int, val accCount: Int, val errCount: Int) : EiEvent()
    data class Failure(val message: String)                                             : EiEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val app:             Application,
    private val userPrefs:       UserPreferences,
    private val transactionRepo: TransactionRepository,
    private val accountRepo:     AccountRepository,
    private val eiManager:       ExportImportManager,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        userPrefs.appPrefsFlow,
        transactionRepo.observeAll(),
        accountRepo.observeAll(),
    ) { prefs, txns, accounts ->
        ProfileUiState(
            prefs        = prefs,
            txnCount     = txns.size,
            accountCount = accounts.size,
            accounts     = accounts,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    // Events consumed by ProfileScreen
    private val _eiEvent = MutableSharedFlow<EiEvent>(extraBufferCapacity = 1)
    val eiEvent: SharedFlow<EiEvent> = _eiEvent.asSharedFlow()

    // ── Export ────────────────────────────────────────────────────────────────

    fun triggerExport() {
        viewModelScope.launch {
            runCatching {
                val txns  = transactionRepo.observeAll().first()
                val accs  = accountRepo.observeAll().first()
                val uri   = eiManager.exportXlsx(txns, accs)
                _eiEvent.emit(EiEvent.ExportSuccess(uri))
            }.onFailure {
                _eiEvent.emit(EiEvent.Failure("Export gagal: ${it.message}"))
            }
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /** Step 1 (legacy): parse file in strict format */
    fun previewImport(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val result = eiManager.importXlsx(uri)
                _eiEvent.emit(EiEvent.ImportPreviewReady(result))
            }.onFailure {
                _eiEvent.emit(EiEvent.Failure("Gagal membaca file: ${it.message}"))
            }
        }
    }

    /** Step 1 (smart): parse any xlsx file intelligently */
    fun smartPreviewImport(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val existing = accountRepo.observeAll().first()
                val result   = eiManager.smartImportXlsx(uri, existing)
                _eiEvent.emit(EiEvent.SmartImportPreviewReady(result))
            }.onFailure {
                _eiEvent.emit(EiEvent.Failure("Gagal membaca file: ${it.message}"))
            }
        }
    }

    /**
     * Step 2 (smart): commit with account resolutions.
     * For each SmartTransaction, look up resolution by rawAccountName.
     * CreateNew accounts are inserted first, then transactions.
     */
    fun commitSmartImport(
        result:             SmartImportResult,
        accountResolutions: Map<String, AccountResolution>,
        replace:            Boolean,
    ) {
        viewModelScope.launch {
            runCatching {
                if (replace) {
                    transactionRepo.deleteAll()
                    accountRepo.deleteAll()
                }
                // Create new accounts first
                // — detect AccountType & brand from name using BrandDetector
                val createdIds = mutableMapOf<String, String>()  // rawName -> newId
                for ((rawName, resolution) in accountResolutions) {
                    if (resolution is AccountResolution.CreateNew) {
                        val newId    = "acc_${UUID.randomUUID()}"
                        val brand    = BrandDetector.detect(resolution.name)
                        val accType  = detectAccountType(resolution.name)
                        val gradStart = brand?.gradientStart?.value?.toLong()
                            ?: 0xFF374151L.toInt().toLong()
                        val gradEnd   = brand?.gradientEnd?.value?.toLong()
                            ?: 0xFF6B7280L.toInt().toLong()
                        accountRepo.insert(
                            Account(
                                id            = newId,
                                type          = accType,
                                name          = resolution.name,
                                balance       = 0L,
                                gradientStart = gradStart,
                                gradientEnd   = gradEnd,
                                brandKey      = brand?.key,
                                sortOrder     = 999,
                            )
                        )
                        createdIds[rawName] = newId
                    }
                }

                // Snapshot of all currently existing accounts for fuzzy-matching toRawAccountName
                val existingAccountsSnap = accountRepo.observeAll().first()

                // Helper: resolve a raw account name to its DB id
                fun resolveAccountId(rawName: String): String? {
                    val res = accountResolutions[rawName]
                    return when (res) {
                        is AccountResolution.UseExisting -> res.account.id
                        is AccountResolution.CreateNew   -> createdIds[rawName]
                        is AccountResolution.Skip, null  -> {
                            // Fuzzy fallback: match against existing + newly created
                            val allAccounts = existingAccountsSnap +
                                createdIds.entries.map { (name, id) ->
                                    com.dompetku.domain.model.Account(
                                        id = id, type = AccountType.other,
                                        name = name, balance = 0L,
                                        gradientStart = 0L, gradientEnd = 0L,
                                    )
                                }
                            val (match, score) = com.dompetku.util.SmartImportEngine
                                .matchAccount(rawName, allAccounts)
                            if (score >= 0.5f) match?.id else null
                        }
                    }
                }

                // Insert transactions + accumulate net balance per accountId
                val balanceDelta = mutableMapOf<String, Long>()
                var txnCount  = 0
                var skipCount = 0
                for (st in result.transactions) {
                    val accountId = resolveAccountId(st.rawAccountName)
                    if (accountId == null) { skipCount++; continue }

                    // Resolve toId for transfer transactions (Transfer-Out with toRawAccountName)
                    val toId = if (st.suggestedType == TransactionType.transfer &&
                        st.toRawAccountName != null) {
                        resolveAccountId(st.toRawAccountName)
                    } else null

                    runCatching {
                        transactionRepo.insert(
                            Transaction(
                                id        = st.tempId,
                                type      = st.suggestedType,
                                amount    = st.amount,
                                category  = st.category,
                                note      = st.note,
                                date      = st.date,
                                time      = st.time,
                                accountId = accountId,
                                fromId    = if (st.suggestedType == TransactionType.transfer) accountId else null,
                                toId      = toId,
                                detected  = if (st.detected) true else null,
                            )
                        )
                        // Accumulate balance delta:
                        //   income   → +amount to accountId
                        //   expense  → -amount from accountId
                        //   transfer → -amount from accountId (from-account)
                        //             +amount to toId (to-account, if resolved)
                        val delta = when (st.suggestedType) {
                            TransactionType.income   -> +st.amount
                            TransactionType.expense  -> -st.amount
                            TransactionType.transfer -> -st.amount
                        }
                        balanceDelta[accountId] = (balanceDelta[accountId] ?: 0L) + delta
                        // Credit the to-account for transfers
                        if (st.suggestedType == TransactionType.transfer && toId != null) {
                            balanceDelta[toId] = (balanceDelta[toId] ?: 0L) + st.amount
                        }
                        txnCount++
                    }
                }

                // Apply computed balance to each affected account
                for ((accountId, delta) in balanceDelta) {
                    accountRepo.adjustBalance(accountId, delta)
                }
                val accCount = accountResolutions.count {
                    it.value is AccountResolution.UseExisting ||
                    it.value is AccountResolution.CreateNew
                }
                _eiEvent.emit(
                    EiEvent.ImportCommitted(
                        txnCount = txnCount,
                        accCount = accCount,
                        errCount = skipCount + result.errors.size,
                    )
                )
            }.onFailure {
                _eiEvent.emit(EiEvent.Failure("Import gagal: ${it.message}"))
            }
        }
    }

    /**
     * Step 2: actually save to DB.
     * [replace] = true  → wipe existing data first, then insert all
     * [replace] = false → merge (skip rows with duplicate IDs via IGNORE conflict strategy)
     */
    fun commitImport(result: ImportResult, replace: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (replace) {
                    transactionRepo.deleteAll()
                    accountRepo.deleteAll()
                }
                // Insert accounts first (transactions may reference them)
                result.accounts.forEach { acc ->
                    runCatching { accountRepo.insert(acc) }
                }
                result.transactions.forEach { txn ->
                    runCatching { transactionRepo.insert(txn) }
                }
                _eiEvent.emit(
                    EiEvent.ImportCommitted(
                        txnCount = result.transactions.size,
                        accCount = result.accounts.size,
                        errCount = result.errors.size,
                    )
                )
            }.onFailure {
                _eiEvent.emit(EiEvent.Failure("Import gagal: ${it.message}"))
            }
        }
    }

    // ── Profile & settings ────────────────────────────────────────────────────

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch { userPrefs.setUserProfile(profile) }
    }

    fun setAvatarPath(path: String) {
        viewModelScope.launch { userPrefs.setAvatarPath(path) }
    }

    fun setPinEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setPinEnabled(enabled) }
    }

    fun setPinHash(hash: String) {
        viewModelScope.launch { userPrefs.setPinHash(hash) }
    }

    fun setBioEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setBioEnabled(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setSoundEnabled(enabled) }
    }

    fun setNotifEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setNotifEnabled(enabled)
            ReminderWorker.schedule(app, enabled)
        }
    }

    fun setLang(lang: String) {
        viewModelScope.launch { userPrefs.setLang(lang) }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            transactionRepo.deleteAll()
            accountRepo.deleteAll()
        }
    }

    // ── Import helpers ─────────────────────────────────────────────────────────

    /**
     * Infer AccountType from account name using BrandDetector + keyword fallback.
     * BrandDetector already knows GoPay → ewallet, BCA → bank, etc.
     * Keyword fallback covers generic names like "Dompet", "Tabungan", dll.
     */
    private fun detectAccountType(name: String): AccountType {
        val brand = BrandDetector.detect(name)
        if (brand != null) {
            val key = brand.key.lowercase()
            // E-Wallet brands
            if (key in setOf("gopay", "ovo", "dana", "shopeepay", "linkaja"))
                return AccountType.ewallet
            // E-Money / prepaid
            if (key in setOf("flazz", "emoney", "brizzi", "tapcash", "jakcard"))
                return AccountType.emoney
            // Digital bank — still type bank
            if (key in setOf("jenius", "jago", "jago syariah", "seabank", "blu", "flip"))
                return AccountType.bank
            // Regular bank
            return AccountType.bank
        }

        // Keyword fallback for generic names
        val lower = name.lowercase()
        return when {
            lower.contains("dompet") || lower.contains("cash") || lower.contains("tunai")
                || lower.contains("wallet")                        -> AccountType.cash
            lower.contains("tabungan") || lower.contains("saving") -> AccountType.savings
            lower.contains("investasi") || lower.contains("invest")
                || lower.contains("saham") || lower.contains("reksadana") -> AccountType.investment
            lower.contains("ewallet") || lower.contains("e-wallet")
                || lower.contains("dana") || lower.contains("pay")  -> AccountType.ewallet
            lower.contains("emoney") || lower.contains("e-money")
                || lower.contains("kartu")                          -> AccountType.emoney
            lower.contains("kredit") || lower.contains("credit")    -> AccountType.credit
            lower.contains("bank") || lower.contains("rekening")    -> AccountType.bank
            else                                                    -> AccountType.other
        }
    }

    /**
     * DEBUG ONLY — fire all 5 notification slots immediately.
     * Uses OneTimeWorkRequest with EXPEDITED priority so WorkManager runs it
     * right away without waiting for battery/network constraints.
     */
    fun testNotifications() {
        val wm = androidx.work.WorkManager.getInstance(app)
        (0..4).forEach { slot ->
            val req = androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(androidx.work.workDataOf("slot" to slot))
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            wm.enqueue(req)
        }
    }
}
