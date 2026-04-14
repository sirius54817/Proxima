package com.sirius.proxima.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirius.proxima.data.datastore.SettingsDataStore
import com.sirius.proxima.data.model.NoteChecklistItem
import com.sirius.proxima.data.model.NoteWithChecklist
import com.sirius.proxima.data.model.StudyPdf
import com.sirius.proxima.data.model.StudySession
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.repository.StudyRepository
import com.sirius.proxima.data.repository.SubjectRepository
import com.sirius.proxima.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

class StudyViewModel(
    application: Application,
    private val subjectRepository: SubjectRepository,
    private val studyRepository: StudyRepository,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {

    private val noteSearch = MutableStateFlow("")
    val noteSearchQuery: StateFlow<String> = noteSearch.asStateFlow()
    private val _pdfImportStatus = MutableStateFlow<String?>(null)
    val pdfImportStatus: StateFlow<String?> = _pdfImportStatus.asStateFlow()

    val subjects: StateFlow<List<Subject>> = subjectRepository.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<StudySession>> = studyRepository.getStudySessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyPdfs: StateFlow<List<StudyPdf>> = studyRepository.getStudyPdfs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyGoalMinutes: StateFlow<Int> = settingsDataStore.weeklyStudyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 600)

    private val weekRange: Pair<String, String>
        get() = currentWeekRange()

    val weeklyStudiedMinutes: StateFlow<Long> = sessions
        .map { allSessions ->
            allSessions
                .filter { it.sessionDate >= weekRange.first && it.sessionDate <= weekRange.second }
                .sumOf { it.durationSeconds } / 60L
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val studyStreakDays: StateFlow<Int> = sessions
        .map { studyRepository.getStudyStreakDays(LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val subjectTotals: StateFlow<List<Pair<Subject, Long>>> =
        studyRepository.getSubjectTotals(subjects)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<NoteWithChecklist>> = noteSearch
        .map { it.trim() }
        .flatMapLatest { query -> studyRepository.getNotes(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setNoteSearch(query: String) {
        noteSearch.value = query
    }

    fun saveWeeklyGoalHours(goalHours: Int) {
        viewModelScope.launch {
            settingsDataStore.setWeeklyStudyGoalMinutes(goalHours * 60)
        }
    }

    fun addSession(subjectId: Int, startedAtMillis: Long, durationSeconds: Long) {
        viewModelScope.launch {
            studyRepository.addStudySession(subjectId, startedAtMillis, durationSeconds)
        }
    }

    fun addOrUpdateNote(
        noteId: Int?,
        subjectId: Int,
        title: String,
        content: String,
        isChecklist: Boolean,
        checklistItems: List<String>
    ) {
        viewModelScope.launch {
            studyRepository.addOrUpdateNote(noteId, subjectId, title, content, isChecklist, checklistItems)
        }
    }

    suspend fun upsertNoteAndGetId(
        noteId: Int?,
        subjectId: Int,
        title: String,
        content: String,
        isChecklist: Boolean,
        checklistItems: List<String>
    ): Int {
        return studyRepository.upsertNoteAndReturnId(
            noteId = noteId,
            subjectId = subjectId,
            title = title,
            content = content,
            isChecklist = isChecklist,
            checklistItems = checklistItems
        )
    }

    fun clearPdfImportStatus() {
        _pdfImportStatus.value = null
    }

    fun importStudyPdf(subjectId: Int, uri: Uri) {
        viewModelScope.launch {
            val id = importStudyPdfInternal(subjectId, uri)
            _pdfImportStatus.value = if (id != null) "PDF added successfully" else "Failed to add PDF"
        }
    }

    private suspend fun importStudyPdfInternal(subjectId: Int, uri: Uri): Int? {
        val app = getApplication<Application>()
        val resolver = app.contentResolver

        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val name = resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            } ?: "study_${System.currentTimeMillis()}.pdf"

        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val dir = File(app.filesDir, "study_pdfs").apply { mkdirs() }
        val outFile = File(dir, "${System.currentTimeMillis()}_$safeName")

        val copied = withContext(Dispatchers.IO) {
            runCatching {
                resolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return@runCatching false
                true
            }.getOrElse { false }
        }
        if (!copied) return null

        val displayTitle = name.substringBeforeLast('.').ifBlank { outFile.nameWithoutExtension }
        return studyRepository.addStudyPdf(subjectId, displayTitle, outFile.absolutePath)
    }

    fun getStudyPdfById(id: Int): kotlinx.coroutines.flow.Flow<StudyPdf?> =
        studyRepository.getStudyPdfById(id)

    fun togglePin(note: NoteWithChecklist) {
        viewModelScope.launch { studyRepository.toggleNotePin(note) }
    }

    fun toggleChecklist(item: NoteChecklistItem) {
        viewModelScope.launch { studyRepository.toggleChecklistItem(item) }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch { studyRepository.deleteNote(noteId) }
    }

    fun getNoteById(noteId: Int): kotlinx.coroutines.flow.Flow<NoteWithChecklist?> =
        studyRepository.getNoteById(noteId)

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudyViewModel(
                        application,
                        ServiceLocator.getSubjectRepository(application),
                        ServiceLocator.getStudyRepository(application),
                        ServiceLocator.getSettingsDataStore(application)
                    ) as T
                }
            }
        }

        private fun currentWeekRange(today: LocalDate = LocalDate.now()): Pair<String, String> {
            val start = today.with(DayOfWeek.MONDAY)
            val end = start.plusDays(6)
            return start.toString() to end.toString()
        }
    }
}



