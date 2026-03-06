package com.dompetku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dompetku.data.preferences.UserPreferences
import com.dompetku.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import com.dompetku.domain.model.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val userPrefs: UserPreferences,
) : ViewModel() {

    val prefs = userPrefs.appPrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val startDestination = userPrefs.appPrefsFlow
        .map { p ->
            when {
                !p.onboarded -> Screen.Onboarding.route
                p.pinEnabled -> Screen.PinLock.route
                else         -> Screen.Main.route
            }
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    // ── Auto-lock ─────────────────────────────────────────────────────────────
    private val _shouldLock = MutableStateFlow(false)
    val shouldLock = _shouldLock.asStateFlow()

    fun triggerLock() {
        if (prefs.value?.pinEnabled == true) _shouldLock.value = true
    }

    fun clearLock() {
        _shouldLock.value = false
    }
}