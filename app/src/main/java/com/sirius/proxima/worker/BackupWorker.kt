package com.sirius.proxima.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sirius.proxima.backup.DriveBackupHelper
import com.sirius.proxima.di.ServiceLocator
import kotlinx.coroutines.flow.first

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val settingsDataStore = ServiceLocator.getSettingsDataStore(applicationContext)
            val subjectRepository = ServiceLocator.getSubjectRepository(applicationContext)
            val timetableRepository = ServiceLocator.getTimetableRepository(applicationContext)
            val studyRepository = ServiceLocator.getStudyRepository(applicationContext)

            val isSignedIn = settingsDataStore.isSignedIn.first()
            if (!isSignedIn) return Result.success()

            val subjects = subjectRepository.getAllSubjectsList()
            val entries = timetableRepository.getAllEntriesList()
            val attendanceHistory = subjectRepository.getAllAttendanceRecordsList()
            val notes = studyRepository.getSubjectNotesList()
            val checklist = studyRepository.getChecklistItemsList()
            val pdfs = studyRepository.getStudyPdfsList()
            val sisRegisterNo = settingsDataStore.sisRegisterNo.first()
            val sisPassword = settingsDataStore.sisPassword.first()
            val sisLoggedIn = settingsDataStore.sisLoggedIn.first()

            val success = DriveBackupHelper.backup(
                applicationContext,
                subjects,
                entries,
                attendanceHistory,
                notes,
                checklist,
                pdfs,
                sisRegisterNo,
                sisPassword,
                sisLoggedIn
            )
            if (success) {
                settingsDataStore.setLastBackupTime(System.currentTimeMillis())
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
