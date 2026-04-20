package com.sirius.proxima.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirius.proxima.data.datastore.SettingsDataStore
import com.sirius.proxima.data.model.AssignmentReminder
import com.sirius.proxima.data.model.ExamReminder
import com.sirius.proxima.data.repository.AcademicToolsRepository
import com.sirius.proxima.di.ServiceLocator
import com.sirius.proxima.notification.AlarmScheduler
import com.sirius.proxima.ui.components.DeleteAnimationBus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AcademicToolsViewModel(
    application: Application,
    private val repository: AcademicToolsRepository,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {

    val assignments: StateFlow<List<AssignmentReminder>> = repository.getAllAssignments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamReminder>> = repository.getAllExams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previousCgpa: StateFlow<String> = settingsDataStore.academicPreviousCgpa
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val previousCredits: StateFlow<String> = settingsDataStore.academicPreviousCredits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val completedCredits: StateFlow<String> = settingsDataStore.academicCompletedCredits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val requiredCredits: StateFlow<String> = settingsDataStore.academicRequiredCredits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setPreviousCgpa(value: String) {
        viewModelScope.launch { settingsDataStore.setAcademicPreviousCgpa(value) }
    }

    fun setPreviousCredits(value: String) {
        viewModelScope.launch { settingsDataStore.setAcademicPreviousCredits(value) }
    }

    fun setCompletedCredits(value: String) {
        viewModelScope.launch { settingsDataStore.setAcademicCompletedCredits(value) }
    }

    fun setRequiredCredits(value: String) {
        viewModelScope.launch { settingsDataStore.setAcademicRequiredCredits(value) }
    }

    fun addAssignment(title: String, dueDate: String) {
        val due = runCatching { LocalDate.parse(dueDate) }.getOrNull() ?: return
        val dueMillis = due.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val remindMillis = due.minusDays(1).atTime(18, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        viewModelScope.launch {
            val id = repository.addAssignment(
                AssignmentReminder(
                    title = title.trim(),
                    dueAtMillis = dueMillis,
                    remindAtMillis = remindMillis
                )
            ).toInt()

            if (remindMillis > System.currentTimeMillis()) {
                AlarmScheduler.scheduleAssignmentReminder(
                    context = getApplication(),
                    reminderId = id,
                    title = title.trim(),
                    dueDateText = due.toString(),
                    triggerAtMillis = remindMillis
                )
            }
        }
    }

    fun addExam(subject: String, examDateTime: String) {
        val exam = runCatching { LocalDateTime.parse(examDateTime) }.getOrNull() ?: return
        val examMillis = exam.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val remindMillis = exam.minusHours(12).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        viewModelScope.launch {
            val id = repository.addExam(
                ExamReminder(
                    subject = subject.trim(),
                    examAtMillis = examMillis,
                    remindAtMillis = remindMillis
                )
            ).toInt()

            if (remindMillis > System.currentTimeMillis()) {
                AlarmScheduler.scheduleExamReminder(
                    context = getApplication(),
                    reminderId = id,
                    subject = subject.trim(),
                    examTimeText = exam.toString(),
                    triggerAtMillis = remindMillis
                )
            }
        }
    }

    fun deleteAssignment(item: AssignmentReminder) {
        viewModelScope.launch {
            repository.deleteAssignment(item)
            AlarmScheduler.cancelAssignmentReminder(getApplication(), item.id)
            DeleteAnimationBus.trigger()
        }
    }

    fun deleteExam(item: ExamReminder) {
        viewModelScope.launch {
            repository.deleteExam(item)
            AlarmScheduler.cancelExamReminder(getApplication(), item.id)
            DeleteAnimationBus.trigger()
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AcademicToolsViewModel(
                        application,
                        ServiceLocator.getAcademicToolsRepository(application),
                        ServiceLocator.getSettingsDataStore(application)
                    ) as T
                }
            }
        }
    }
}

fun millisToLocalDateText(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

fun millisToLocalDateTimeText(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime().toString()


