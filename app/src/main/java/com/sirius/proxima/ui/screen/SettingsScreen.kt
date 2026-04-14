package com.sirius.proxima.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.sirius.proxima.ui.components.ConfirmDialog
import com.sirius.proxima.ui.theme.*
import com.sirius.proxima.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val context = LocalContext.current
    val accountName by viewModel.googleAccountName.collectAsStateWithLifecycle()
    val accountEmail by viewModel.googleAccountEmail.collectAsStateWithLifecycle()
    val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val isBackingUp by viewModel.isBackingUp.collectAsStateWithLifecycle()
    val isClearing by viewModel.isClearing.collectAsStateWithLifecycle()
    val sisFeaturesUnlocked by viewModel.sisFeaturesUnlocked.collectAsStateWithLifecycle()
    val sisRestoredFromBackup by viewModel.sisRestoredFromBackup.collectAsStateWithLifecycle()
    val unlockMessage by viewModel.unlockMessage.collectAsStateWithLifecycle()
    val showSpecialModePopup by viewModel.showSpecialModePopup.collectAsStateWithLifecycle()
    val selectedCalendarName by viewModel.selectedCalendarName.collectAsStateWithLifecycle()
    val isSyncingToCalendar by viewModel.isSyncingToCalendar.collectAsStateWithLifecycle()
    val isSyncingFromCalendar by viewModel.isSyncingFromCalendar.collectAsStateWithLifecycle()
    val isClearingCalendar by viewModel.isClearingCalendar.collectAsStateWithLifecycle()
    val calendarSyncMessage by viewModel.calendarSyncMessage.collectAsStateWithLifecycle()
    val showHomeSemesterProgress by viewModel.showHomeSemesterProgress.collectAsStateWithLifecycle()
    val showHomeWeeklyGoalProgress by viewModel.showHomeWeeklyGoalProgress.collectAsStateWithLifecycle()

    LaunchedEffect(unlockMessage) {
        val message = unlockMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.clearUnlockMessage()
    }

    var showClearDialog by remember { mutableStateOf(false) }
    var showClearCalendarDialog by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CALENDAR] == true
        val writeGranted = permissions[Manifest.permission.WRITE_CALENDAR] == true
        hasCalendarPermission = readGranted && writeGranted
        if (!hasCalendarPermission) {
            Toast.makeText(context, "Calendar permission is required for sync", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(calendarSyncMessage) {
        val message = calendarSyncMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.clearCalendarSyncMessage()
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            viewModel.onSignInSuccess(
                name = account.displayName ?: "User",
                email = account.email ?: ""
            )
            signInError = null
        } catch (e: com.google.android.gms.common.api.ApiException) {
            android.util.Log.e("SettingsScreen", "Sign-in failed: code=${e.statusCode}", e)
            signInError = when (e.statusCode) {
                12500 -> "Sign-in failed. Please check Google Play Services and try again."
                12501 -> "Sign-in cancelled."
                12502 -> "Sign-in already in progress."
                10 -> "Developer error: OAuth client not configured. Check SHA-1 fingerprint in Google Cloud Console."
                else -> "Sign-in failed (code: ${e.statusCode}). ${e.message}"
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsScreen", "Sign-in failed", e)
            signInError = "Sign-in failed: ${e.message}"
        }
    }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
    }

    // Check if already signed in on first composition
    LaunchedEffect(Unit) {
        val existingAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (existingAccount != null && !isSignedIn) {
            viewModel.onSignInSuccess(
                name = existingAccount.displayName ?: "User",
                email = existingAccount.email ?: ""
            )
        }
    }

    SettingsScreenContent(
        accountName = accountName,
        accountEmail = accountEmail,
        lastBackupTime = lastBackupTime,
        isSignedIn = isSignedIn,
        isBackingUp = isBackingUp,
        isClearing = isClearing,
        signInError = signInError,
        onSignIn = {
            signInError = null
            val client = GoogleSignIn.getClient(context, gso)
            // Sign out first to force account chooser
            client.signOut().addOnCompleteListener {
                signInLauncher.launch(client.signInIntent)
            }
        },
        onSignOut = {
            val client = GoogleSignIn.getClient(context, gso)
            client.revokeAccess().addOnCompleteListener {
                client.signOut().addOnCompleteListener {
                    viewModel.onSignOut()
                }
            }
        },
        onBackupNow = { viewModel.backupNow() },
        onClearData = { showClearDialog = true },
        onTestNotification = {
            com.sirius.proxima.notification.NotificationHelper.sendDemoNotification(context)
        },
        sisFeaturesUnlocked = sisFeaturesUnlocked,
        sisRestoredFromBackup = sisRestoredFromBackup,
        onVersionTapped = { viewModel.onVersionTapped() },
        selectedCalendarName = selectedCalendarName,
        isSyncingToCalendar = isSyncingToCalendar,
        isSyncingFromCalendar = isSyncingFromCalendar,
        isClearingCalendar = isClearingCalendar,
        onSyncToCalendar = {
            if (hasCalendarPermission) viewModel.syncTimetableToGoogleCalendar()
            else calendarPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            )
        },
        onSyncFromCalendar = {
            if (hasCalendarPermission) viewModel.syncFromGoogleCalendar()
            else calendarPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            )
        },
        onClearCalendarData = { showClearCalendarDialog = true },
        showHomeSemesterProgress = showHomeSemesterProgress,
        showHomeWeeklyGoalProgress = showHomeWeeklyGoalProgress,
        onSetShowHomeSemesterProgress = viewModel::setShowHomeSemesterProgress,
        onSetShowHomeWeeklyGoalProgress = viewModel::setShowHomeWeeklyGoalProgress
    )

    if (showClearDialog) {
        ConfirmDialog(
            title = "Clear All Data",
            message = "This will delete all data from the app AND your Google Drive backup. This cannot be undone.",
            confirmText = "Clear All Data",
            isDangerous = true,
            onConfirm = {
                viewModel.clearAllData()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    if (showSpecialModePopup) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSpecialModePopup() },
            title = { Text("Special Mode") },
            text = { Text("Active in special mode") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSpecialModePopup() }) {
                    Text("Nice")
                }
            }
        )
    }

    if (showClearCalendarDialog) {
        ConfirmDialog(
            title = "Clear App Calendar Data",
            message = "This removes only events created by Proxima from Google Calendar.",
            confirmText = "Clear Calendar Data",
            isDangerous = true,
            onConfirm = {
                if (hasCalendarPermission) viewModel.clearAppCalendarDataOnly()
                else calendarPermissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                )
                showClearCalendarDialog = false
            },
            onDismiss = { showClearCalendarDialog = false }
        )
    }
}

