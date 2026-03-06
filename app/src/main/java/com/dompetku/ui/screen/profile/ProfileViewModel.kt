package com.dompetku.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dompetku.data.preferences.UserPreferences
import com.dompetku.data.repository.AccountRepository
import com.dompetku.data.repository.TransactionRepository
import com.dompetku.domain.model.AppPreferences
import com.dompetku.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val prefs:        AppPreferences = AppPreferences(),
    val txnCount:     Int            = 0,
    val accountCount: Int            = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPrefs:       UserPreferences,
    private val transactionRepo: TransactionRepository,
    private val accountRepo:     AccountRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        userPrefs.appPrefsFlow,
        transactionRepo.observeAll(),
        accountRepo.observeAll(),
    ) { prefs, txns, accounts ->
        ProfileUiState(prefs = prefs, txnCount = txns.size, accountCount = accounts.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

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
