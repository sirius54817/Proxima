package com.sirius.proxima.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sirius.proxima.data.dao.AssignmentReminderDao
import com.sirius.proxima.data.dao.ExamReminderDao
import com.sirius.proxima.data.dao.NoteChecklistItemDao
import com.sirius.proxima.data.dao.PlannerEventDao
import com.sirius.proxima.data.dao.StudySessionDao
import com.sirius.proxima.data.dao.StudyPdfDao
import com.sirius.proxima.data.dao.SubjectDao
import com.sirius.proxima.data.dao.SubjectNoteDao
import com.sirius.proxima.data.dao.SubjectAttendanceRecordDao
import com.sirius.proxima.data.dao.TimetableEntryDao
import com.sirius.proxima.data.model.AssignmentReminder
import com.sirius.proxima.data.model.ExamReminder
import com.sirius.proxima.data.model.NoteChecklistItem
import com.sirius.proxima.data.model.PlannerEvent
import com.sirius.proxima.data.model.StudyPdf
import com.sirius.proxima.data.model.StudySession
import com.sirius.proxima.data.model.SubjectAttendanceRecord
import com.sirius.proxima.data.model.SubjectNote
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.model.TimetableEntry

@Database(
    entities = [
        Subject::class,
        TimetableEntry::class,
        SubjectAttendanceRecord::class,
        AssignmentReminder::class,
        ExamReminder::class,
        StudySession::class,
        StudyPdf::class,
        SubjectNote::class,
        NoteChecklistItem::class,
        PlannerEvent::class
    ],
    version = 9,
    exportSchema = false
)
abstract class ProximaDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableEntryDao(): TimetableEntryDao
    abstract fun subjectAttendanceRecordDao(): SubjectAttendanceRecordDao
    abstract fun assignmentReminderDao(): AssignmentReminderDao
    abstract fun examReminderDao(): ExamReminderDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun studyPdfDao(): StudyPdfDao
    abstract fun subjectNoteDao(): SubjectNoteDao
    abstract fun noteChecklistItemDao(): NoteChecklistItemDao
    abstract fun plannerEventDao(): PlannerEventDao
}

