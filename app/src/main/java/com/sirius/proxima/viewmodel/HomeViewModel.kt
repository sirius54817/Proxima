package com.sirius.proxima.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirius.proxima.data.datastore.SettingsDataStore
import com.sirius.proxima.data.model.AttendanceStatus
import com.sirius.proxima.data.model.PlannerEvent
import com.sirius.proxima.data.model.PlannerEventType
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.model.SubjectAttendanceRecord
import com.sirius.proxima.data.model.TimetableEntry
import com.sirius.proxima.data.repository.AcademicToolsRepository
import com.sirius.proxima.data.repository.StudyRepository
import com.sirius.proxima.data.repository.SubjectRepository
import com.sirius.proxima.data.repository.TimetableRepository
import com.sirius.proxima.data.sis.SisRepository
import com.sirius.proxima.data.sis.SisResult
import com.sirius.proxima.data.sis.SIS_NETWORK_UNAVAILABLE
import com.sirius.proxima.di.ServiceLocator
import com.sirius.proxima.notification.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import org.json.JSONArray

class HomeViewModel(
    application: Application,
    private val subjectRepository: SubjectRepository,
    private val timetableRepository: TimetableRepository,
    private val academicToolsRepository: AcademicToolsRepository,
    private val studyRepository: StudyRepository,
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

    val tomorrowHolidayText: StateFlow<String?> = _currentDate
        .map { loadTomorrowHolidayText(it.plusDays(1)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val todayDayOfWeek: Int
        get() = when (LocalDate.now().dayOfWeek) {
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
            DayOfWeek.SUNDAY -> 7
        }

    val todayEntries: StateFlow<List<TimetableEntry>> = timetableRepository
        .getEntriesByDay(todayDayOfWeek)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val examCountdowns: StateFlow<List<ExamCountdownItem>> = academicToolsRepository
        .getAllExams()
        .map { exams ->
            val today = LocalDate.now()
            exams.mapNotNull { exam ->
                val examDate = Instant.ofEpochMilli(exam.examAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                val diff = ChronoUnit.DAYS.between(today, examDate)
                if (diff < 0) null else ExamCountdownItem(exam.id, exam.subject, examDate.toString(), diff)
            }.sortedBy { it.daysRemaining }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val semesterStartDate: StateFlow<String> = settingsDataStore.semesterStartDate
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val semesterEndDate: StateFlow<String> = settingsDataStore.semesterEndDate
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val showSemesterProgressCard: StateFlow<Boolean> = settingsDataStore.showHomeSemesterProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showExamCountdownCard: StateFlow<Boolean> = settingsDataStore.showHomeExamCountdown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showWeekOverviewCard: StateFlow<Boolean> = settingsDataStore.showHomeWeekOverview
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showWeeklyGoalProgress: StateFlow<Boolean> = settingsDataStore.showHomeWeeklyGoalProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val weeklyGoalMinutes: StateFlow<Int> = settingsDataStore.weeklyStudyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 600)

    val weeklyStudiedMinutes: StateFlow<Long> = studyRepository.getStudySessions()
        .map { allSessions ->
            val weekStart = LocalDate.now().with(DayOfWeek.MONDAY).toString()
            val weekEnd = LocalDate.now().with(DayOfWeek.MONDAY).plusDays(6).toString()
            allSessions
                .filter { it.sessionDate >= weekStart && it.sessionDate <= weekEnd }
                .sumOf { it.durationSeconds } / 60L
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val semesterProgress: StateFlow<Float> = combine(currentDate, semesterStartDate, semesterEndDate) { date, startRaw, endRaw ->
        val customStart = runCatching { LocalDate.parse(startRaw) }.getOrNull()
        val customEnd = runCatching { LocalDate.parse(endRaw) }.getOrNull()

        val (start, end) = if (customStart != null && customEnd != null && !customEnd.isBefore(customStart)) {
            customStart to customEnd
        } else {
            if (date.monthValue <= 6) LocalDate.of(date.year, 1, 1) to LocalDate.of(date.year, 6, 30)
            else LocalDate.of(date.year, 7, 1) to LocalDate.of(date.year, 12, 31)
        }

        val total = ChronoUnit.DAYS.between(start, end).toFloat().coerceAtLeast(1f)
        val progressed = ChronoUnit.DAYS.between(start, date).toFloat()
        (progressed / total).coerceIn(0f, 1f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val weekOverview: StateFlow<List<HomeWeekEvent>> = combine(
        academicToolsRepository.getAllAssignments(),
        academicToolsRepository.getAllExams(),
        studyRepository.getAllPlannerEvents(),
        currentDate
    ) { assignments, exams, plannerEvents, date ->
        val weekStart = date.with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)

        val assignmentEvents = assignments.mapNotNull { assignment ->
            val dueDate = Instant.ofEpochMilli(assignment.dueAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            if (dueDate < weekStart || dueDate > weekEnd) null
            else HomeWeekEvent("a_${assignment.id}", assignment.title, dueDate.toString(), PlannerEventType.ASSIGNMENT)
        }

        val examEvents = exams.mapNotNull { exam ->
            val examDate = Instant.ofEpochMilli(exam.examAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            if (examDate < weekStart || examDate > weekEnd) null
            else HomeWeekEvent("e_${exam.id}", exam.subject, examDate.toString(), PlannerEventType.EXAM)
        }

        val customEvents = plannerEvents.filter {
            val eventDate = runCatching { LocalDate.parse(it.eventDate) }.getOrNull() ?: return@filter false
            eventDate >= weekStart && eventDate <= weekEnd
        }.map {
            HomeWeekEvent("p_${it.id}", it.title, it.eventDate, it.type, it)
        }

        (assignmentEvents + examEvents + customEvents).sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sisFeaturesUnlocked: StateFlow<Boolean> = settingsDataStore.sisFeaturesUnlocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _historyPortalLoadingSubjectId = MutableStateFlow<Int?>(null)
    val historyPortalLoadingSubjectId: StateFlow<Int?> = _historyPortalLoadingSubjectId.asStateFlow()

    private val _historyPortalError = MutableStateFlow<String?>(null)
    val historyPortalError: StateFlow<String?> = _historyPortalError.asStateFlow()

    fun addSubject(name: String, totalClasses: Int, attendedClasses: Int) {
        viewModelScope.launch {
            subjectRepository.insertSubject(Subject(name = name, totalClasses = totalClasses, attendedClasses = attendedClasses))
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

    fun getAttendanceHistory(subjectId: Int): Flow<List<SubjectAttendanceRecord>> =
        subjectRepository.getAttendanceRecordsBySubjectId(subjectId)

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
                        _historyPortalError.value = mapSisErrorMessage(attendanceResult.message)
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
                        _historyPortalError.value = mapSisErrorMessage(historyResult.message)
                    }
                }
            } catch (e: Exception) {
                _historyPortalError.value = mapSisErrorMessage(e.message ?: "Unable to load portal history")
            } finally {
                _historyPortalLoadingSubjectId.value = null
            }
        }
    }

    fun refreshDate() {
        _currentDate.value = LocalDate.now()
    }

    fun addCustomEvent(title: String, date: String, type: String) {
        viewModelScope.launch {
            if (title.isBlank()) return@launch
            studyRepository.addPlannerEvent(title, date, type)
        }
    }

    fun deleteCustomEvent(event: PlannerEvent) {
        viewModelScope.launch {
            studyRepository.deletePlannerEvent(event)
        }
    }

    fun setSemesterDates(startDate: String, endDate: String) {
        viewModelScope.launch {
            settingsDataStore.setSemesterDates(startDate.trim(), endDate.trim())
        }
    }

    fun setShowSemesterProgressCard(show: Boolean) {
        viewModelScope.launch { settingsDataStore.setShowHomeSemesterProgress(show) }
    }

    fun setShowExamCountdownCard(show: Boolean) {
        viewModelScope.launch { settingsDataStore.setShowHomeExamCountdown(show) }
    }

    fun setShowWeekOverviewCard(show: Boolean) {
        viewModelScope.launch { settingsDataStore.setShowHomeWeekOverview(show) }
    }

    fun setShowWeeklyGoalProgress(show: Boolean) {
        viewModelScope.launch { settingsDataStore.setShowHomeWeeklyGoalProgress(show) }
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

    private fun mapSisErrorMessage(rawMessage: String): String {
        if (!rawMessage.startsWith("$SIS_NETWORK_UNAVAILABLE:")) return rawMessage
        return rawMessage.removePrefix("$SIS_NETWORK_UNAVAILABLE:").ifBlank { rawMessage }
    }

    private fun loadTomorrowHolidayText(tomorrow: LocalDate): String? {
        val prefs = getApplication<Application>().getSharedPreferences(HOLIDAY_PREFS, Application.MODE_PRIVATE)
        val raw = prefs.getString(HOLIDAY_DATA_KEY, null) ?: return null
        return runCatching {
            val array = JSONArray(raw)
            var reason: String? = null
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val date = item.optString("date")
                val type = item.optString("type")
                val note = item.optString("note")
                if (date == tomorrow.toString() && type.contains("holiday", ignoreCase = true)) {
                    reason = note.ifBlank { null }
                    break
                }
            }
            reason?.let {
                val dayName = tomorrow.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                "Tomorrow holiday: $dayName, $tomorrow - $it"
            }
        }.getOrNull()
    }

    data class ExamCountdownItem(
        val id: Int,
        val subject: String,
        val examDate: String,
        val daysRemaining: Long
    )

    data class HomeWeekEvent(
        val id: String,
        val title: String,
        val date: String,
        val type: String,
        val plannerEvent: PlannerEvent? = null
    )

    companion object {
        private const val HOLIDAY_PREFS = "holiday_calendar_storage"
        private const val HOLIDAY_DATA_KEY = "holiday_days_json"

        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(
                        application,
                        ServiceLocator.getSubjectRepository(application),
                        ServiceLocator.getTimetableRepository(application),
                        ServiceLocator.getAcademicToolsRepository(application),
                        ServiceLocator.getStudyRepository(application),
                        ServiceLocator.getSisRepository(application),
                        ServiceLocator.getSettingsDataStore(application)
                    ) as T
                }
            }
        }
    }
}
