package com.sirius.proxima.backup

import android.content.Context
import android.util.Base64
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.model.SubjectAttendanceRecord
import com.sirius.proxima.data.model.TimetableEntry
import com.sirius.proxima.data.model.SubjectNote
import com.sirius.proxima.data.model.NoteChecklistItem
import com.sirius.proxima.data.model.StudyPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

data class BackupPdfBlob(
    val item: StudyPdf,
    val fileName: String,
    val base64Content: String
)

data class BackupData(
    val subjects: List<Subject>,
    val timetableEntries: List<TimetableEntry>,
    val attendanceHistory: List<SubjectAttendanceRecord>? = null,
    val notes: List<SubjectNote>? = null,
    val noteChecklistItems: List<NoteChecklistItem>? = null,
    val studyPdfs: List<BackupPdfBlob>? = null,
    val sisRegisterNo: String? = null,
    val sisPassword: String? = null,
    val sisLoggedIn: Boolean? = null
)

object DriveBackupHelper {

    private const val BACKUP_FILE_NAME = "proxima_backup.json"
    private const val APP_NAME = "Proxima"

    private fun getDriveService(context: Context): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(APP_NAME).build()
    }

    suspend fun backup(
        context: Context,
        subjects: List<Subject>,
        entries: List<TimetableEntry>,
        attendanceHistory: List<SubjectAttendanceRecord>,
        notes: List<SubjectNote>,
        noteChecklistItems: List<NoteChecklistItem>,
        studyPdfs: List<StudyPdf>,
        sisRegisterNo: String?,
        sisPassword: String?,
        sisLoggedIn: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService(context) ?: return@withContext false
                val pdfBlobs = studyPdfs.mapNotNull { pdf ->
                    val file = File(pdf.filePath)
                    if (!file.exists()) return@mapNotNull null
                    val bytes = runCatching { file.readBytes() }.getOrNull() ?: return@mapNotNull null
                    BackupPdfBlob(
                        item = pdf,
                        fileName = file.name,
                        base64Content = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    )
                }

                val backupData = BackupData(
                    subjects = subjects,
                    timetableEntries = entries,
                    attendanceHistory = attendanceHistory,
                    notes = notes,
                    noteChecklistItems = noteChecklistItems,
                    studyPdfs = pdfBlobs,
                    sisRegisterNo = sisRegisterNo,
                    sisPassword = sisPassword,
                    sisLoggedIn = sisLoggedIn
                )
                val json = Gson().toJson(backupData)

                // Check if backup file already exists
                val existingFileId = findBackupFileId(driveService)

                if (existingFileId != null) {
                    // Update existing file
                    val content = ByteArrayContent.fromString("application/json", json)
                    driveService.files().update(existingFileId, null, content).execute()
                } else {
                    // Create new file
                    val fileMetadata = com.google.api.services.drive.model.File().apply {
                        name = BACKUP_FILE_NAME
                        parents = listOf("appDataFolder")
                    }
                    val content = ByteArrayContent.fromString("application/json", json)
                    driveService.files().create(fileMetadata, content)
                        .setFields("id")
                        .execute()
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun restore(context: Context): BackupData? {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService(context) ?: return@withContext null
                val fileId = findBackupFileId(driveService) ?: return@withContext null

                val outputStream = ByteArrayOutputStream()
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                val json = outputStream.toString("UTF-8")

                val raw = Gson().fromJson(json, BackupData::class.java)
                raw.copy(
                    attendanceHistory = raw.attendanceHistory ?: emptyList(),
                    notes = raw.notes ?: emptyList(),
                    noteChecklistItems = raw.noteChecklistItems ?: emptyList(),
                    studyPdfs = raw.studyPdfs ?: emptyList()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun deleteBackup(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService(context) ?: return@withContext false
                val fileId = findBackupFileId(driveService) ?: return@withContext true
                driveService.files().delete(fileId).execute()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun findBackupFileId(driveService: Drive): String? {
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$BACKUP_FILE_NAME'")
            .setFields("files(id, name)")
            .setPageSize(1)
            .execute()
        return result.files?.firstOrNull()?.id
    }
}

