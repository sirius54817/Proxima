package com.sirius.proxima.backup

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.model.TimetableEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class BackupData(
    val subjects: List<Subject>,
    val timetableEntries: List<TimetableEntry>
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

    suspend fun backup(context: Context, subjects: List<Subject>, entries: List<TimetableEntry>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService(context) ?: return@withContext false
                val backupData = BackupData(subjects, entries)
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

                Gson().fromJson(json, BackupData::class.java)
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

