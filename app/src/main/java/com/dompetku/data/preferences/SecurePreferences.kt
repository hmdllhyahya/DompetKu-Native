package com.dompetku.data.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Snapshot of all sensitive preferences stored in EncryptedSharedPreferences.
 * Exposed as a StateFlow so [UserPreferences.appPrefsFlow] can combine it reactively.
 */
data class SecureData(
    val pinEnabled: Boolean = false,
    val pinHash: String = "",
    val bioEnabled: Boolean = false,
    val userName: String = "",
    val userAge: Int = 0,
    val userJob: String = "",
    val userEdu: String = "",
)

/**
 * Encrypted storage for sensitive user data.
 *
 * Uses [EncryptedSharedPreferences] (AES256-GCM for values, AES256-SIV for keys)
 * backed by a MasterKey stored in Android Keystore.
 *
 * Data stored here:
 *  - PIN hash (PBKDF2)
 *  - PIN / biometric enabled flags
 *  - User profile (name, age, job, edu)
 *
 * The rest of the preferences (sound, lang, budget, etc.) remain in the
 * regular DataStore since they are not sensitive.
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "dompetku_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    // ── Reactive flow ─────────────────────────────────────────────────────────

    private val _flow = MutableStateFlow(snapshot())
    val flow: StateFlow<SecureData> = _flow.asStateFlow()

    private fun snapshot() = SecureData(
        pinEnabled = prefs.getBoolean(K_PIN_ENABLED, false),
        pinHash    = prefs.getString(K_PIN_HASH, "")    ?: "",
        bioEnabled = prefs.getBoolean(K_BIO_ENABLED, false),
        userName   = prefs.getString(K_USER_NAME, "")   ?: "",
        userAge    = prefs.getInt(K_USER_AGE, 0),
        userJob    = prefs.getString(K_USER_JOB, "")    ?: "",
        userEdu    = prefs.getString(K_USER_EDU, "")    ?: "",
    )

    private fun publish() { _flow.value = snapshot() }

    // ── Setters ───────────────────────────────────────────────────────────────

    fun setPinHash(hash: String) {
        prefs.edit()
            .putString(K_PIN_HASH, hash)
            .putBoolean(K_PIN_ENABLED, hash.isNotBlank())
            .apply()
        publish()
    }

    fun setPinEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(K_PIN_ENABLED, enabled).apply()
        publish()
    }

    fun setBioEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(K_BIO_ENABLED, enabled).apply()
        publish()
    }

    fun setUserProfile(name: String, age: Int, job: String, edu: String) {
        prefs.edit()
            .putString(K_USER_NAME, name)
            .putInt(K_USER_AGE, age)
            .putString(K_USER_JOB, job)
            .putString(K_USER_EDU, edu)
            .apply()
        publish()
    }

    // ── One-time migration helper ─────────────────────────────────────────────

    /**
     * Called once by [UserPreferences] to port legacy plaintext values from
     * DataStore into this encrypted store. Only runs if the encrypted store
     * has no PIN hash yet (i.e., fresh migration).
     */
    fun migrateLegacy(
        pinHash: String, pinEnabled: Boolean, bioEnabled: Boolean,
        name: String, age: Int, job: String, edu: String,
    ) {
        prefs.edit()
            .putString(K_PIN_HASH, pinHash)
            .putBoolean(K_PIN_ENABLED, pinEnabled)
            .putBoolean(K_BIO_ENABLED, bioEnabled)
            .putString(K_USER_NAME, name)
            .putInt(K_USER_AGE, age)
            .putString(K_USER_JOB, job)
            .putString(K_USER_EDU, edu)
            .apply()
        publish()
    }

    // ── Keys ──────────────────────────────────────────────────────────────────

    companion object {
        private const val K_PIN_ENABLED = "pin_enabled"
        private const val K_PIN_HASH    = "pin_hash"
        private const val K_BIO_ENABLED = "bio_enabled"
        private const val K_USER_NAME   = "user_name"
        private const val K_USER_AGE    = "user_age"
        private const val K_USER_JOB    = "user_job"
        private const val K_USER_EDU    = "user_edu"
    }
}
