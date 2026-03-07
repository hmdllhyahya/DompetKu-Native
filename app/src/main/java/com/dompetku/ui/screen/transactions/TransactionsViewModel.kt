package com.dompetku.ui.screen.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dompetku.data.preferences.UserPreferences
import com.dompetku.data.repository.AccountRepository
import com.dompetku.data.repository.AttachmentRepository
import com.dompetku.data.repository.TransactionRepository
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.Attachment
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.TransactionType
import com.dompetku.util.DateUtils
import java.util.UUID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TypeFilter  { ALL, INCOME, EXPENSE, TRANSFER }
enum class DateFilter  { ALL, TODAY, WEEK, MONTH, CUSTOM }

data class TxnFilters(
    val type:       TypeFilter = TypeFilter.ALL,
    val date:       DateFilter = DateFilter.MONTH,
    val customFrom: String     = "",
    val customTo:   String     = DateUtils.todayStr(),
    val search:     String     = "",
)

data class TxnUiState(
    val allTxns:  List<Transaction> = emptyList(),
    val accounts: List<Account>     = emptyList(),
    val hidden:   Boolean           = false,
    val filters:  TxnFilters        = TxnFilters(),
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val accountRepo:     AccountRepository,
    private val attachmentRepo:  AttachmentRepository,
    private val userPrefs:       UserPreferences,
) : ViewModel() {

    /** Observe attachments for a specific transaction (reactive) */
    fun attachmentsFlow(txnId: String) = attachmentRepo.flowByTransaction(txnId)

    /** Save content:// URI strings as AttachmentEntity records */
    private suspend fun persistUriAttachments(txnId: String, uris: List<String>) {
        uris.filter { it.startsWith("content://") }.forEach { uri ->
            attachmentRepo.insert(Attachment(
                id            = "att_${UUID.randomUUID()}",
                transactionId = txnId,
                filePath      = uri,
                mimeType      = if (uri.contains("image", ignoreCase = true)) "image/*" else "*/*",
            ))
        }
    }

    private val _filters = MutableStateFlow(TxnFilters())

    val uiState: StateFlow<TxnUiState> = combine(
        transactionRepo.allTransactions,
        accountRepo.allAccounts,
        userPrefs.appPrefsFlow,
        _filters,
    ) { txns, accounts, prefs, filters ->
        TxnUiState(allTxns = txns, accounts = accounts, hidden = prefs.hideBalance, filters = filters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TxnUiState())

    fun toggleHideBalance() { viewModelScope.launch { userPrefs.toggleHideBalance() } }

    fun setTypeFilter(f: TypeFilter)  { _filters.update { it.copy(type = f) } }
    fun setDateFilter(f: DateFilter)  { _filters.update { it.copy(date = f) } }
    fun setCustomFrom(v: String)      { _filters.update { it.copy(customFrom = v) } }
    fun setCustomTo(v: String)        { _filters.update { it.copy(customTo = v) } }
    fun setSearch(q: String)          { _filters.update { it.copy(search = q) } }

    fun deleteTransaction(txn: Transaction) {
        viewModelScope.launch {
            // Reverse the balance effect before deleting
            when (txn.type) {
                TransactionType.income   -> accountRepo.adjustBalance(txn.accountId, -txn.amount)
                TransactionType.expense  -> accountRepo.adjustBalance(txn.accountId, +txn.amount)
                TransactionType.transfer -> {
                    txn.fromId?.let { accountRepo.adjustBalance(it, +(txn.amount + txn.adminFee)) }
                    txn.toId?.let   { accountRepo.adjustBalance(it, -txn.amount) }
                }
            }
            transactionRepo.delete(txn)
        }
    }

    fun saveTransaction(txn: Transaction) {
        viewModelScope.launch {
            val uriAttachments = txn.attachmentIds.filter { it.startsWith("content://") }
            val cleanTxn = txn.copy(attachmentIds = emptyList())
            transactionRepo.insert(cleanTxn)
            persistUriAttachments(txn.id, uriAttachments)
            when (txn.type) {
                TransactionType.income   -> accountRepo.adjustBalance(txn.accountId, +txn.amount)
                TransactionType.expense  -> accountRepo.adjustBalance(txn.accountId, -txn.amount)
                TransactionType.transfer -> accountRepo.applyTransfer(
                    fromId   = txn.fromId ?: return@launch,
                    toId     = txn.toId   ?: return@launch,
                    amount   = txn.amount,
                    adminFee = txn.adminFee,
                )
            }
        }
    }

    fun updateTransaction(old: Transaction, new: Transaction) {
        viewModelScope.launch {
            val uriAttachments = new.attachmentIds.filter { it.startsWith("content://") }
            val cleanNew = new.copy(attachmentIds = emptyList())
            // 1. Reverse old transaction's balance effect
            when (old.type) {
                TransactionType.income   -> accountRepo.adjustBalance(old.accountId, -old.amount)
                TransactionType.expense  -> accountRepo.adjustBalance(old.accountId, +old.amount)
                TransactionType.transfer -> {
                    old.fromId?.let { accountRepo.adjustBalance(it, +(old.amount + old.adminFee)) }
                    old.toId?.let   { accountRepo.adjustBalance(it, -old.amount) }
                }
            }
            // 2. Apply new transaction's balance effect
            when (new.type) {
                TransactionType.income   -> accountRepo.adjustBalance(new.accountId, +new.amount)
                TransactionType.expense  -> accountRepo.adjustBalance(new.accountId, -new.amount)
                TransactionType.transfer -> accountRepo.applyTransfer(
                    fromId   = new.fromId ?: return@launch,
                    toId     = new.toId   ?: return@launch,
                    amount   = new.amount,
                    adminFee = new.adminFee,
                )
            }
            // 3. Persist
            transactionRepo.update(cleanNew)
            persistUriAttachments(new.id, uriAttachments)
        }
    }

    fun saveTransfer(txn: Transaction) {
        viewModelScope.launch {
            val from = txn.fromId?.takeIf { it.isNotBlank() } ?: return@launch
            val to   = txn.toId?.takeIf   { it.isNotBlank() } ?: return@launch
            transactionRepo.recordTransfer(txn)
            accountRepo.applyTransfer(
                fromId   = from,
                toId     = to,
                amount   = txn.amount,
                adminFee = txn.adminFee,
            )
        }
    }

    fun filtered(state: TxnUiState): List<Transaction> {
        val f          = state.filters
        val today      = DateUtils.todayStr()
        val monthStart = today.substring(0, 7) + "-01"
        val weekStart  = java.time.LocalDate.now().minusDays(6).toString()

        var list = state.allTxns.sortedByDescending { it.date + it.time }
        list = when (f.type) {
            TypeFilter.INCOME   -> list.filter { it.type == TransactionType.income }
            TypeFilter.EXPENSE  -> list.filter { it.type == TransactionType.expense }
            TypeFilter.TRANSFER -> list.filter { it.type == TransactionType.transfer }
            TypeFilter.ALL      -> list
        }
        list = when (f.date) {
            DateFilter.TODAY  -> list.filter { it.date == today }
            DateFilter.WEEK   -> list.filter { it.date >= weekStart }
            DateFilter.MONTH  -> list.filter { it.date >= monthStart }
            DateFilter.CUSTOM -> if (f.customFrom.isNotEmpty()) list.filter { it.date >= f.customFrom && it.date <= f.customTo } else list
            DateFilter.ALL    -> list
        }
        if (f.search.isNotBlank()) {
            val q = f.search.lowercase()
            list = list.filter { txn ->
                val accName = state.accounts.find { it.id == txn.accountId }?.name ?: ""
                txn.note.lowercase().contains(q) || txn.category.lowercase().contains(q) || accName.lowercase().contains(q)
            }
        }
        return list
    }
}
