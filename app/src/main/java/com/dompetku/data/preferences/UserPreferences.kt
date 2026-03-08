package com.dompetku.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.dompetku.domain.model.AppPreferences
import com.dompetku.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dompetku_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secure: SecurePreferences,
) {
    private val store = context.dataStore
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // One-time migration: move sensitive keys from DataStore → SecurePreferences
        scope.launch { migrateLegacyIfNeeded() }
    }

    // ── Keys (non-sensitive only) ─────────────────────────────────────────────
    companion object Keys {
        val ONBOARDED       = booleanPreferencesKey("onboarded")
        val SOUND_ENABLED   = booleanPreferencesKey("sound_enabled")
        val HIDE_BALANCE    = booleanPreferencesKey("hide_balance")
        val LANG            = stringPreferencesKey("lang")
        val MONTHLY_BUDGET  = longPreferencesKey("monthly_budget")
        val SAVED_PCT       = intPreferencesKey("saved_pct")
        val AVATAR_PATH     = stringPreferencesKey("avatar_path")
        val NOTIF_ENABLED   = booleanPreferencesKey("notif_enabled")
        // Migration sentinel
        val SECURE_MIGRATED = booleanPreferencesKey("secure_migrated")
        // Legacy keys — read once during migration, then cleared
        private val LEGACY_PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        private val LEGACY_PIN_HASH    = stringPreferencesKey("pin_hash")
        private val LEGACY_BIO_ENABLED = booleanPreferencesKey("bio_enabled")
        private val LEGACY_USER_NAME   = stringPreferencesKey("user_name")
        private val LEGACY_USER_AGE    = intPreferencesKey("user_age")
        private val LEGACY_USER_JOB    = stringPreferencesKey("user_job")
        private val LEGACY_USER_EDU    = stringPreferencesKey("user_edu")
    }

    // ── Migration ─────────────────────────────────────────────────────────────

    private suspend fun migrateLegacyIfNeeded() {
        val prefs = store.data.first()
        if (prefs[SECURE_MIGRATED] == true) return

        // Only migrate if SecurePreferences is still empty (fresh migration)
        // and legacy DataStore has a PIN hash stored
        val legacyPinHash    = prefs[LEGACY_PIN_HASH]    ?: ""
        val legacyPinEnabled = prefs[LEGACY_PIN_ENABLED] ?: false
        val legacyBioEnabled = prefs[LEGACY_BIO_ENABLED] ?: false
        val legacyName       = prefs[LEGACY_USER_NAME]   ?: ""
        val legacyAge        = prefs[LEGACY_USER_AGE]    ?: 0
        val legacyJob        = prefs[LEGACY_USER_JOB]    ?: ""
        val legacyEdu        = prefs[LEGACY_USER_EDU]    ?: ""

        if (secure.flow.value.pinHash.isBlank()) {
            secure.migrateLegacy(
                pinHash = legacyPinHash, pinEnabled = legacyPinEnabled,
                bioEnabled = legacyBioEnabled,
                name = legacyName, age = legacyAge, job = legacyJob, edu = legacyEdu,
            )
        }

        // Mark done + scrub legacy plaintext from DataStore
        store.edit { p ->
            p[SECURE_MIGRATED] = true
            p.remove(LEGACY_PIN_HASH)
            p.remove(LEGACY_PIN_ENABLED)
            p.remove(LEGACY_BIO_ENABLED)
            p.remove(LEGACY_USER_NAME)
            p.remove(LEGACY_USER_AGE)
            p.remove(LEGACY_USER_JOB)
            p.remove(LEGACY_USER_EDU)
        }
    }

    // ── Observe ───────────────────────────────────────────────────────────────

    val appPrefsFlow: Flow<AppPreferences> = combine(
        store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it },
        secure.flow,
    ) { prefs, sec ->
        AppPreferences(
            onboarded     = prefs[ONBOARDED]      ?: false,
            pinEnabled    = sec.pinEnabled,
            pinHash       = sec.pinHash,
            bioEnabled    = sec.bioEnabled,
            soundEnabled  = prefs[SOUND_ENABLED]  ?: true,
            hideBalance   = prefs[HIDE_BALANCE]   ?: false,
            lang          = prefs[LANG]           ?: "id",
            monthlyBudget = prefs[MONTHLY_BUDGET] ?: 0L,
            savedPct      = prefs[SAVED_PCT]      ?: 0,
            avatarPath    = prefs[AVATAR_PATH]    ?: "",
            notifEnabled  = prefs[NOTIF_ENABLED]  ?: true,
            userProfile   = UserProfile(
                name = sec.userName,
                age  = sec.userAge,
                job  = sec.userJob,
                edu  = sec.userEdu,
            ),
        )
    }

    // ── Mutations — non-sensitive (DataStore) ─────────────────────────────────

    suspend fun setOnboarded(value: Boolean) =
        store.edit { it[ONBOARDED] = value }

    suspend fun setSoundEnabled(enabled: Boolean) =
        store.edit { it[SOUND_ENABLED] = enabled }

    suspend fun setHideBalance(hide: Boolean) =
        store.edit { it[HIDE_BALANCE] = hide }

    suspend fun toggleHideBalance() =
        store.edit { it[HIDE_BALANCE] = !(it[HIDE_BALANCE] ?: false) }

    suspend fun setLang(lang: String) =
        store.edit { it[LANG] = lang }

    suspend fun setMonthlyBudget(budget: Long) =
        store.edit { it[MONTHLY_BUDGET] = budget }

    suspend fun setSavedPct(pct: Int) =
        store.edit { it[SAVED_PCT] = pct }

    suspend fun setAvatarPath(path: String) =
        store.edit { it[AVATAR_PATH] = path }

    suspend fun setNotifEnabled(enabled: Boolean) =
        store.edit { it[NOTIF_ENABLED] = enabled }

    // ── Mutations — sensitive (SecurePreferences) ─────────────────────────────

    suspend fun setPinEnabled(enabled: Boolean) =
        secure.setPinEnabled(enabled)

    suspend fun setPinHash(hash: String) =
        secure.setPinHash(hash)

    suspend fun setBioEnabled(enabled: Boolean) =
        secure.setBioEnabled(enabled)

    suspend fun setUserProfile(profile: UserProfile) =
        secure.setUserProfile(profile.name, profile.age, profile.job, profile.edu)
}
