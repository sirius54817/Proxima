package com.sirius.proxima.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirius.proxima.backup.DriveBackupHelper
import com.sirius.proxima.data.calendar.CalendarSyncHelper
import com.sirius.proxima.data.datastore.SettingsDataStore
import com.sirius.proxima.data.model.TimetableEntry
import com.sirius.proxima.data.repository.AcademicToolsRepository
import com.sirius.proxima.data.repository.StudyRepository
import com.sirius.proxima.data.repository.SubjectRepository
import com.sirius.proxima.data.repository.TimetableRepository
import com.sirius.proxima.di.ServiceLocator
import com.sirius.proxima.notification.AlarmScheduler
import com.sirius.proxima.worker.BackupScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val settingsDataStore: SettingsDataStore,
    private val subjectRepository: SubjectRepository,
    private val timetableRepository: TimetableRepository,
    private val academicToolsRepository: AcademicToolsRepository,
    private val studyRepository: StudyRepository
) : AndroidViewModel(application) {

    private var versionTapCount = 0
    private var relockTapCount = 0
    private var lastRelockTapAt = 0L
    private val unlockTapTarget = 10

    val googleAccountName: StateFlow<String?> = settingsDataStore.googleAccountName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googleAccountEmail: StateFlow<String?> = settingsDataStore.googleAccountEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastBackupTime: StateFlow<Long> = settingsDataStore.lastBackupTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val isSignedIn: StateFlow<Boolean> = settingsDataStore.isSignedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedCalendarName: StateFlow<String?> = settingsDataStore.googleCalendarName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sisFeaturesUnlocked: StateFlow<Boolean> = settingsDataStore.sisFeaturesUnlocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val sisRestoredFromBackup: StateFlow<Boolean> = settingsDataStore.sisRestoredFromBackup
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showHomeSemesterProgress: StateFlow<Boolean> = settingsDataStore.showHomeSemesterProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showHomeWeeklyGoalProgress: StateFlow<Boolean> = settingsDataStore.showHomeWeeklyGoalProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isClearing = MutableStateFlow(false)
    val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()

    private val _unlockMessage = MutableStateFlow<String?>(null)
    val unlockMessage: StateFlow<String?> = _unlockMessage.asStateFlow()

    private val _showSpecialModePopup = MutableStateFlow(false)
    val showSpecialModePopup: StateFlow<Boolean> = _showSpecialModePopup.asStateFlow()

    private val _isSyncingToCalendar = MutableStateFlow(false)
    val isSyncingToCalendar: StateFlow<Boolean> = _isSyncingToCalendar.asStateFlow()

    private val _isSyncingFromCalendar = MutableStateFlow(false)
    val isSyncingFromCalendar: StateFlow<Boolean> = _isSyncingFromCalendar.asStateFlow()

    private val _isClearingCalendar = MutableStateFlow(false)
    val isClearingCalendar: StateFlow<Boolean> = _isClearingCalendar.asStateFlow()

    private val _calendarSyncMessage = MutableStateFlow<String?>(null)
    val calendarSyncMessage: StateFlow<String?> = _calendarSyncMessage.asStateFlow()

    fun onVersionTapped() {
        viewModelScope.launch {
            if (sisFeaturesUnlocked.value) {
                val now = System.currentTimeMillis()
                if (now - lastRelockTapAt > 1500L) {
                    relockTapCount = 0
                }
                lastRelockTapAt = now
                relockTapCount += 1

                if (relockTapCount >= 2) {
                    settingsDataStore.setSisFeaturesUnlocked(false)
                    relockTapCount = 0
                    versionTapCount = 0
                    _unlockMessage.value = "Leaving so soon? Special mode disabled"
                } else {
                    _unlockMessage.value = null
                }
                return@launch
            }

            relockTapCount = 0
            versionTapCount += 1

            when (versionTapCount) {
                3 -> _unlockMessage.value = "3 taps... the app is blushing already"
                5 -> _unlockMessage.value = "5 taps... secret agents would be proud"
                9 -> _unlockMessage.value = "9 taps... one more and we enter hyperspace"
                in 1 until unlockTapTarget -> _unlockMessage.value = null
                else -> {
                    settingsDataStore.setSisFeaturesUnlocked(true)
                    versionTapCount = 0
                    _unlockMessage.value = "SIS features enabled"
                    _showSpecialModePopup.value = true
                }
            }
        }
    }

    fun clearUnlockMessage() {
        _unlockMessage.value = null
    }

    fun dismissSpecialModePopup() {
        _showSpecialModePopup.value = false
    }

    fun clearCalendarSyncMessage() {
        _calendarSyncMessage.value = null
    }

    fun setShowHomeSemesterProgress(show: Boolean) {
        viewModelScope.launch { settingsDataStore.setShowHomeSemesterProgress(show) }
    }

    fun setShowHomeWeeklyGoalProgress(show: Boolean) {
        viewModelScope.launch { settingsDataStore.setShowHomeWeeklyGoalProgress(show) }
    }

    fun syncTimetableToGoogleCalendar() {
        viewModelScope.launch {
            _isSyncingToCalendar.value = true
            try {
                val calendar = resolveCalendarSelection() ?: run {
                    _calendarSyncMessage.value = "No writable Google Calendar found"
                    return@launch
                }

                val entries = timetableRepository.getAllEntriesList()
                val stats = CalendarSyncHelper.syncToCalendar(
                    context = getApplication(),
                    calendarId = calendar.first,
                    calendarName = calendar.second,
                    entries = entries
                )

                settingsDataStore.setGoogleCalendar(stats.calendarId, stats.calendarName)
                _calendarSyncMessage.value = "Synced ${stats.upserted} classes to ${stats.calendarName}"
            } catch (e: Exception) {
                _calendarSyncMessage.value = "Calendar sync failed: ${e.message ?: "Unknown error"}"
            } finally {
                _isSyncingToCalendar.value = false
            }
        }
    }

    fun syncFromGoogleCalendar() {
        viewModelScope.launch {
            _isSyncingFromCalendar.value = true
            try {
                val calendar = resolveCalendarSelection() ?: run {
                    _calendarSyncMessage.value = "No writable Google Calendar found"
                    return@launch
                }

                val snapshots = CalendarSyncHelper.loadAppEventsFromCalendar(getApplication(), calendar.first)
                val subjectByName = subjectRepository.getAllSubjectsList()
                    .associateBy { it.name.trim().lowercase() }

                snapshots.forEach { event ->
                    val matchedSubject = subjectByName[event.subjectName.trim().lowercase()]
                    val existing = timetableRepository.getEntryBySlot(event.dayOfWeek, event.hourSlot)

                    if (existing == null) {
                        timetableRepository.insertEntry(
                            TimetableEntry(
                                dayOfWeek = event.dayOfWeek,
                                hourSlot = event.hourSlot,
                                subjectId = matchedSubject?.id,
                                subjectName = matchedSubject?.name ?: event.subjectName,
                                classNumber = event.classNumber
                            )
                        )
                    } else {
                        timetableRepository.updateEntry(
                            existing.copy(
                                subjectId = matchedSubject?.id,
                                subjectName = matchedSubject?.name ?: event.subjectName,
                                classNumber = event.classNumber
                            )
                        )
                    }
                }

                settingsDataStore.setGoogleCalendar(calendar.first, calendar.second)
                _calendarSyncMessage.value = "Imported ${snapshots.size} classes from ${calendar.second}"
            } catch (e: Exception) {
                _calendarSyncMessage.value = "Calendar import failed: ${e.message ?: "Unknown error"}"
            } finally {
                _isSyncingFromCalendar.value = false
            }
        }
    }

    fun clearAppCalendarDataOnly() {
        viewModelScope.launch {
            _isClearingCalendar.value = true
            try {
                val calendar = resolveCalendarSelection() ?: run {
                    _calendarSyncMessage.value = "No Google Calendar selected"
                    return@launch
                }

                val deleted = CalendarSyncHelper.clearAppEvents(getApplication(), calendar.first)
                _calendarSyncMessage.value = "Cleared $deleted app-created events from ${calendar.second}"
            } catch (e: Exception) {
                _calendarSyncMessage.value = "Calendar clear failed: ${e.message ?: "Unknown error"}"
            } finally {
                _isClearingCalendar.value = false
            }
        }
    }

    private suspend fun resolveCalendarSelection(): Pair<Long, String>? {
        val savedId = settingsDataStore.googleCalendarId.first()
        val savedName = settingsDataStore.googleCalendarName.first()
        if (savedId != null && !savedName.isNullOrBlank()) {
            return savedId to savedName
        }

        val detected = CalendarSyncHelper.findWritableGoogleCalendar(getApplication()) ?: return null
        settingsDataStore.setGoogleCalendar(detected.first, detected.second)
        return detected
    }

    fun onSignInSuccess(name: String, email: String) {
        viewModelScope.launch {
            settingsDataStore.setGoogleAccount(name, email)
            BackupScheduler.scheduleDailyBackup(getApplication())
            // Try to restore from backup on sign-in
            restoreFromBackup()
        }
    }

    fun onSignOut() {
        viewModelScope.launch {
            settingsDataStore.clearGoogleAccount()
            settingsDataStore.setSisRestoredFromBackup(false)
            BackupScheduler.cancelBackup(getApplication())
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            _isBackingUp.value = true
            try {
                val subjects = subjectRepository.getAllSubjectsList()
                val entries = timetableRepository.getAllEntriesList()
                val attendanceHistory = subjectRepository.getAllAttendanceRecordsList()
                val notes = studyRepository.getSubjectNotesList()
                val checklist = studyRepository.getChecklistItemsList()
                val pdfs = studyRepository.getStudyPdfsList()
                val sisRegisterNo = settingsDataStore.sisRegisterNo.first()
                val sisPassword = settingsDataStore.sisPassword.first()
                val sisLoggedIn = settingsDataStore.sisLoggedIn.first()
                val success = DriveBackupHelper.backup(
                    getApplication(),
                    subjects,
                    entries,
                    attendanceHistory,
                    notes,
                    checklist,
                    pdfs,
                    sisRegisterNo,
                    sisPassword,
                    sisLoggedIn
                )
                if (success) {
                    settingsDataStore.setLastBackupTime(System.currentTimeMillis())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    private suspend fun restoreFromBackup() {
        try {
            val backupData = DriveBackupHelper.restore(getApplication()) ?: return
            // Only restore if local db is empty
            val localSubjects = subjectRepository.getAllSubjectsList()
            if (localSubjects.isEmpty()) {
                val oldToNewSubjectIds = mutableMapOf<Int, Int>()
                backupData.subjects.forEach { subject ->
                    val newId = subjectRepository.insertSubject(subject.copy(id = 0)).toInt()
                    oldToNewSubjectIds[subject.id] = newId
                }
                backupData.timetableEntries.forEach { entry ->
                    val mappedSubjectId = entry.subjectId?.let { oldToNewSubjectIds[it] }
                    timetableRepository.insertEntry(entry.copy(id = 0, subjectId = mappedSubjectId))
                }

                val mappedHistory = (backupData.attendanceHistory ?: emptyList()).mapNotNull { record ->
                    val mappedSubjectId = oldToNewSubjectIds[record.subjectId] ?: return@mapNotNull null
                    record.copy(id = 0, subjectId = mappedSubjectId)
                }
                subjectRepository.insertAttendanceRecords(mappedHistory)

                val oldToNewNoteIds = mutableMapOf<Int, Int>()
                (backupData.notes ?: emptyList()).forEach { note ->
                    val mappedSubjectId = oldToNewSubjectIds[note.subjectId] ?: return@forEach
                    val newNoteId = studyRepository.insertRawNote(note.copy(id = 0, subjectId = mappedSubjectId))
                    oldToNewNoteIds[note.id] = newNoteId
                }

                val mappedChecklist = (backupData.noteChecklistItems ?: emptyList()).mapNotNull { item ->
                    val mappedNoteId = oldToNewNoteIds[item.noteId] ?: return@mapNotNull null
                    item.copy(id = 0, noteId = mappedNoteId)
                }
                studyRepository.insertRawChecklistItems(mappedChecklist)

                (backupData.studyPdfs ?: emptyList()).forEach { blob ->
                    val mappedSubjectId = oldToNewSubjectIds[blob.item.subjectId] ?: return@forEach
                    val bytes = runCatching {
                        android.util.Base64.decode(blob.base64Content, android.util.Base64.DEFAULT)
                    }.getOrNull() ?: return@forEach

                    val app = getApplication<Application>()
                    val dir = java.io.File(app.filesDir, "study_pdfs").apply { mkdirs() }
                    val safeName = blob.fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    val outFile = java.io.File(dir, "restore_${System.currentTimeMillis()}_$safeName")
                    runCatching { outFile.writeBytes(bytes) }.getOrNull() ?: return@forEach
                    studyRepository.addStudyPdf(mappedSubjectId, blob.item.title, outFile.absolutePath)
                }

                val backupRegNo = backupData.sisRegisterNo
                val backupPassword = backupData.sisPassword
                if (!backupRegNo.isNullOrBlank() && !backupPassword.isNullOrBlank() && (backupData.sisLoggedIn == true)) {
                    settingsDataStore.setSisCredentials(backupRegNo, backupPassword)
                    settingsDataStore.setSisRestoredFromBackup(true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _isClearing.value = true
            try {
                // Cancel all alarms
                val entries = timetableRepository.getAllEntriesList()
                AlarmScheduler.cancelAllAlarms(getApplication(), entries)

                // Clear local data
                subjectRepository.deleteAll()
                timetableRepository.deleteAll()

                // Clear academic tools data and alarms
                val assignments = academicToolsRepository.getAllAssignmentsList()
                assignments.forEach { com.sirius.proxima.notification.AlarmScheduler.cancelAssignmentReminder(getApplication(), it.id) }
                val exams = academicToolsRepository.getAllExamsList()
                exams.forEach { com.sirius.proxima.notification.AlarmScheduler.cancelExamReminder(getApplication(), it.id) }
                academicToolsRepository.clearAll()

                // Delete Drive backup
                DriveBackupHelper.deleteBackup(getApplication())

                settingsDataStore.setLastBackupTime(0L)
                settingsDataStore.setSisRestoredFromBackup(false)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isClearing.value = false
            }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        application,
                        ServiceLocator.getSettingsDataStore(application),
                        ServiceLocator.getSubjectRepository(application),
                        ServiceLocator.getTimetableRepository(application),
                        ServiceLocator.getAcademicToolsRepository(application),
                        ServiceLocator.getStudyRepository(application)
                    ) as T
                }
            }
        }
    }
}
