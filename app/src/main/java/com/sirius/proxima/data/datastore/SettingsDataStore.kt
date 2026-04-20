package com.sirius.proxima.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.sirius.proxima.ui.theme.ThemeMode
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
        val SIS_RESTORED_FROM_BACKUP = booleanPreferencesKey("sis_restored_from_backup")
        val GOOGLE_CALENDAR_ID = longPreferencesKey("google_calendar_id")
        val GOOGLE_CALENDAR_NAME = stringPreferencesKey("google_calendar_name")
        val ACADEMIC_PREVIOUS_CGPA = stringPreferencesKey("academic_previous_cgpa")
        val ACADEMIC_PREVIOUS_CREDITS = stringPreferencesKey("academic_previous_credits")
        val ACADEMIC_COMPLETED_CREDITS = stringPreferencesKey("academic_completed_credits")
        val ACADEMIC_REQUIRED_CREDITS = stringPreferencesKey("academic_required_credits")
        val WEEKLY_STUDY_GOAL_MINUTES = intPreferencesKey("weekly_study_goal_minutes")
        val SEMESTER_START_DATE = stringPreferencesKey("semester_start_date")
        val SEMESTER_END_DATE = stringPreferencesKey("semester_end_date")
        val SHOW_HOME_SEMESTER_PROGRESS = booleanPreferencesKey("show_home_semester_progress")
        val SHOW_HOME_EXAM_COUNTDOWN = booleanPreferencesKey("show_home_exam_countdown")
        val SHOW_HOME_WEEK_OVERVIEW = booleanPreferencesKey("show_home_week_overview")
        val SHOW_HOME_WEEKLY_GOAL_PROGRESS = booleanPreferencesKey("show_home_weekly_goal_progress")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val USE_MATERIAL3 = booleanPreferencesKey("use_material3")
        val USE_MATERIAL_YOU = booleanPreferencesKey("use_material_you")
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
    }

    val googleAccountName: Flow<String?> = dataStore.data.map { it[GOOGLE_ACCOUNT_NAME] }
    val googleAccountEmail: Flow<String?> = dataStore.data.map { it[GOOGLE_ACCOUNT_EMAIL] }
    val lastBackupTime: Flow<Long> = dataStore.data.map { it[LAST_BACKUP_TIME] ?: 0L }
    val isSignedIn: Flow<Boolean> = dataStore.data.map { it[IS_SIGNED_IN] ?: false }
    val sisRegisterNo: Flow<String?> = dataStore.data.map { it[SIS_REGISTER_NO] }
    val sisPassword: Flow<String?> = dataStore.data.map { it[SIS_PASSWORD] }
    val sisLoggedIn: Flow<Boolean> = dataStore.data.map { it[SIS_LOGGED_IN] ?: false }
    val sisFeaturesUnlocked: Flow<Boolean> = dataStore.data.map { it[SIS_FEATURES_UNLOCKED] ?: false }
    val sisRestoredFromBackup: Flow<Boolean> = dataStore.data.map { it[SIS_RESTORED_FROM_BACKUP] ?: false }
    val googleCalendarId: Flow<Long?> = dataStore.data.map { it[GOOGLE_CALENDAR_ID] }
    val googleCalendarName: Flow<String?> = dataStore.data.map { it[GOOGLE_CALENDAR_NAME] }
    val academicPreviousCgpa: Flow<String> = dataStore.data.map { it[ACADEMIC_PREVIOUS_CGPA] ?: "" }
    val academicPreviousCredits: Flow<String> = dataStore.data.map { it[ACADEMIC_PREVIOUS_CREDITS] ?: "" }
    val academicCompletedCredits: Flow<String> = dataStore.data.map { it[ACADEMIC_COMPLETED_CREDITS] ?: "" }
    val academicRequiredCredits: Flow<String> = dataStore.data.map { it[ACADEMIC_REQUIRED_CREDITS] ?: "" }
    val weeklyStudyGoalMinutes: Flow<Int> = dataStore.data.map { it[WEEKLY_STUDY_GOAL_MINUTES] ?: 600 }
    val semesterStartDate: Flow<String?> = dataStore.data.map { it[SEMESTER_START_DATE] }
    val semesterEndDate: Flow<String?> = dataStore.data.map { it[SEMESTER_END_DATE] }
    val showHomeSemesterProgress: Flow<Boolean> = dataStore.data.map { it[SHOW_HOME_SEMESTER_PROGRESS] ?: true }
    val showHomeExamCountdown: Flow<Boolean> = dataStore.data.map { it[SHOW_HOME_EXAM_COUNTDOWN] ?: true }
    val showHomeWeekOverview: Flow<Boolean> = dataStore.data.map { it[SHOW_HOME_WEEK_OVERVIEW] ?: true }
    val showHomeWeeklyGoalProgress: Flow<Boolean> = dataStore.data.map { it[SHOW_HOME_WEEKLY_GOAL_PROGRESS] ?: true }
    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.fromStorage(prefs[APP_THEME_MODE])
    }
    val useMaterial3: Flow<Boolean> = dataStore.data.map { it[USE_MATERIAL3] ?: false }
    val useMaterialYou: Flow<Boolean> = dataStore.data.map { it[USE_MATERIAL_YOU] ?: false }
    val developerMode: Flow<Boolean> = dataStore.data.map { it[DEVELOPER_MODE] ?: false }

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

    suspend fun setSisRestoredFromBackup(restored: Boolean) {
        dataStore.edit {
            it[SIS_RESTORED_FROM_BACKUP] = restored
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

    suspend fun setAcademicPreviousCgpa(value: String) {
        dataStore.edit { it[ACADEMIC_PREVIOUS_CGPA] = value }
    }

    suspend fun setAcademicPreviousCredits(value: String) {
        dataStore.edit { it[ACADEMIC_PREVIOUS_CREDITS] = value }
    }

    suspend fun setAcademicCompletedCredits(value: String) {
        dataStore.edit { it[ACADEMIC_COMPLETED_CREDITS] = value }
    }

    suspend fun setAcademicRequiredCredits(value: String) {
        dataStore.edit { it[ACADEMIC_REQUIRED_CREDITS] = value }
    }

    suspend fun setWeeklyStudyGoalMinutes(value: Int) {
        dataStore.edit { it[WEEKLY_STUDY_GOAL_MINUTES] = value.coerceAtLeast(60) }
    }

    suspend fun setSemesterDates(startDate: String, endDate: String) {
        dataStore.edit {
            it[SEMESTER_START_DATE] = startDate
            it[SEMESTER_END_DATE] = endDate
        }
    }

    suspend fun setShowHomeSemesterProgress(show: Boolean) {
        dataStore.edit { it[SHOW_HOME_SEMESTER_PROGRESS] = show }
    }

    suspend fun setShowHomeExamCountdown(show: Boolean) {
        dataStore.edit { it[SHOW_HOME_EXAM_COUNTDOWN] = show }
    }

    suspend fun setShowHomeWeekOverview(show: Boolean) {
        dataStore.edit { it[SHOW_HOME_WEEK_OVERVIEW] = show }
    }

    suspend fun setShowHomeWeeklyGoalProgress(show: Boolean) {
        dataStore.edit { it[SHOW_HOME_WEEKLY_GOAL_PROGRESS] = show }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[APP_THEME_MODE] = mode.storageValue }
    }

    suspend fun setUseMaterial3(enabled: Boolean) {
        dataStore.edit { it[USE_MATERIAL3] = enabled }
    }

    suspend fun setUseMaterialYou(enabled: Boolean) {
        dataStore.edit { it[USE_MATERIAL_YOU] = enabled }
    }

    suspend fun setDeveloperMode(enabled: Boolean) {
        dataStore.edit { it[DEVELOPER_MODE] = enabled }
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
