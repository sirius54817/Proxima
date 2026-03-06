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

class TimetableViewModel(
    application: Application,
    private val timetableRepository: TimetableRepository,
    private val subjectRepository: SubjectRepository
) : AndroidViewModel(application) {

    val subjects: StateFlow<List<Subject>> = subjectRepository.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expandedDay = MutableStateFlow(-1)
    val expandedDay: StateFlow<Int> = _expandedDay.asStateFlow()

    val allEntries: StateFlow<List<TimetableEntry>> = timetableRepository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleDay(day: Int) {
        _expandedDay.value = if (_expandedDay.value == day) -1 else day
    }

    fun getEntriesForDay(day: Int): List<TimetableEntry> {
        return allEntries.value.filter { it.dayOfWeek == day }
    }

    fun addOrUpdateEntry(dayOfWeek: Int, hourSlot: Int, subject: Subject, classNumber: String) {
        viewModelScope.launch {
            val existing = timetableRepository.getEntryBySlot(dayOfWeek, hourSlot)
            if (existing != null) {
                val updated = existing.copy(
                    subjectId = subject.id,
                    subjectName = subject.name,
                    classNumber = classNumber
                )
                timetableRepository.updateEntry(updated)
                AlarmScheduler.scheduleAlarm(getApplication(), updated, subject.percentage)
            } else {
                val entry = TimetableEntry(
                    dayOfWeek = dayOfWeek,
                    hourSlot = hourSlot,
                    subjectId = subject.id,
                    subjectName = subject.name,
                    classNumber = classNumber
                )
                val id = timetableRepository.insertEntry(entry)
                AlarmScheduler.scheduleAlarm(getApplication(), entry.copy(id = id.toInt()), subject.percentage)
            }
        }
    }

    fun deleteEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), entry.dayOfWeek, entry.hourSlot)
            timetableRepository.deleteEntry(entry)
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TimetableViewModel(
                        application,
                        ServiceLocator.getTimetableRepository(application),
                        ServiceLocator.getSubjectRepository(application)
                    ) as T
                }
            }
        }
    }
}
