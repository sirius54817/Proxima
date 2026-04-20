package com.sirius.proxima.data.repository

import com.sirius.proxima.data.dao.SubjectDao
import com.sirius.proxima.data.dao.SubjectAttendanceRecordDao
import com.sirius.proxima.data.dao.TimetableEntryDao
import com.sirius.proxima.data.model.AttendanceStatus
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.model.SubjectAttendanceRecord
import com.sirius.proxima.data.sis.SisAttendance
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class SubjectRepository(
    private val subjectDao: SubjectDao,
    private val timetableEntryDao: TimetableEntryDao,
    private val attendanceRecordDao: SubjectAttendanceRecordDao
) {
    fun getAllSubjects(): Flow<List<Subject>> = subjectDao.getAllSubjects()

    suspend fun getAllSubjectsList(): List<Subject> = subjectDao.getAllSubjectsList()

    suspend fun getSubjectById(id: Int): Subject? = subjectDao.getSubjectById(id)

    suspend fun getSubjectByName(name: String): Subject? = subjectDao.getSubjectByName(name)

    suspend fun insertSubject(subject: Subject): Long = subjectDao.insertSubject(subject)

    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)

    suspend fun hideSubject(id: Int) = subjectDao.hideSubject(id)

    suspend fun hideSubjects(ids: List<Int>) {
        if (ids.isEmpty()) return
        subjectDao.hideSubjects(ids)
    }

    suspend fun unhideSubject(id: Int) = subjectDao.unhideSubject(id)

    suspend fun deleteSubject(subject: Subject) {
        timetableEntryDao.markSubjectDeleted(subject.id)
        subjectDao.deleteSubject(subject)
    }

    suspend fun deleteSubjectsByIds(ids: List<Int>) {
        if (ids.isEmpty()) return
        timetableEntryDao.markSubjectsDeleted(ids)
        subjectDao.deleteSubjectsByIds(ids)
    }

    suspend fun markPresent(id: Int) {
        subjectDao.markPresent(id)
        attendanceRecordDao.insertRecord(
            SubjectAttendanceRecord(subjectId = id, status = AttendanceStatus.PRESENT, date = LocalDate.now().toString())
        )
    }

    suspend fun markAbsent(id: Int) {
        subjectDao.markAbsent(id)
        attendanceRecordDao.insertRecord(
            SubjectAttendanceRecord(subjectId = id, status = AttendanceStatus.ABSENT, date = LocalDate.now().toString())
        )
    }

    suspend fun markOnDuty(id: Int) {
        subjectDao.markOnDuty(id)
        attendanceRecordDao.insertRecord(
            SubjectAttendanceRecord(subjectId = id, status = AttendanceStatus.ON_DUTY, date = LocalDate.now().toString())
        )
    }

    fun getAttendanceRecordsBySubjectId(subjectId: Int): Flow<List<SubjectAttendanceRecord>> =
        attendanceRecordDao.getRecordsBySubjectId(subjectId)

    suspend fun getAllAttendanceRecordsList(): List<SubjectAttendanceRecord> =
        attendanceRecordDao.getAllRecordsList()

    suspend fun insertAttendanceRecords(records: List<SubjectAttendanceRecord>) {
        if (records.isEmpty()) return
        attendanceRecordDao.insertRecords(records)
    }

    suspend fun syncSubjectsFromSis(attendance: List<SisAttendance>) {
        attendance.forEach { sisSubject ->
            val subjectName = sisSubject.courseName.trim()
            if (subjectName.isBlank()) return@forEach

            val attended = sisSubject.present + sisSubject.onDuty + sisSubject.medicalLeave
            val existing = subjectDao.getSubjectByName(subjectName)

            if (existing == null) {
                subjectDao.insertSubject(
                    Subject(
                        name = subjectName,
                        totalClasses = sisSubject.total,
                        attendedClasses = attended,
                        sisPresent = sisSubject.present,
                        sisAbsent = sisSubject.absent,
                        sisOnDuty = sisSubject.onDuty
                    )
                )
            } else {
                subjectDao.updateSubject(
                    existing.copy(
                        name = subjectName,
                        totalClasses = sisSubject.total,
                        attendedClasses = attended,
                        sisPresent = sisSubject.present,
                        sisAbsent = sisSubject.absent,
                        sisOnDuty = sisSubject.onDuty
                    )
                )
            }
        }
    }

    suspend fun deleteAll() {
        attendanceRecordDao.deleteAllRecords()
        subjectDao.deleteAllSubjects()
    }
}
