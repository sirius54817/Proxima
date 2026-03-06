package com.sirius.proxima.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.model.TimetableEntry
import com.sirius.proxima.data.repository.SubjectRepository
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
    private val timetableRepository: TimetableRepository
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
                        ServiceLocator.getTimetableRepository(application)
                    ) as T
                }
            }
        }
    }
}
