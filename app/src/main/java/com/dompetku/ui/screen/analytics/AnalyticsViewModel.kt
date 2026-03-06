package com.dompetku.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dompetku.data.preferences.UserPreferences
import com.dompetku.data.repository.TransactionRepository
import com.dompetku.domain.model.Transaction
import com.dompetku.domain.model.TransactionType
import com.dompetku.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class AnalyticsUiState(
    val allTxns:    List<Transaction> = emptyList(),
    val userJob:    String = "",
    val userAge:    Int    = 0,
    val typeFilter: String = "all",
    val dateFilter: String = "month",
    val customFrom: String = "",
    val customTo:   String = DateUtils.todayStr(),
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val userPrefs:       UserPreferences,
) : ViewModel() {

    private val _typeFilter = MutableStateFlow("all")
    private val _dateFilter = MutableStateFlow("month")
    private val _customFrom = MutableStateFlow("")
    private val _customTo   = MutableStateFlow(DateUtils.todayStr())

    // combine only supports up to 5 typed flows — nest the filter state
    private val _filterState = combine(_typeFilter, _dateFilter, _customFrom, _customTo) {
        type, date, from, to -> arrayOf(type, date, from, to)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        transactionRepo.observeAll(),
        userPrefs.appPrefsFlow,
        _filterState,
    ) { txns, prefs, filters ->
        AnalyticsUiState(
            allTxns    = txns,
            userJob    = prefs.userProfile.job,
            userAge    = prefs.userProfile.age,
            typeFilter = filters[0],
            dateFilter = filters[1],
            customFrom = filters[2],
            customTo   = filters[3],
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun setTypeFilter(v: String) { _typeFilter.value = v }
    fun setDateFilter(v: String) { _dateFilter.value = v }
    fun setCustomFrom(v: String) { _customFrom.value = v }
    fun setCustomTo(v: String)   { _customTo.value   = v }

    fun filtered(state: AnalyticsUiState): List<Transaction> {
        val today      = DateUtils.todayStr()
        val monthStart = today.substring(0, 7) + "-01"
        val weekStart  = java.time.LocalDate.now().minusDays(6).toString()
        val yearStart  = today.substring(0, 4) + "-01-01"

        var list = state.allTxns.filter { it.category != "Penyesuaian Saldo" }
        list = when (state.typeFilter) {
            "income"   -> list.filter { it.type == TransactionType.income }
            "expense"  -> list.filter { it.type == TransactionType.expense }
            "transfer" -> list.filter { it.type == TransactionType.transfer }
            else       -> list
        }
        list = when (state.dateFilter) {
            "today"  -> list.filter { it.date == today }
            "week"   -> list.filter { it.date >= weekStart }
            "month"  -> list.filter { it.date >= monthStart }
            "year"   -> list.filter { it.date >= yearStart }
            "custom" -> if (state.customFrom.isNotEmpty()) list.filter { it.date >= state.customFrom && it.date <= state.customTo } else list
            else     -> list
        }
        return list
    }
}
