package com.sirius.proxima.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import com.sirius.proxima.ui.components.PinKeypad
import com.sirius.proxima.ui.components.ConfirmDialog
import com.sirius.proxima.ui.theme.ThemeMode
import com.sirius.proxima.ui.theme.*
import com.sirius.proxima.viewmodel.SecurityViewModel
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
    val isDownloadingBackup by viewModel.isDownloadingBackup.collectAsStateWithLifecycle()
    val isClearing by viewModel.isClearing.collectAsStateWithLifecycle()
    val sisFeaturesUnlocked by viewModel.sisFeaturesUnlocked.collectAsStateWithLifecycle()
    val sisRestoredFromBackup by viewModel.sisRestoredFromBackup.collectAsStateWithLifecycle()
    val unlockMessage by viewModel.unlockMessage.collectAsStateWithLifecycle()
    val showSpecialModePopup by viewModel.showSpecialModePopup.collectAsStateWithLifecycle()
    val selectedCalendarName by viewModel.selectedCalendarName.collectAsStateWithLifecycle()
    val isSyncingToCalendar by viewModel.isSyncingToCalendar.collectAsStateWithLifecycle()
    val isSyncingFromCalendar by viewModel.isSyncingFromCalendar.collectAsStateWithLifecycle()
    val isClearingCalendar by viewModel.isClearingCalendar.collectAsStateWithLifecycle()
    val isClearingPdfs by viewModel.isClearingPdfs.collectAsStateWithLifecycle()
    val calendarSyncMessage by viewModel.calendarSyncMessage.collectAsStateWithLifecycle()
    val backupDebugLog by viewModel.backupDebugLog.collectAsStateWithLifecycle()
    val showHomeSemesterProgress by viewModel.showHomeSemesterProgress.collectAsStateWithLifecycle()
    val showHomeWeeklyGoalProgress by viewModel.showHomeWeeklyGoalProgress.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val useMaterial3 by viewModel.useMaterial3.collectAsStateWithLifecycle()
    val useMaterialYou by viewModel.useMaterialYou.collectAsStateWithLifecycle()
    val developerMode by viewModel.developerMode.collectAsStateWithLifecycle()
    val securityViewModel: SecurityViewModel = viewModel(
        factory = SecurityViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
    val hasPin by securityViewModel.hasPin.collectAsStateWithLifecycle()
    val appLockEnabled by securityViewModel.appLockEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(unlockMessage) {
        val message = unlockMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.clearUnlockMessage()
    }

    var showClearDialog by remember { mutableStateOf(false) }
    var showClearCalendarDialog by remember { mutableStateOf(false) }
    var showClearPdfsDialog by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var pendingEnableAppLock by remember { mutableStateOf(false) }
    var pinSetupError by remember { mutableStateOf<String?>(null) }
    var pinClearSignal by remember { mutableIntStateOf(0) }
    var pinShakeSignal by remember { mutableIntStateOf(0) }
    var changeStep by remember { mutableIntStateOf(0) }
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var changePinError by remember { mutableStateOf<String?>(null) }
    var changePinClearSignal by remember { mutableIntStateOf(0) }
    var changePinShakeSignal by remember { mutableIntStateOf(0) }
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
            val hasDriveScope = GoogleSignIn.hasPermissions(existingAccount, Scope(DriveScopes.DRIVE_APPDATA))
            if (hasDriveScope) {
                viewModel.onSignInSuccess(
                    name = existingAccount.displayName ?: "User",
                    email = existingAccount.email ?: ""
                )
            } else {
                signInError = "Google account found, but Drive backup permission is missing. Please sign in again."
            }
        }
    }

    SettingsScreenContent(
        accountName = accountName,
        accountEmail = accountEmail,
        lastBackupTime = lastBackupTime,
        isSignedIn = isSignedIn,
        isBackingUp = isBackingUp,
        isDownloadingBackup = isDownloadingBackup,
        isClearing = isClearing,
        signInError = signInError,
        backupDebugLog = backupDebugLog,
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
        onDownloadBackup = { viewModel.downloadBackupFallback() },
        onClearBackupLog = { viewModel.clearBackupDebugLog() },
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
        isClearingPdfs = isClearingPdfs,
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
        onClearAllPdfs = { showClearPdfsDialog = true },
        showHomeSemesterProgress = showHomeSemesterProgress,
        showHomeWeeklyGoalProgress = showHomeWeeklyGoalProgress,
        onSetShowHomeSemesterProgress = viewModel::setShowHomeSemesterProgress,
        onSetShowHomeWeeklyGoalProgress = viewModel::setShowHomeWeeklyGoalProgress,
        appThemeMode = appThemeMode,
        useMaterial3 = useMaterial3,
        useMaterialYou = useMaterialYou,
        developerMode = developerMode,
        onSetAppThemeMode = viewModel::setAppThemeMode,
        onSetUseMaterial3 = viewModel::setUseMaterial3,
        onSetUseMaterialYou = viewModel::setUseMaterialYou,
        onSetDeveloperMode = viewModel::setDeveloperMode,
        hasPin = hasPin,
        appLockEnabled = appLockEnabled,
        onSetAppLockEnabled = { enabled ->
            if (!enabled) {
                securityViewModel.setAppLockEnabled(false)
            } else if (hasPin) {
                securityViewModel.setAppLockEnabled(true)
            } else {
                pendingEnableAppLock = true
                pinSetupError = null
                showPinSetupDialog = true
            }
        },
        onOpenChangePin = {
            changeStep = 0
            currentPinInput = ""
            newPinInput = ""
            changePinError = null
            changePinClearSignal += 1
            showChangePinDialog = true
        }
    )

    if (showPinSetupDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinSetupDialog = false
                pendingEnableAppLock = false
            },
            title = { Text("Set Security PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Create a 6-digit PIN to enable security features.")
                    pinSetupError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    PinKeypad(
                        onPinComplete = { pin ->
                            if (pin.length != 6) {
                                pinSetupError = "PIN must be 6 digits"
                                pinShakeSignal += 1
                                pinClearSignal += 1
                                return@PinKeypad
                            }
                            securityViewModel.setupPin(pin)
                            if (pendingEnableAppLock) securityViewModel.setAppLockEnabled(true)
                            pendingEnableAppLock = false
                            pinSetupError = null
                            showPinSetupDialog = false
                        },
                        clearSignal = pinClearSignal,
                        shakeSignal = pinShakeSignal
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showPinSetupDialog = false
                    pendingEnableAppLock = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePinDialog = false
                changeStep = 0
                currentPinInput = ""
                newPinInput = ""
                changePinError = null
            },
            title = { Text("Change PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        when (changeStep) {
                            0 -> "Enter your current 6-digit PIN"
                            1 -> "Enter your new 6-digit PIN"
                            else -> "Confirm your new 6-digit PIN"
                        }
                    )
                    changePinError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    PinKeypad(
                        onPinComplete = { pin ->
                            when (changeStep) {
                                0 -> {
                                    if (!securityViewModel.verifyPin(pin)) {
                                        changePinError = "Incorrect current PIN"
                                        changePinShakeSignal += 1
                                        changePinClearSignal += 1
                                    } else {
                                        currentPinInput = pin
                                        changePinError = null
                                        changeStep = 1
                                        changePinClearSignal += 1
                                    }
                                }

                                1 -> {
                                    newPinInput = pin
                                    changePinError = null
                                    changeStep = 2
                                    changePinClearSignal += 1
                                }

                                else -> {
                                    if (pin != newPinInput) {
                                        changePinError = "New PINs do not match"
                                        changePinShakeSignal += 1
                                        changePinClearSignal += 1
                                    } else {
                                        val changed = securityViewModel.changePin(currentPinInput, newPinInput)
                                        if (changed) {
                                            Toast.makeText(context, "PIN updated", Toast.LENGTH_SHORT).show()
                                            showChangePinDialog = false
                                        } else {
                                            changePinError = "Failed to update PIN"
                                            changePinShakeSignal += 1
                                            changePinClearSignal += 1
                                        }
                                    }
                                }
                            }
                        },
                        clearSignal = changePinClearSignal,
                        shakeSignal = changePinShakeSignal
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showChangePinDialog = false
                    changeStep = 0
                    currentPinInput = ""
                    newPinInput = ""
                    changePinError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

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

    if (showClearPdfsDialog) {
        ConfirmDialog(
            title = "Delete All Study Material",
            message = "This deletes all study material files from the app and removes them from backup.",
            confirmText = "Delete All Files",
            isDangerous = true,
            onConfirm = {
                viewModel.clearAllStudyPdfs()
                showClearPdfsDialog = false
            },
            onDismiss = { showClearPdfsDialog = false }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreenContent(
    accountName: String? = null,
    accountEmail: String? = null,
    lastBackupTime: Long = 0L,
    isSignedIn: Boolean = false,
    isBackingUp: Boolean = false,
    isDownloadingBackup: Boolean = false,
    isClearing: Boolean = false,
    signInError: String? = null,
    backupDebugLog: String = "",
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onBackupNow: () -> Unit = {},
    onDownloadBackup: () -> Unit = {},
    onClearBackupLog: () -> Unit = {},
    onClearData: () -> Unit = {},
    onTestNotification: () -> Unit = {},
    sisFeaturesUnlocked: Boolean = false,
    sisRestoredFromBackup: Boolean = false,
    onVersionTapped: () -> Unit = {},
    selectedCalendarName: String? = null,
    isSyncingToCalendar: Boolean = false,
    isSyncingFromCalendar: Boolean = false,
    isClearingCalendar: Boolean = false,
    isClearingPdfs: Boolean = false,
    onSyncToCalendar: () -> Unit = {},
    onSyncFromCalendar: () -> Unit = {},
    onClearCalendarData: () -> Unit = {},
    onClearAllPdfs: () -> Unit = {},
    showHomeSemesterProgress: Boolean = true,
    showHomeWeeklyGoalProgress: Boolean = true,
    onSetShowHomeSemesterProgress: (Boolean) -> Unit = {},
    onSetShowHomeWeeklyGoalProgress: (Boolean) -> Unit = {},
    appThemeMode: ThemeMode = ThemeMode.SYSTEM,
    useMaterial3: Boolean = false,
    useMaterialYou: Boolean = false,
    developerMode: Boolean = false,
    onSetAppThemeMode: (ThemeMode) -> Unit = {},
    onSetUseMaterial3: (Boolean) -> Unit = {},
    onSetUseMaterialYou: (Boolean) -> Unit = {},
    onSetDeveloperMode: (Boolean) -> Unit = {},
    hasPin: Boolean = false,
    appLockEnabled: Boolean = false,
    onSetAppLockEnabled: (Boolean) -> Unit = {},
    onOpenChangePin: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    var currentPage by rememberSaveable { mutableStateOf(SettingsPage.Root) }

    if (currentPage != SettingsPage.Root) {
        BackHandler { currentPage = SettingsPage.Root }
    }

    val pages = listOf(
        SettingsPage.Account,
        SettingsPage.Backup,
        SettingsPage.Notifications,
        SettingsPage.Home,
        SettingsPage.Calendar,
        SettingsPage.Security,
        SettingsPage.About,
        SettingsPage.Danger
    )

    if (currentPage == SettingsPage.Root) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    text = "Tap a category to open settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedForeground
                )
            }

            pages.forEach { page ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentPage = page },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Border),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = page.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = page.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedForeground
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open ${page.title}",
                                tint = MutedForeground
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(currentPage.title) },
                navigationIcon = {
                    IconButton(onClick = { currentPage = SettingsPage.Root }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = currentPage.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedForeground
                )
            }

            when (currentPage) {
                SettingsPage.Account -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Border),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (isSignedIn && accountName != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                }

                SettingsPage.Backup -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Border),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
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

                                Row(verticalAlignment = Alignment.CenterVertically) {
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

                                Text(
                                    text = "Each cloud backup is saved with a timestamped filename.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedForeground
                                )

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
                                    onClick = onDownloadBackup,
                                    enabled = !isBackingUp && !isDownloadingBackup,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isDownloadingBackup) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Downloading backup...", color = MaterialTheme.colorScheme.onSurface)
                                    } else {
                                        Icon(
                                            Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Download Backup Manually", color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Button(
                                    onClick = onBackupNow,
                                    enabled = !isBackingUp && !isDownloadingBackup,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isBackingUp) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Backing up...", color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Icon(
                                            Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Backup Now", color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }

                                if (developerMode) {
                                    HorizontalDivider(color = Border, thickness = 0.5.dp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(backupDebugLog.ifBlank { "No backup logs yet" }))
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Border),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy Log")
                                        }

                                        OutlinedButton(
                                            onClick = onClearBackupLog,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Border),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Clear Log")
                                        }
                                    }

                                    Text(
                                        text = if (backupDebugLog.isBlank()) "No backup logs yet. Tap Backup Now, then Copy Log."
                                        else backupDebugLog.lines().takeLast(8).joinToString("\n"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedForeground
                                    )
                                }
                            }
                        }
                    }
                }

                SettingsPage.Notifications -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Border),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
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
                }

                SettingsPage.Home -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Border),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "App Theme",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ThemeMode.entries.forEach { mode ->
                                        val selected = appThemeMode == mode
                                        OutlinedButton(
                                            onClick = { onSetAppThemeMode(mode) },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) MaterialTheme.colorScheme.primary else Border
                                            ),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (selected) {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                }
                                            )
                                        ) {
                                            Text(
                                                text = when (mode) {
                                                    ThemeMode.SYSTEM -> "System"
                                                    ThemeMode.LIGHT -> "Light"
                                                    ThemeMode.DARK -> "Dark"
                                                }
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Default is System. It follows your phone theme automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedForeground
                                )

                                HorizontalDivider(color = Border, thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Material 3 theme")
                                        Text(
                                            text = "Enable Material 3 styling (off by default)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedForeground
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Switch(
                                        checked = useMaterial3,
                                        onCheckedChange = onSetUseMaterial3
                                    )
                                }

                                HorizontalDivider(color = Border, thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Material You colors")
                                        Text(
                                            text = "Use dynamic colors from your wallpaper (Android 12+)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedForeground
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Switch(
                                        checked = useMaterialYou,
                                        onCheckedChange = onSetUseMaterialYou,
                                        enabled = useMaterial3
                                    )
                                }

                                if (!useMaterial3) {
                                    Text(
                                        text = "Turn on Material 3 to use Material You colors.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedForeground
                                    )
                                }

                                HorizontalDivider(color = Border, thickness = 0.5.dp)

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
                }

                SettingsPage.Calendar -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Border),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
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
                }

                SettingsPage.About -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Border),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
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

                                HorizontalDivider(color = Border, thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Developer mode")
                                        Text(
                                            text = "Show diagnostic copy-log tools",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedForeground
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Switch(
                                        checked = developerMode,
                                        onCheckedChange = onSetDeveloperMode
                                    )
                                }
                            }
                        }
                    }
                }

                SettingsPage.Security -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Border),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "App Security",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("App Lock")
                                        Text(
                                            text = "Require PIN/biometric when reopening app",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedForeground
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Switch(
                                        checked = appLockEnabled,
                                        onCheckedChange = onSetAppLockEnabled
                                    )
                                }

                                if (hasPin) {
                                    OutlinedButton(
                                        onClick = onOpenChangePin,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Border),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockReset,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Change PIN")
                                    }
                                }

                                if (!hasPin) {
                                    Text(
                                        text = "Enable App Lock to create your PIN.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedForeground
                                    )
                                }
                            }
                        }
                    }
                }

                SettingsPage.Danger -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
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
                                    onClick = onClearAllPdfs,
                                    enabled = !isClearingPdfs,
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.85f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isClearingPdfs) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onError
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Deleting files...")
                                    } else {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Delete All Study Material")
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = onClearCalendarData,
                                    enabled = !isClearingCalendar,
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.85f)),
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
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
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

                SettingsPage.Root -> Unit
            }
        }
    }
}

private enum class SettingsPage(val title: String, val description: String) {
    Root("Settings", "Manage your account and data"),
    Account("Account", "Sign in, account details, and sign out"),
    Backup("Backup", "Cloud backup status and manual backup"),
    Notifications("Notifications", "Notification testing and behavior"),
    Home("Customize", "Appearance and home section settings"),
    Calendar("Google Calendar Sync", "Sync timetable to and from Google Calendar"),
    Security("Security", "App lock and biometric unlock"),
    About("About", "Version and app information"),
    Danger("Danger Zone", "Destructive actions and data clearing")
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


