@Composable
fun SettingsScreenContent(
    accountName: String? = null,
    accountEmail: String? = null,
    lastBackupTime: Long = 0L,
    isSignedIn: Boolean = false,
    isBackingUp: Boolean = false,
    isClearing: Boolean = false,
    signInError: String? = null,
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onBackupNow: () -> Unit = {},
    onClearData: () -> Unit = {},
    onTestNotification: () -> Unit = {},
    sisFeaturesUnlocked: Boolean = false,
    sisRestoredFromBackup: Boolean = false,
    onVersionTapped: () -> Unit = {},
    selectedCalendarName: String? = null,
    isSyncingToCalendar: Boolean = false,
    isSyncingFromCalendar: Boolean = false,
    isClearingCalendar: Boolean = false,
    onSyncToCalendar: () -> Unit = {},
    onSyncFromCalendar: () -> Unit = {},
    onClearCalendarData: () -> Unit = {},
    showHomeSemesterProgress: Boolean = true,
    showHomeWeeklyGoalProgress: Boolean = true,
    onSetShowHomeSemesterProgress: (Boolean) -> Unit = {},
    onSetShowHomeWeeklyGoalProgress: (Boolean) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage your account and data",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedForeground
            )
        }

        // Account Section
        item {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Border),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (isSignedIn && accountName != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MutedForeground
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = accountName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = accountEmail ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedForeground
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out", color = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        Text(
                            text = "Sign in with Google to enable cloud backup",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedForeground
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onSignIn,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google")
                        }
                        if (signInError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = signInError,
                                style = MaterialTheme.typography.bodySmall,
                                color = DangerRed
                            )
                        }
                    }
                }
            }
        }

        // Backup Section
        item {
            Text(
                text = "Backup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Border),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Last Backup",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lastBackupTime > 0) {
                                    SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                                        .format(Date(lastBackupTime))
                                } else "Never",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedForeground
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = if (lastBackupTime > 0) AttendanceGreen else MutedForeground,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    HorizontalDivider(color = Border, thickness = 0.5.dp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MutedForeground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auto backup every night at 2:00 AM",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedForeground
                        )
                    }

                    if (sisFeaturesUnlocked && sisRestoredFromBackup) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = AttendanceGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SIS credentials restored from backup",
                                style = MaterialTheme.typography.bodySmall,
                                color = AttendanceGreen
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onBackupNow,
                        enabled = isSignedIn && !isBackingUp,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backing up...", color = MaterialTheme.colorScheme.onSurface)
                        } else {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backup Now", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // Notifications Section
        item {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Border),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Class reminders fire 5 minutes before each scheduled class.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedForeground
                    )
                    OutlinedButton(
                        onClick = onTestNotification,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Test Notification", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Home Section
        item {
            Text(
                text = "Home",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Border),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Semester Progress")
                        Switch(
                            checked = showHomeSemesterProgress,
                            onCheckedChange = onSetShowHomeSemesterProgress
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Weekly Goal")
                        Switch(
                            checked = showHomeWeeklyGoalProgress,
                            onCheckedChange = onSetShowHomeWeeklyGoalProgress
                        )
                    }
                }
            }
        }

        // Google Calendar Sync Section
        item {
            Text(
                text = "Google Calendar Sync",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Border),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Selected: ${selectedCalendarName ?: "Auto-detect Google Calendar"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedForeground
                    )
                    OutlinedButton(
                        onClick = onSyncToCalendar,
                        enabled = !isSyncingToCalendar && !isSyncingFromCalendar,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncingToCalendar) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing to Calendar...")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Timetable to Google Calendar")
                        }
                    }

                    OutlinedButton(
                        onClick = onSyncFromCalendar,
                        enabled = !isSyncingFromCalendar && !isSyncingToCalendar,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncingFromCalendar) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing from Calendar...")
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync from Google Calendar")
                        }
                    }
                }
            }
        }

        // About Section
        item {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Border),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Proxima",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "v1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (sisFeaturesUnlocked) AttendanceGreen else MutedForeground,
                            modifier = Modifier.clickable { onVersionTapped() }
                        )
                    }
                    Text(
                        text = if (sisFeaturesUnlocked) {
                            "Attendance tracker, class scheduler, SIS enabled"
                        } else {
                            "Attendance tracker & class scheduler"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedForeground
                    )
                }
            }
        }

        // Danger Zone
        item {
            Text(
                text = "Danger Zone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = DangerRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(
                    containerColor = DangerRed.copy(alpha = 0.05f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Clear All Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = DangerRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Delete all subjects, timetable entries, and cloud backup. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedForeground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onClearCalendarData,
                        enabled = !isClearingCalendar,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DangerRed.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isClearingCalendar) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clearing Calendar...")
                        } else {
                            Icon(
                                Icons.Default.EventBusy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear App Calendar Data")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onClearData,
                        enabled = !isClearing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DangerRed
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isClearing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clearing...")
                        } else {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear All Data")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
fun SettingsScreenSignedOutPreview() {
    ProximaTheme {
        SettingsScreenContent(
            isSignedIn = false,
            lastBackupTime = 0L
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
fun SettingsScreenSignedInPreview() {
    ProximaTheme {
        SettingsScreenContent(
            accountName = "John Doe",
            accountEmail = "john@example.com",
            lastBackupTime = System.currentTimeMillis(),
            isSignedIn = true
        )
    }
}


















