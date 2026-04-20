package com.sirius.proxima.data.repository

import com.sirius.proxima.data.dao.NoteChecklistItemDao
import com.sirius.proxima.data.dao.PlannerEventDao
import com.sirius.proxima.data.dao.StudyPdfDao
import com.sirius.proxima.data.dao.StudySessionDao
import com.sirius.proxima.data.dao.SubjectNoteDao
import com.sirius.proxima.data.model.NoteChecklistItem
import com.sirius.proxima.data.model.NoteWithChecklist
import com.sirius.proxima.data.model.PlannerEvent
import com.sirius.proxima.data.model.StudyPdf
import com.sirius.proxima.data.model.StudySession
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.model.SubjectNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class StudyRepository(
    private val studySessionDao: StudySessionDao,
    private val studyPdfDao: StudyPdfDao,
    private val subjectNoteDao: SubjectNoteDao,
    private val noteChecklistItemDao: NoteChecklistItemDao,
    private val plannerEventDao: PlannerEventDao
) {
    fun getStudySessions(): Flow<List<StudySession>> = studySessionDao.getAll()

    fun getWeeklyDurationSeconds(fromDate: String, toDate: String): Flow<Long> =
        studySessionDao.getTotalDurationBetween(fromDate, toDate)

    fun getSubjectTotals(subjectsFlow: Flow<List<Subject>>): Flow<List<Pair<Subject, Long>>> {
        return combine(subjectsFlow, getStudySessions()) { subjects, sessions ->
            val bySubject = sessions.groupBy { it.subjectId }
            subjects.map { subject ->
                val total = bySubject[subject.id]?.sumOf { it.durationSeconds } ?: 0L
                subject to total
            }.sortedByDescending { it.second }
        }
    }

    suspend fun addStudySession(subjectId: Int, startedAtMillis: Long, durationSeconds: Long) {
        if (durationSeconds <= 0L) return
        studySessionDao.insert(
            StudySession(
                subjectId = subjectId,
                startedAtMillis = startedAtMillis,
                durationSeconds = durationSeconds,
                sessionDate = LocalDate.now().toString()
            )
        )
    }

    suspend fun getStudyStreakDays(today: LocalDate = LocalDate.now()): Int {
        val dates = studySessionDao.getDistinctSessionDates().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (dates.isEmpty()) return 0

        val dateSet = dates.toSet()
        var cursor = if (dateSet.contains(today)) today else today.minusDays(1)
        if (!dateSet.contains(cursor)) return 0

        var streak = 0
        while (dateSet.contains(cursor)) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun getNotes(searchQuery: String): Flow<List<NoteWithChecklist>> {
        val words = searchQuery
            .trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        return subjectNoteDao.getAllNotes().map { notes ->
            if (words.isEmpty()) return@map notes
            notes.filter { noteWithChecklist ->
                val haystack = buildString {
                    append(noteWithChecklist.note.title)
                    append(' ')
                    append(noteWithChecklist.note.content)
                    append(' ')
                    noteWithChecklist.checklistItems.forEach { append(it.text).append(' ') }
                }.lowercase()
                words.all { haystack.contains(it) }
            }
        }
    }

    fun getNoteById(noteId: Int): Flow<NoteWithChecklist?> =
        subjectNoteDao.getNoteWithChecklistById(noteId)

    suspend fun getSubjectNotesList(): List<SubjectNote> = subjectNoteDao.getAllNotesList()

    suspend fun getChecklistItemsList(): List<NoteChecklistItem> = noteChecklistItemDao.getAllList()

    fun getStudyPdfs(): Flow<List<StudyPdf>> = studyPdfDao.getAll()

    suspend fun getStudyPdfsList(): List<StudyPdf> = studyPdfDao.getAllList()

    fun getStudyPdfById(id: Int): Flow<StudyPdf?> = studyPdfDao.getById(id)

    suspend fun addStudyPdf(subjectId: Int, title: String, filePath: String): Int {
        return studyPdfDao.insert(
            StudyPdf(
                subjectId = subjectId,
                title = title.trim(),
                filePath = filePath
            )
        ).toInt()
    }

    suspend fun deleteStudyPdf(pdf: StudyPdf) {
        studyPdfDao.delete(pdf)
    }

    suspend fun deleteStudyPdfsByIds(ids: List<Int>) {
        if (ids.isEmpty()) return
        studyPdfDao.deleteByIds(ids)
    }

    suspend fun deleteAllStudyPdfs() {
        studyPdfDao.deleteAll()
    }

    suspend fun insertRawNote(note: SubjectNote): Int = subjectNoteDao.insert(note).toInt()

    suspend fun insertRawChecklistItems(items: List<NoteChecklistItem>) {
        if (items.isNotEmpty()) noteChecklistItemDao.insertAll(items)
    }

    suspend fun addOrUpdateNote(
        noteId: Int?,
        subjectId: Int,
        title: String,
        content: String,
        isChecklist: Boolean,
        checklistItems: List<String>
    ) {
        upsertNoteAndReturnId(noteId, subjectId, title, content, isChecklist, checklistItems)
    }

    suspend fun upsertNoteAndReturnId(
        noteId: Int?,
        subjectId: Int,
        title: String,
        content: String,
        isChecklist: Boolean,
        checklistItems: List<String>
    ): Int {
        val now = System.currentTimeMillis()
        val resolvedId = if (noteId == null) {
            subjectNoteDao.insert(
                SubjectNote(
                    subjectId = subjectId,
                    title = title.trim(),
                    content = content.trim(),
                    isChecklist = isChecklist,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            ).toInt()
        } else {
            val existing = subjectNoteDao.getById(noteId)
            subjectNoteDao.update(
                SubjectNote(
                    id = noteId,
                    subjectId = subjectId,
                    title = title.trim(),
                    content = content.trim(),
                    isChecklist = isChecklist,
                    isPinned = existing?.isPinned ?: false,
                    createdAtMillis = existing?.createdAtMillis ?: now,
                    updatedAtMillis = now
                )
            )
            noteId
        }

        noteChecklistItemDao.deleteByNoteId(resolvedId)
        if (isChecklist) {
            noteChecklistItemDao.insertAll(
                checklistItems.filter { it.isNotBlank() }.mapIndexed { index, text ->
                    NoteChecklistItem(noteId = resolvedId, text = text.trim(), position = index)
                }
            )
        }
        return resolvedId
    }

    suspend fun toggleNotePin(item: NoteWithChecklist) {
        subjectNoteDao.update(item.note.copy(isPinned = !item.note.isPinned, updatedAtMillis = System.currentTimeMillis()))
    }

    suspend fun toggleChecklistItem(item: NoteChecklistItem) {
        noteChecklistItemDao.update(item.copy(isChecked = !item.isChecked))
    }

    suspend fun deleteNote(noteId: Int) {
        noteChecklistItemDao.deleteByNoteId(noteId)
        subjectNoteDao.deleteById(noteId)
    }

    fun getPlannerEvents(startDate: String, endDate: String): Flow<List<PlannerEvent>> =
        plannerEventDao.getBetween(startDate, endDate)

    fun getAllPlannerEvents(): Flow<List<PlannerEvent>> = plannerEventDao.getAll()

    suspend fun addPlannerEvent(title: String, date: String, type: String) {
        plannerEventDao.insert(PlannerEvent(title = title.trim(), eventDate = date.trim(), type = type))
    }

    suspend fun deletePlannerEvent(event: PlannerEvent) = plannerEventDao.delete(event)

    suspend fun clearAll() {
        studySessionDao.deleteAll()
        studyPdfDao.deleteAll()
        noteChecklistItemDao.deleteAll()
        subjectNoteDao.deleteAll()
        plannerEventDao.deleteAll()
    }
}




