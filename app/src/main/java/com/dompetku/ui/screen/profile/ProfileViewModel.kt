package com.dompetku.ui.screen.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dompetku.data.preferences.UserPreferences
import com.dompetku.data.repository.AccountRepository
import com.dompetku.data.repository.TransactionRepository
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.AccountType
import com.dompetku.domain.model.AppPreferences
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.UserProfile
import com.dompetku.util.AccountResolution
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
                val createdIds = mutableMapOf<String, String>()  // rawName -> newId
                for ((rawName, resolution) in accountResolutions) {
                    if (resolution is AccountResolution.CreateNew) {
                        val newId = "acc_${UUID.randomUUID()}"
                        accountRepo.insert(
                            Account(
                                id            = newId,
                                type          = AccountType.other,
                                name          = resolution.name,
                                balance       = 0L,
                                gradientStart = 0xFF374151L.toInt().toLong(),
                                gradientEnd   = 0xFF6B7280L.toInt().toLong(),
                                sortOrder     = 999,
                            )
                        )
                        createdIds[rawName] = newId
                    }
                }
                // Insert transactions
                var txnCount  = 0
                var skipCount = 0
                for (st in result.transactions) {
                    val resolution = accountResolutions[st.rawAccountName]
                    val accountId  = when (resolution) {
                        is AccountResolution.UseExisting -> resolution.account.id
                        is AccountResolution.CreateNew   -> createdIds[st.rawAccountName]
                        is AccountResolution.Skip, null  -> null
                    }
                    if (accountId == null) { skipCount++; continue }
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
                            )
                        )
                        txnCount++
                    }
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

    fun setLang(lang: String) {
        viewModelScope.launch { userPrefs.setLang(lang) }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            transactionRepo.deleteAll()
            accountRepo.deleteAll()
        }
    }
}
