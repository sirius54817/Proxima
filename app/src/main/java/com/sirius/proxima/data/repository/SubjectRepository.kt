package com.sirius.proxima.data.repository

import com.sirius.proxima.data.dao.SubjectDao
import com.sirius.proxima.data.dao.TimetableEntryDao
import com.sirius.proxima.data.model.Subject
import kotlinx.coroutines.flow.Flow

class SubjectRepository(
    private val subjectDao: SubjectDao,
    private val timetableEntryDao: TimetableEntryDao
) {
    fun getAllSubjects(): Flow<List<Subject>> = subjectDao.getAllSubjects()

    suspend fun getAllSubjectsList(): List<Subject> = subjectDao.getAllSubjectsList()

    suspend fun getSubjectById(id: Int): Subject? = subjectDao.getSubjectById(id)

    suspend fun insertSubject(subject: Subject): Long = subjectDao.insertSubject(subject)

    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)

    suspend fun deleteSubject(subject: Subject) {
        timetableEntryDao.markSubjectDeleted(subject.id)
        subjectDao.deleteSubject(subject)
    }

    suspend fun markPresent(id: Int) = subjectDao.markPresent(id)

    suspend fun markAbsent(id: Int) = subjectDao.markAbsent(id)

    suspend fun deleteAll() = subjectDao.deleteAllSubjects()
}
