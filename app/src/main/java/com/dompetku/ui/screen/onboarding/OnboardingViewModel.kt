package com.dompetku.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dompetku.data.preferences.UserPreferences
import com.dompetku.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferences,
) : ViewModel() {

    fun completeOnboarding(name: String, age: Int, job: String, edu: String) {
        viewModelScope.launch {
            prefs.setUserProfile(UserProfile(name = name, age = age, job = job, edu = edu))
            prefs.setOnboarded(true)
        }
    }
}
