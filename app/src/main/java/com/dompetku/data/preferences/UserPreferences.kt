package com.dompetku.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.dompetku.domain.model.AppPreferences
import com.dompetku.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dompetku_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.dataStore

    // ── Keys ─────────────────────────────────────────────────────────────────
    companion object Keys {
        val ONBOARDED      = booleanPreferencesKey("onboarded")
        val PIN_ENABLED    = booleanPreferencesKey("pin_enabled")
        val PIN_HASH       = stringPreferencesKey("pin_hash")
        val BIO_ENABLED    = booleanPreferencesKey("bio_enabled")
        val SOUND_ENABLED  = booleanPreferencesKey("sound_enabled")
        val HIDE_BALANCE   = booleanPreferencesKey("hide_balance")
        val LANG           = stringPreferencesKey("lang")
        val MONTHLY_BUDGET = longPreferencesKey("monthly_budget")
        val SAVED_PCT      = intPreferencesKey("saved_pct")
        val USER_NAME      = stringPreferencesKey("user_name")
        val USER_AGE       = intPreferencesKey("user_age")
        val USER_JOB       = stringPreferencesKey("user_job")
        val USER_EDU       = stringPreferencesKey("user_edu")
        val AVATAR_PATH    = stringPreferencesKey("avatar_path")
        val NOTIF_ENABLED  = booleanPreferencesKey("notif_enabled")
    }

    // ── Observe ───────────────────────────────────────────────────────────────
    val appPrefsFlow: Flow<AppPreferences> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            AppPreferences(
                onboarded     = prefs[ONBOARDED]      ?: false,
                pinEnabled    = prefs[PIN_ENABLED]    ?: false,
                pinHash       = prefs[PIN_HASH]       ?: "",
                bioEnabled    = prefs[BIO_ENABLED]    ?: false,
                soundEnabled  = prefs[SOUND_ENABLED]  ?: true,
                hideBalance   = prefs[HIDE_BALANCE]   ?: false,
                lang          = prefs[LANG]           ?: "id",
                monthlyBudget = prefs[MONTHLY_BUDGET] ?: 0L,
                savedPct      = prefs[SAVED_PCT]      ?: 0,
                avatarPath    = prefs[AVATAR_PATH]    ?: "",
                notifEnabled  = prefs[NOTIF_ENABLED]  ?: true,
                userProfile   = UserProfile(
                    name = prefs[USER_NAME] ?: "",
                    age  = prefs[USER_AGE]  ?: 0,
                    job  = prefs[USER_JOB]  ?: "",
                    edu  = prefs[USER_EDU]  ?: "",
                ),
            )
        }

    // ── Mutations ─────────────────────────────────────────────────────────────
    suspend fun setOnboarded(value: Boolean) =
        store.edit { it[ONBOARDED] = value }

    suspend fun setPinEnabled(enabled: Boolean) =
        store.edit { it[PIN_ENABLED] = enabled }

    suspend fun setPinHash(hash: String) =
        store.edit { it[PIN_HASH] = hash; it[PIN_ENABLED] = hash.isNotBlank() }

    suspend fun setBioEnabled(enabled: Boolean) =
        store.edit { it[BIO_ENABLED] = enabled }

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

    suspend fun setUserProfile(profile: UserProfile) =
        store.edit {
            it[USER_NAME] = profile.name
            it[USER_AGE]  = profile.age
            it[USER_JOB]  = profile.job
            it[USER_EDU]  = profile.edu
        }

    suspend fun setAvatarPath(path: String) =
        store.edit { it[AVATAR_PATH] = path }

    suspend fun setNotifEnabled(enabled: Boolean) =
        store.edit { it[NOTIF_ENABLED] = enabled }
}
