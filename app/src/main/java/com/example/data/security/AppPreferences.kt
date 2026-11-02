package com.example.data.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "secrettalk_prefs")

class AppPreferences(private val context: Context) {
    companion object {
        val KEY_IS_LOCKED = booleanPreferencesKey("is_locked")
        val KEY_FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val KEY_LOCKOUT_UNTIL = longPreferencesKey("lockout_until")
        val KEY_AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val KEY_ICON_HIDDEN = booleanPreferencesKey("icon_hidden")
        val KEY_ACTIVE_SECRET_CODE = stringPreferencesKey("active_secret_code")
        val KEY_LAST_ACTIVE_TIME = longPreferencesKey("last_active_time")
    }

    val isLockedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_LOCKED] ?: true
    }

    val isIconHiddenFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ICON_HIDDEN] ?: false
    }

    val autoLockMinutesFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_LOCK_MINUTES] ?: 1 // Default 1 min
    }

    suspend fun setLocked(locked: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOCKED] = locked
        }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_LOCK_MINUTES] = minutes
        }
    }

    suspend fun setIconHidden(hidden: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ICON_HIDDEN] = hidden
        }
    }

    suspend fun recordFailedAttempt(): Pair<Int, Long> {
        var attempts = 0
        var lockoutUntil = 0L
        context.dataStore.edit { prefs ->
            attempts = (prefs[KEY_FAILED_ATTEMPTS] ?: 0) + 1
            prefs[KEY_FAILED_ATTEMPTS] = attempts
            if (attempts >= 5) {
                lockoutUntil = System.currentTimeMillis() + 30_000L // 30 sec lockout
                prefs[KEY_LOCKOUT_UNTIL] = lockoutUntil
            }
        }
        return Pair(attempts, lockoutUntil)
    }

    suspend fun resetFailedAttempts() {
        context.dataStore.edit { prefs ->
            prefs[KEY_FAILED_ATTEMPTS] = 0
            prefs[KEY_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun getLockoutUntil(): Long {
        return context.dataStore.data.first()[KEY_LOCKOUT_UNTIL] ?: 0L
    }

    suspend fun setActiveSecretCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_SECRET_CODE] = code
        }
    }

    suspend fun getActiveSecretCode(): String {
        return context.dataStore.data.first()[KEY_ACTIVE_SECRET_CODE] ?: ""
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOCKED] = true
            prefs[KEY_ACTIVE_SECRET_CODE] = ""
            prefs[KEY_FAILED_ATTEMPTS] = 0
            prefs[KEY_LOCKOUT_UNTIL] = 0L
        }
    }
}
