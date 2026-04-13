package com.sirius.proxima.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "proxima_settings")

class SettingsDataStore(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val GOOGLE_ACCOUNT_NAME = stringPreferencesKey("google_account_name")
        val GOOGLE_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val IS_SIGNED_IN = booleanPreferencesKey("is_signed_in")
        val SIS_REGISTER_NO = stringPreferencesKey("sis_register_no")
        val SIS_PASSWORD = stringPreferencesKey("sis_password")
        val SIS_LOGGED_IN = booleanPreferencesKey("sis_logged_in")
        val SIS_FEATURES_UNLOCKED = booleanPreferencesKey("sis_features_unlocked")
        val GOOGLE_CALENDAR_ID = longPreferencesKey("google_calendar_id")
        val GOOGLE_CALENDAR_NAME = stringPreferencesKey("google_calendar_name")
    }

    val googleAccountName: Flow<String?> = dataStore.data.map { it[GOOGLE_ACCOUNT_NAME] }
    val googleAccountEmail: Flow<String?> = dataStore.data.map { it[GOOGLE_ACCOUNT_EMAIL] }
    val lastBackupTime: Flow<Long> = dataStore.data.map { it[LAST_BACKUP_TIME] ?: 0L }
    val isSignedIn: Flow<Boolean> = dataStore.data.map { it[IS_SIGNED_IN] ?: false }
    val sisRegisterNo: Flow<String?> = dataStore.data.map { it[SIS_REGISTER_NO] }
    val sisPassword: Flow<String?> = dataStore.data.map { it[SIS_PASSWORD] }
    val sisLoggedIn: Flow<Boolean> = dataStore.data.map { it[SIS_LOGGED_IN] ?: false }
    val sisFeaturesUnlocked: Flow<Boolean> = dataStore.data.map { it[SIS_FEATURES_UNLOCKED] ?: false }
    val googleCalendarId: Flow<Long?> = dataStore.data.map { it[GOOGLE_CALENDAR_ID] }
    val googleCalendarName: Flow<String?> = dataStore.data.map { it[GOOGLE_CALENDAR_NAME] }

    suspend fun setSisCredentials(registerNo: String, password: String) {
        dataStore.edit {
            it[SIS_REGISTER_NO] = registerNo
            it[SIS_PASSWORD] = password
            it[SIS_LOGGED_IN] = true
        }
    }

    suspend fun clearSisCredentials() {
        dataStore.edit {
            it.remove(SIS_REGISTER_NO)
            it.remove(SIS_PASSWORD)
            it[SIS_LOGGED_IN] = false
        }
    }

    suspend fun setSisFeaturesUnlocked(unlocked: Boolean) {
        dataStore.edit {
            it[SIS_FEATURES_UNLOCKED] = unlocked
        }
    }

    suspend fun setGoogleCalendar(calendarId: Long, calendarName: String) {
        dataStore.edit {
            it[GOOGLE_CALENDAR_ID] = calendarId
            it[GOOGLE_CALENDAR_NAME] = calendarName
        }
    }

    suspend fun clearGoogleCalendar() {
        dataStore.edit {
            it.remove(GOOGLE_CALENDAR_ID)
            it.remove(GOOGLE_CALENDAR_NAME)
        }
    }

    suspend fun setGoogleAccount(name: String, email: String) {
        dataStore.edit {
            it[GOOGLE_ACCOUNT_NAME] = name
            it[GOOGLE_ACCOUNT_EMAIL] = email
            it[IS_SIGNED_IN] = true
        }
    }

    suspend fun setLastBackupTime(time: Long) {
        dataStore.edit { it[LAST_BACKUP_TIME] = time }
    }

    suspend fun clearGoogleAccount() {
        dataStore.edit {
            it.remove(GOOGLE_ACCOUNT_NAME)
            it.remove(GOOGLE_ACCOUNT_EMAIL)
            it[IS_SIGNED_IN] = false
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
