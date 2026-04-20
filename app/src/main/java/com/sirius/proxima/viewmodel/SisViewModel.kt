package com.sirius.proxima.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirius.proxima.data.datastore.SettingsDataStore
import com.sirius.proxima.data.model.AttendanceStatus
import com.sirius.proxima.data.model.SubjectAttendanceRecord
import com.sirius.proxima.data.repository.SubjectRepository
import com.sirius.proxima.data.sis.SisAttendance
import com.sirius.proxima.data.sis.SisDebugLogStore
import com.sirius.proxima.data.sis.SisRepository
import com.sirius.proxima.data.sis.SisResult
import com.sirius.proxima.di.ServiceLocator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class SisUiState {
    object LoggedOut : SisUiState()
    object LoggingIn : SisUiState()
    object LoadingAttendance : SisUiState()
    data class Loaded(val attendance: List<SisAttendance>) : SisUiState()
    data class Error(val message: String) : SisUiState()
}

class SisViewModel(
    application: Application,
    private val sisRepository: SisRepository,
    private val settingsDataStore: SettingsDataStore,
    private val subjectRepository: SubjectRepository
) : AndroidViewModel(application) {

    private suspend fun syncSubjectHistoryFromSis(attendance: List<SisAttendance>) {
        attendance.forEach { sisSubject ->
            val subjectName = sisSubject.courseName.trim()
            if (subjectName.isBlank()) return@forEach

            val localSubject = subjectRepository.getSubjectByName(subjectName) ?: return@forEach
            when (val historyResult = sisRepository.getSubjectAttendanceHistory(sisSubject.detailsUrl)) {
                is SisResult.Success -> {
                    val records = historyResult.data.mapNotNull { row ->
                        val normalizedStatus = when (row.status) {
                            AttendanceStatus.PRESENT -> AttendanceStatus.PRESENT
                            AttendanceStatus.ABSENT -> AttendanceStatus.ABSENT
                            AttendanceStatus.ON_DUTY -> AttendanceStatus.ON_DUTY
                            else -> null
                        } ?: return@mapNotNull null

                        SubjectAttendanceRecord(
                            subjectId = localSubject.id,
                            status = normalizedStatus,
                            date = row.date,
                            slotName = row.slotName
                        )
                    }
                    subjectRepository.insertAttendanceRecords(records)
                }
                is SisResult.Error -> Unit
            }
        }
    }

    private val _uiState = MutableStateFlow<SisUiState>(SisUiState.LoggedOut)
    val uiState: StateFlow<SisUiState> = _uiState.asStateFlow()

    val savedRegisterNo: StateFlow<String?> = settingsDataStore.sisRegisterNo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedPassword: StateFlow<String?> = settingsDataStore.sisPassword
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLoggedIn: StateFlow<Boolean> = settingsDataStore.sisLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val developerMode: StateFlow<Boolean> = settingsDataStore.developerMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val useMaterial3: StateFlow<Boolean> = settingsDataStore.useMaterial3
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val sisDebugLog: StateFlow<String> = SisDebugLogStore.log

    init {
        // Auto-login if credentials are saved
        viewModelScope.launch {
            settingsDataStore.sisLoggedIn.collect { loggedIn ->
                if (loggedIn && _uiState.value is SisUiState.LoggedOut) {
                    autoLogin()
                }
            }
        }
    }

    private fun autoLogin() {
        viewModelScope.launch {
            val regNo = settingsDataStore.sisRegisterNo.first()
            val password = settingsDataStore.sisPassword.first()
            if (regNo != null && password != null) {
                fetchAttendance(regNo, password)
            }
        }
    }

    fun login(registerNo: String, password: String) {
        viewModelScope.launch {
            fetchAttendance(registerNo, password)
        }
    }

    private suspend fun fetchAttendance(registerNo: String, password: String) {
        _uiState.value = SisUiState.LoggingIn
        when (val result = sisRepository.loginAndFetch(registerNo, password)) {
            is SisResult.Success -> {
                subjectRepository.syncSubjectsFromSis(result.data)
                _uiState.value = SisUiState.Loaded(result.data)
                syncSubjectHistoryFromSis(result.data)
            }
            is SisResult.Error -> {
                _uiState.value = SisUiState.Error(result.message)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = SisUiState.LoadingAttendance
            when (val result = sisRepository.getAttendance()) {
                is SisResult.Success -> {
                    subjectRepository.syncSubjectsFromSis(result.data)
                    _uiState.value = SisUiState.Loaded(result.data)
                    syncSubjectHistoryFromSis(result.data)
                }
                is SisResult.Error -> {
                    // Session may have expired — re-login with saved credentials
                    val regNo = settingsDataStore.sisRegisterNo.first()
                    val password = settingsDataStore.sisPassword.first()
                    if (regNo != null && password != null) {
                        fetchAttendance(regNo, password)
                    } else {
                        _uiState.value = SisUiState.Error(result.message)
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sisRepository.logout()
            _uiState.value = SisUiState.LoggedOut
        }
    }

    fun clearSisDebugLog() {
        SisDebugLogStore.clear()
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SisViewModel(
                        application,
                        ServiceLocator.getSisRepository(application),
                        ServiceLocator.getSettingsDataStore(application),
                        ServiceLocator.getSubjectRepository(application)
                    ) as T
                }
            }
        }
    }
}


