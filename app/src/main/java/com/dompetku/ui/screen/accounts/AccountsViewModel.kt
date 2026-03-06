package com.dompetku.ui.screen.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dompetku.data.repository.AccountRepository
import com.dompetku.data.repository.TransactionRepository
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepo:     AccountRepository,
    private val transactionRepo: TransactionRepository,
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = accountRepo.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactions: StateFlow<List<Transaction>> = transactionRepo.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalBalance: StateFlow<Long> = accounts
        .map { it.sumOf { acc -> acc.balance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun addAccount(account: Account) {
        viewModelScope.launch {
            accountRepo.insert(account.copy(id = UUID.randomUUID().toString()))
        }
    }

    fun updateAccount(account: Account, balanceAdjustment: Long) {
        viewModelScope.launch {
            accountRepo.update(account)
            if (balanceAdjustment != 0L) {
                transactionRepo.recordBalanceAdjustment(account.id, account.name, balanceAdjustment)
            }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            transactionRepo.deleteByAccount(accountId)
            accountRepo.delete(accountId)
        }
    }

    fun reorderAccounts(reordered: List<Account>) {
        viewModelScope.launch {
            reordered.forEachIndexed { index, account ->
                accountRepo.update(account.copy(sortOrder = index))
            }
        }
    }
}
