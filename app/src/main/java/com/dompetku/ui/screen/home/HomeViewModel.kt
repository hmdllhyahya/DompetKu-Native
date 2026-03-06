package com.dompetku.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dompetku.data.preferences.UserPreferences
import com.dompetku.data.repository.AccountRepository
import com.dompetku.data.repository.TransactionRepository
import com.dompetku.domain.model.Account
import com.dompetku.domain.model.AppPreferences
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.TransactionType
import com.dompetku.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val accounts:      List<Account>     = emptyList(),
    val recentTxns:    List<Transaction> = emptyList(),
    val totalBalance:  Long = 0L,
    val monthIncome:   Long = 0L,
    val monthExpense:  Long = 0L,
    val todayExpense:  Long = 0L,
    val monthlyBudget: Long = 10_000_000L,
    val prefs:         AppPreferences    = AppPreferences(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepo:     AccountRepository,
    private val transactionRepo: TransactionRepository,
    private val userPrefs:       UserPreferences,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        accountRepo.allAccounts,
        transactionRepo.allTransactions,
        userPrefs.appPrefsFlow,
    ) { accounts, txns, prefs ->
        val today      = DateUtils.todayStr()
        val monthStart = today.substring(0, 7) + "-01"
        val monthTxns  = txns.filter { it.date >= monthStart && it.date <= today }
        val monthInc   = monthTxns.filter { it.type == TransactionType.income  && it.category != "Penyesuaian Saldo" }.sumOf { it.amount }
        val monthExp   = monthTxns.filter { it.type == TransactionType.expense && it.category != "Penyesuaian Saldo" }.sumOf { it.amount }
        val todayExp   = txns.filter { it.date == today && it.type == TransactionType.expense }.sumOf { it.amount }
        HomeUiState(
            accounts      = accounts,
            recentTxns    = txns.sortedByDescending { it.date + it.time }.take(10),
            totalBalance  = accounts.sumOf { it.balance },
            monthIncome   = monthInc,
            monthExpense  = monthExp,
            todayExpense  = todayExp,
            monthlyBudget = prefs.monthlyBudget,
            prefs         = prefs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleHideBalance() {
        viewModelScope.launch {
            val current = userPrefs.appPrefsFlow.first()
            userPrefs.setHideBalance(!current.hideBalance)
        }
    }

    fun setMonthlyBudget(value: Long) {
        viewModelScope.launch { userPrefs.setMonthlyBudget(value) }
    }

    fun setSavedPct(pct: Int) {
        viewModelScope.launch { userPrefs.setSavedPct(pct) }
    }
}
