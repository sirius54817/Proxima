package com.sirius.proxima.data.repository

import com.sirius.proxima.data.dao.AssignmentReminderDao
import com.sirius.proxima.data.dao.ExamReminderDao
import com.sirius.proxima.data.model.AssignmentReminder
import com.sirius.proxima.data.model.ExamReminder
import kotlinx.coroutines.flow.Flow

class AcademicToolsRepository(
    private val assignmentReminderDao: AssignmentReminderDao,
    private val examReminderDao: ExamReminderDao
) {
    fun getAllAssignments(): Flow<List<AssignmentReminder>> = assignmentReminderDao.getAll()

    fun getAllExams(): Flow<List<ExamReminder>> = examReminderDao.getAll()

    suspend fun getAllAssignmentsList(): List<AssignmentReminder> = assignmentReminderDao.getAllList()

    suspend fun getAllExamsList(): List<ExamReminder> = examReminderDao.getAllList()

    suspend fun addAssignment(item: AssignmentReminder): Long = assignmentReminderDao.insert(item)

    suspend fun addExam(item: ExamReminder): Long = examReminderDao.insert(item)

    suspend fun deleteAssignment(item: AssignmentReminder) = assignmentReminderDao.delete(item)

    suspend fun deleteExam(item: ExamReminder) = examReminderDao.delete(item)

    suspend fun clearAll() {
        assignmentReminderDao.deleteAll()
        examReminderDao.deleteAll()
    }
}

