package com.sirius.proxima.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sirius.proxima.backup.DriveBackupHelper
import com.sirius.proxima.data.datastore.SettingsDataStore
import com.sirius.proxima.data.model.NoteChecklistItem
import com.sirius.proxima.data.model.NoteWithChecklist
import com.sirius.proxima.data.model.StudyPdf
import com.sirius.proxima.data.model.StudySession
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.repository.StudyRepository
import com.sirius.proxima.data.repository.SubjectRepository
import com.sirius.proxima.di.ServiceLocator
import com.sirius.proxima.ui.components.DeleteAnimationBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
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
    private val _pdfDeleteInProgress = MutableStateFlow(false)
    val pdfDeleteInProgress: StateFlow<Boolean> = _pdfDeleteInProgress.asStateFlow()
    private val _pdfDeleteProgress = MutableStateFlow(0f)
    val pdfDeleteProgress: StateFlow<Float> = _pdfDeleteProgress.asStateFlow()
    private val _pdfDeleteProgressText = MutableStateFlow("")
    val pdfDeleteProgressText: StateFlow<String> = _pdfDeleteProgressText.asStateFlow()

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
            _pdfImportStatus.value = if (id != null) "Study material added successfully" else "Failed to add study material"
        }
    }

    fun importStudyPdfs(subjectId: Int, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            var successCount = 0
            uris.forEach { uri ->
                if (importStudyPdfInternal(subjectId, uri) != null) {
                    successCount += 1
                }
            }
            _pdfImportStatus.value = if (successCount == uris.size) {
                "$successCount file(s) added"
            } else {
                "$successCount/${uris.size} file(s) added"
            }
        }
    }

    fun deleteStudyPdf(pdf: StudyPdf) {
        deleteStudyPdfs(listOf(pdf))
    }

    fun deleteStudyPdfs(pdfs: List<StudyPdf>) {
        if (pdfs.isEmpty() || _pdfDeleteInProgress.value) return
        viewModelScope.launch {
            _pdfDeleteInProgress.value = true
            try {
                _pdfDeleteProgress.value = 0f
                _pdfDeleteProgressText.value = "Starting deletion..."
                val isSignedIn = settingsDataStore.isSignedIn.first()
                var fullyDeletedCount = 0
                var fileFailureCount = 0
                var backupFailureCount = 0

                pdfs.forEachIndexed { index, pdf ->
                    _pdfDeleteProgressText.value = "Deleting ${index + 1} of ${pdfs.size}: ${pdf.title}"
                    val fileDeleted = runCatching {
                        val file = File(pdf.filePath)
                        !file.exists() || file.delete()
                    }.getOrElse { false }
                    if (!fileDeleted) fileFailureCount += 1

                    val backupDeleted =
                    if (isSignedIn) {
                        runCatching {
                            DriveBackupHelper.deleteStudyPdfFromBackup(
                                context = getApplication(),
                                pdfId = pdf.id,
                                subjectId = pdf.subjectId,
                                title = pdf.title
                            )
                        }.getOrElse { false }
                    } else {
                        true
                    }
                    if (!backupDeleted) backupFailureCount += 1

                    if (fileDeleted && backupDeleted) {
                        fullyDeletedCount += 1
                    }
                    _pdfDeleteProgress.value = (index + 1).toFloat() / pdfs.size.toFloat()
                    DeleteAnimationBus.trigger()
                }

                studyRepository.deleteStudyPdfsByIds(pdfs.map { it.id })

                _pdfImportStatus.value = buildPdfDeleteSummary(
                    requestedCount = pdfs.size,
                    fullyDeletedCount = fullyDeletedCount,
                    fileFailureCount = fileFailureCount,
                    backupFailureCount = backupFailureCount,
                    includeBackupFailures = isSignedIn
                )
                _pdfDeleteProgressText.value = "Completed"
            } catch (_: Exception) {
                _pdfImportStatus.value = "Failed to delete study material"
                _pdfDeleteProgressText.value = "Failed"
            } finally {
                _pdfDeleteInProgress.value = false
            }
        }
    }

    fun deleteAllStudyPdfs() {
        viewModelScope.launch {
            val allPdfs = studyRepository.getStudyPdfsList()
            deleteStudyPdfs(allPdfs)
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
            } ?: "study_material_${System.currentTimeMillis()}"

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
        viewModelScope.launch {
            studyRepository.deleteNote(noteId)
            DeleteAnimationBus.trigger()
        }
    }

    fun getNoteById(noteId: Int): kotlinx.coroutines.flow.Flow<NoteWithChecklist?> =
        studyRepository.getNoteById(noteId)

    companion object {
        private fun buildPdfDeleteSummary(
            requestedCount: Int,
            fullyDeletedCount: Int,
            fileFailureCount: Int,
            backupFailureCount: Int,
            includeBackupFailures: Boolean
        ): String {
            if (requestedCount <= 0) return "No files selected"
            val hasFailures = fileFailureCount > 0 || (includeBackupFailures && backupFailureCount > 0)
            if (!hasFailures) {
                return if (requestedCount == 1) "1 file deleted" else "$requestedCount files deleted"
            }

            val details = mutableListOf<String>()
            if (fileFailureCount > 0) {
                details += if (fileFailureCount == 1) "1 file cleanup failed" else "$fileFailureCount file cleanups failed"
            }
            if (includeBackupFailures && backupFailureCount > 0) {
                details += if (backupFailureCount == 1) "1 backup cleanup failed" else "$backupFailureCount backup cleanups failed"
            }
            return "${fullyDeletedCount}/${requestedCount} deleted (${details.joinToString()})"
        }

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



