package com.sirius.proxima.backup

import android.accounts.Account
import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private const val BACKUP_FILE_PREFIX = "proxima_backup_"
    private const val MAX_TIMESTAMPED_BACKUPS = 5
    private const val APP_NAME = "Proxima"
    private const val TAG = "DriveBackupHelper"
    @Volatile
    private var lastError: String? = null

    fun getLastError(): String? = lastError

    private fun setLastError(message: String?) {
        lastError = message
    }

    private fun describeError(t: Throwable): String {
        return when (t) {
            is GoogleJsonResponseException -> {
                val detail = t.details?.message ?: t.statusMessage ?: t.message
                "GoogleJsonResponseException(code=${t.statusCode}): ${detail ?: "unknown"}"
            }
            is UserRecoverableAuthIOException -> {
                "UserRecoverableAuthIOException: ${t.message ?: "auth required"}"
            }
            else -> "${t.javaClass.simpleName}: ${t.message ?: "unknown"}"
        }
    }

    fun canUseDriveBackup(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))
    }

    private fun getDriveService(context: Context): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        if (!GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) return null

        val selectedAccount = account.account
            ?: account.email?.let { Account(it, "com.google") }
            ?: run {
                setLastError("GoogleSignInAccount has no valid Account/email. Please sign out and sign in again.")
                return null
            }

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        )
        val accountAssigned = runCatching {
            credential.selectedAccount = selectedAccount
            true
        }.getOrElse {
            setLastError(describeError(it))
            Log.e(TAG, "Failed to set selected Google account", it)
            false
        }
        if (!accountAssigned) return null

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
                val fullSaved = uploadBackupJson(driveService, json)
                if (fullSaved) {
                    setLastError(null)
                    return@withContext true
                }

                // Fallback: if material blobs make the payload too large, save core data only.
                val fallbackData = backupData.copy(studyPdfs = emptyList())
                val fallbackJson = Gson().toJson(fallbackData)
                val fallbackSaved = uploadBackupJson(driveService, fallbackJson)
                if (fallbackSaved) {
                    setLastError("Backup saved without study material files (payload too large or upload rejected).")
                }
                fallbackSaved
            } catch (e: Exception) {
                val reason = describeError(e)
                setLastError(reason)
                Log.e(TAG, "backup() failed: $reason", e)
                false
            }
        }
    }

    suspend fun restore(context: Context): BackupData? {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService(context) ?: return@withContext null
                val backupFile = findLatestBackupFile(driveService) ?: return@withContext null

                val outputStream = ByteArrayOutputStream()
                driveService.files().get(backupFile.id).executeMediaAndDownloadTo(outputStream)
                val json = outputStream.toString("UTF-8")

                val raw = Gson().fromJson(json, BackupData::class.java)
                raw.copy(
                    attendanceHistory = raw.attendanceHistory ?: emptyList(),
                    notes = raw.notes ?: emptyList(),
                    noteChecklistItems = raw.noteChecklistItems ?: emptyList(),
                    studyPdfs = raw.studyPdfs ?: emptyList()
                )
            } catch (e: Exception) {
                Log.e(TAG, "restore() failed: ${describeError(e)}", e)
                null
            }
        }
    }

    suspend fun deleteBackup(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService(context) ?: return@withContext false
                val backupIds = findAllBackupFileIds(driveService)
                backupIds.forEach { driveService.files().delete(it).execute() }
                true
            } catch (e: Exception) {
                Log.e(TAG, "deleteBackup() failed: ${describeError(e)}", e)
                false
            }
        }
    }

    suspend fun deleteStudyPdfFromBackup(
        context: Context,
        pdfId: Int,
        subjectId: Int,
        title: String
    ): Boolean {
        return mutateBackupData(context) { data ->
            val updated = (data.studyPdfs ?: emptyList()).filterNot { blob ->
                blob.item.id == pdfId ||
                    (blob.item.subjectId == subjectId && blob.item.title == title)
            }
            data.copy(studyPdfs = updated)
        }
    }

    suspend fun clearStudyPdfsFromBackup(context: Context): Boolean {
        return mutateBackupData(context) { it.copy(studyPdfs = emptyList()) }
    }

    private suspend fun mutateBackupData(
        context: Context,
        transform: (BackupData) -> BackupData
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService(context) ?: return@withContext false
                val fileId = findLatestBackupFile(driveService)?.id ?: return@withContext true

                val outputStream = ByteArrayOutputStream()
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                val current = Gson().fromJson(outputStream.toString("UTF-8"), BackupData::class.java) ?: return@withContext false

                val normalized = current.copy(
                    attendanceHistory = current.attendanceHistory ?: emptyList(),
                    notes = current.notes ?: emptyList(),
                    noteChecklistItems = current.noteChecklistItems ?: emptyList(),
                    studyPdfs = current.studyPdfs ?: emptyList()
                )

                val updated = transform(normalized)
                val json = Gson().toJson(updated)
                val content = ByteArrayContent.fromString("application/json", json)
                driveService.files().update(fileId, null, content).execute()
                upsertNamedBackupFile(
                    driveService = driveService,
                    fileName = BACKUP_FILE_NAME,
                    json = json
                )
                true
            } catch (e: Exception) {
                Log.e(TAG, "mutateBackupData() failed: ${describeError(e)}", e)
                false
            }
        }
    }

    suspend fun downloadLatestBackup(context: Context): Pair<String, ByteArray>? {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService(context) ?: return@withContext null
                val backupFile = findLatestBackupFile(driveService) ?: return@withContext null
                val outputStream = ByteArrayOutputStream()
                driveService.files().get(backupFile.id).executeMediaAndDownloadTo(outputStream)
                setLastError(null)
                backupFile.name to outputStream.toByteArray()
            } catch (e: Exception) {
                val reason = describeError(e)
                setLastError(reason)
                Log.e(TAG, "downloadLatestBackup() failed: $reason", e)
                null
            }
        }
    }

    private fun uploadBackupJson(driveService: Drive, json: String): Boolean {
        val timestampedSaved = runCatching {
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = newTimestampedBackupFileName()
                parents = listOf("appDataFolder")
            }
            val content = ByteArrayContent.fromString("application/json", json)
            driveService.files().create(fileMetadata, content)
                .setFields("id")
                .execute()
            true
        }.getOrElse {
            setLastError(describeError(it))
            Log.e(TAG, "upload timestamped backup failed: ${describeError(it)}", it)
            false
        }

        val canonicalSaved = upsertNamedBackupFile(
            driveService = driveService,
            fileName = BACKUP_FILE_NAME,
            json = json
        )

        if (timestampedSaved) {
            pruneOldTimestampedBackups(driveService, keepCount = MAX_TIMESTAMPED_BACKUPS)
        }
        return timestampedSaved || canonicalSaved
    }

    private fun findLatestBackupFile(driveService: Drive): com.google.api.services.drive.model.File? {
        return listAllAppDataFiles(driveService)
            ?.asSequence()
            ?.filter { isBackupFileName(it.name) }
            ?.sortedByDescending { it.modifiedTime?.value ?: 0L }
            ?.firstOrNull()
    }

    private fun findAllBackupFileIds(driveService: Drive): List<String> {
        return listAllAppDataFiles(driveService)
            ?.asSequence()
            ?.filter { isBackupFileName(it.name) }
            ?.mapNotNull { it.id }
            ?.toList()
            .orEmpty()
    }

    private fun listAllAppDataFiles(driveService: Drive): List<com.google.api.services.drive.model.File>? {
        val all = mutableListOf<com.google.api.services.drive.model.File>()
        var pageToken: String? = null
        do {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("nextPageToken, files(id, name, modifiedTime)")
                .setPageSize(200)
                .setPageToken(pageToken)
                .execute()
            all += result.files.orEmpty()
            pageToken = result.nextPageToken
        } while (!pageToken.isNullOrBlank())
        return all
    }

    private fun findBackupFileIdByExactName(driveService: Drive, name: String): String? {
        return listAllAppDataFiles(driveService)
            ?.firstOrNull { it.name == name }
            ?.id
    }

    private fun upsertNamedBackupFile(driveService: Drive, fileName: String, json: String): Boolean {
        return runCatching {
            val content = ByteArrayContent.fromString("application/json", json)
            val existingId = findBackupFileIdByExactName(driveService, fileName)
            if (existingId != null) {
                driveService.files().update(existingId, null, content).execute()
            } else {
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = fileName
                    parents = listOf("appDataFolder")
                }
                driveService.files().create(fileMetadata, content)
                    .setFields("id")
                    .execute()
            }
            true
        }.getOrElse {
            setLastError(describeError(it))
            Log.e(TAG, "upsert named backup failed: ${describeError(it)}", it)
            false
        }
    }

    private fun pruneOldTimestampedBackups(driveService: Drive, keepCount: Int) {
        val stale = listAllAppDataFiles(driveService)
            .orEmpty()
            .asSequence()
            .filter { name ->
                val n = name.name.orEmpty()
                n.startsWith(BACKUP_FILE_PREFIX) && n.endsWith(".json") && n != BACKUP_FILE_NAME
            }
            .sortedByDescending { it.modifiedTime?.value ?: 0L }
            .drop(keepCount)
            .mapNotNull { it.id }
            .toList()

        stale.forEach { id ->
            runCatching { driveService.files().delete(id).execute() }
        }
    }

    private fun isBackupFileName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        return name == BACKUP_FILE_NAME || (name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(".json"))
    }

    private fun newTimestampedBackupFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${BACKUP_FILE_PREFIX}${stamp}.json"
    }
}

