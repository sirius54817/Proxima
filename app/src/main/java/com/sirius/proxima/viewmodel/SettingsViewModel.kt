package com.sirius.proxima.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirius.proxima.backup.DriveBackupHelper
import com.sirius.proxima.data.datastore.SettingsDataStore
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
    private val timetableRepository: TimetableRepository
) : AndroidViewModel(application) {

    val googleAccountName: StateFlow<String?> = settingsDataStore.googleAccountName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val googleAccountEmail: StateFlow<String?> = settingsDataStore.googleAccountEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastBackupTime: StateFlow<Long> = settingsDataStore.lastBackupTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val isSignedIn: StateFlow<Boolean> = settingsDataStore.isSignedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isClearing = MutableStateFlow(false)
    val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()

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
            BackupScheduler.cancelBackup(getApplication())
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            _isBackingUp.value = true
            try {
                val subjects = subjectRepository.getAllSubjectsList()
                val entries = timetableRepository.getAllEntriesList()
                val success = DriveBackupHelper.backup(getApplication(), subjects, entries)
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
                backupData.subjects.forEach { subject ->
                    subjectRepository.insertSubject(subject.copy(id = 0))
                }
                backupData.timetableEntries.forEach { entry ->
                    timetableRepository.insertEntry(entry.copy(id = 0))
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

                // Delete Drive backup
                DriveBackupHelper.deleteBackup(getApplication())

                settingsDataStore.setLastBackupTime(0L)
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
                        ServiceLocator.getTimetableRepository(application)
                    ) as T
                }
            }
        }
    }
}
