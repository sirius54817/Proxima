package com.sirius.proxima.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirius.proxima.data.datastore.SettingsDataStore
import com.sirius.proxima.data.model.AttendanceStatus
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.model.SubjectAttendanceRecord
import com.sirius.proxima.data.model.TimetableEntry
import com.sirius.proxima.data.repository.SubjectRepository
import com.sirius.proxima.data.sis.SisRepository
import com.sirius.proxima.data.sis.SisResult
import com.sirius.proxima.data.repository.TimetableRepository
import com.sirius.proxima.di.ServiceLocator
import com.sirius.proxima.notification.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class HomeViewModel(
    application: Application,
    private val subjectRepository: SubjectRepository,
    private val timetableRepository: TimetableRepository,
    private val sisRepository: SisRepository,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {

    val subjects: StateFlow<List<Subject>> = subjectRepository.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    val todayFormatted: StateFlow<String> = _currentDate.map { date ->
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val dateStr = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        "$dayName, $dateStr"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val todayDayOfWeek: Int
        get() {
            val dow = LocalDate.now().dayOfWeek
            return when (dow) {
                DayOfWeek.MONDAY -> 1
                DayOfWeek.TUESDAY -> 2
                DayOfWeek.WEDNESDAY -> 3
                DayOfWeek.THURSDAY -> 4
                DayOfWeek.FRIDAY -> 5
                DayOfWeek.SATURDAY -> 6
                DayOfWeek.SUNDAY -> 7
            }
        }

    val todayEntries: StateFlow<List<TimetableEntry>> = timetableRepository
        .getEntriesByDay(todayDayOfWeek)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sisFeaturesUnlocked: StateFlow<Boolean> = settingsDataStore.sisFeaturesUnlocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _historyPortalLoadingSubjectId = MutableStateFlow<Int?>(null)
    val historyPortalLoadingSubjectId: StateFlow<Int?> = _historyPortalLoadingSubjectId.asStateFlow()

    private val _historyPortalError = MutableStateFlow<String?>(null)
    val historyPortalError: StateFlow<String?> = _historyPortalError.asStateFlow()

    fun addSubject(name: String, totalClasses: Int, attendedClasses: Int) {
        viewModelScope.launch {
            subjectRepository.insertSubject(
                Subject(name = name, totalClasses = totalClasses, attendedClasses = attendedClasses)
            )
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            subjectRepository.updateSubject(subject)
            rescheduleAlarms()
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            subjectRepository.deleteSubject(subject)
            rescheduleAlarms()
        }
    }

    fun markPresent(subjectId: Int) {
        viewModelScope.launch {
            subjectRepository.markPresent(subjectId)
            rescheduleAlarms()
        }
    }

    fun markAbsent(subjectId: Int) {
        viewModelScope.launch {
            subjectRepository.markAbsent(subjectId)
            rescheduleAlarms()
        }
    }

    fun markOnDuty(subjectId: Int) {
        viewModelScope.launch {
            subjectRepository.markOnDuty(subjectId)
            rescheduleAlarms()
        }
    }

    fun getAttendanceHistory(subjectId: Int): Flow<List<SubjectAttendanceRecord>> {
        return subjectRepository.getAttendanceRecordsBySubjectId(subjectId)
    }

    fun addManualAttendanceRecord(subjectId: Int, status: String, date: String, slotName: String?) {
        viewModelScope.launch {
            subjectRepository.insertAttendanceRecords(
                listOf(
                    SubjectAttendanceRecord(
                        subjectId = subjectId,
                        status = status,
                        date = date,
                        slotName = slotName
                    )
                )
            )
        }
    }

    fun clearHistoryPortalError() {
        _historyPortalError.value = null
    }

    fun loadMoreHistoryFromPortal(subject: Subject) {
        viewModelScope.launch {
            if (!sisFeaturesUnlocked.value) {
                _historyPortalError.value = "SIS is locked. Tap version 10 times in Settings to enable."
                return@launch
            }
            _historyPortalLoadingSubjectId.value = subject.id
            _historyPortalError.value = null

            try {
                val registerNo = settingsDataStore.sisRegisterNo.first()
                val password = settingsDataStore.sisPassword.first()
                if (registerNo.isNullOrBlank() || password.isNullOrBlank()) {
                    _historyPortalError.value = "Login to SIS first to load portal entries."
                    return@launch
                }

                val attendanceResult = sisRepository.loginAndFetch(registerNo, password)
                val attendance = when (attendanceResult) {
                    is SisResult.Success -> attendanceResult.data
                    is SisResult.Error -> {
                        _historyPortalError.value = attendanceResult.message
                        return@launch
                    }
                }

                val matchedSubject = attendance.firstOrNull {
                    it.courseName.trim().equals(subject.name.trim(), ignoreCase = true)
                }

                if (matchedSubject == null || matchedSubject.detailsUrl.isBlank()) {
                    _historyPortalError.value = "Subject history URL not found on SIS."
                    return@launch
                }

                when (val historyResult = sisRepository.getSubjectAttendanceHistory(matchedSubject.detailsUrl)) {
                    is SisResult.Success -> {
                        val records = historyResult.data.mapNotNull { row ->
                            val normalizedStatus = when (row.status) {
                                AttendanceStatus.PRESENT -> AttendanceStatus.PRESENT
                                AttendanceStatus.ABSENT -> AttendanceStatus.ABSENT
                                AttendanceStatus.ON_DUTY -> AttendanceStatus.ON_DUTY
                                else -> null
                            } ?: return@mapNotNull null

                            SubjectAttendanceRecord(
                                subjectId = subject.id,
                                status = normalizedStatus,
                                date = row.date,
                                slotName = row.slotName
                            )
                        }
                        subjectRepository.insertAttendanceRecords(records)
                    }

                    is SisResult.Error -> {
                        _historyPortalError.value = historyResult.message
                    }
                }
            } finally {
                _historyPortalLoadingSubjectId.value = null
            }
        }
    }

    fun refreshDate() {
        _currentDate.value = LocalDate.now()
    }

    private fun rescheduleAlarms() {
        viewModelScope.launch {
            val entries = timetableRepository.getAllEntriesList()
            val subjectsList = subjectRepository.getAllSubjectsList()
            val percentages: Map<Int?, Float> = subjectsList.associate { it.id as Int? to it.percentage }
            AlarmScheduler.scheduleAllAlarms(getApplication(), entries, percentages)
        }
    }

    fun getSubjectPercentage(subjectId: Int?): Float {
        if (subjectId == null) return 0f
        return subjects.value.find { it.id == subjectId }?.percentage ?: 0f
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(
                        application,
                        ServiceLocator.getSubjectRepository(application),
                        ServiceLocator.getTimetableRepository(application),
                        ServiceLocator.getSisRepository(application),
                        ServiceLocator.getSettingsDataStore(application)
                    ) as T
                }
            }
        }
    }
}
