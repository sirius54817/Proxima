package com.sirius.proxima.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.data.sis.SisAttendance
import com.sirius.proxima.sis.SisBrowserActivity
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.ui.theme.*
import com.sirius.proxima.viewmodel.SisUiState
import com.sirius.proxima.viewmodel.SisViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SisScreen(
    viewModel: SisViewModel = viewModel(
        factory = SisViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedRegisterNo by viewModel.savedRegisterNo.collectAsStateWithLifecycle()
    val savedPassword by viewModel.savedPassword.collectAsStateWithLifecycle()
    val developerMode by viewModel.developerMode.collectAsStateWithLifecycle()
    val useMaterial3 by viewModel.useMaterial3.collectAsStateWithLifecycle()
    val sisDebugLog by viewModel.sisDebugLog.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AnimatedContent(
        targetState = uiState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "sis_state"
    ) { state ->
        when (state) {
            is SisUiState.LoggedOut -> {
                SisLoginScreen(
                    initialRegisterNo = savedRegisterNo ?: "",
                    onLogin = { regNo, password -> viewModel.login(regNo, password) }
                )
            }
            is SisUiState.LoggingIn -> {
                SisLoadingScreen(useMaterialColors = useMaterial3)
            }
            is SisUiState.LoadingAttendance -> {
                SisLoadingScreen(useMaterialColors = useMaterial3)
            }
            is SisUiState.Loaded -> {
                SisAttendanceScreen(
                    attendance = state.attendance,
                    onRefresh = { viewModel.refresh() },
                    onLogout = { viewModel.logout() },
                    showDeveloperTools = developerMode,
                    onCopyLog = {
                        clipboardManager.setText(AnnotatedString(sisDebugLog.ifBlank { "No SIS logs yet" }))
                        Toast.makeText(context, "SIS log copied", Toast.LENGTH_SHORT).show()
                    },
                    onClearLog = { viewModel.clearSisDebugLog() },
                    onOpenPortalWeb = {
                        if (!savedRegisterNo.isNullOrBlank() && !savedPassword.isNullOrBlank()) {
                            context.startActivity(
                                SisBrowserActivity.createIntent(
                                    context = context,
                                    registerNo = savedRegisterNo!!,
                                    password = savedPassword!!
                                )
                            )
                        }
                    }
                )
            }
            is SisUiState.Error -> {
                SisErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.refresh() },
                    onLogout = { viewModel.logout() },
                    showDeveloperTools = developerMode,
                    onCopyLog = {
                        clipboardManager.setText(AnnotatedString(sisDebugLog.ifBlank { "No SIS logs yet" }))
                        Toast.makeText(context, "SIS log copied", Toast.LENGTH_SHORT).show()
                    },
                    onClearLog = { viewModel.clearSisDebugLog() }
                )
            }
        }
    }
}

@Composable
private fun SisLoginScreen(
    initialRegisterNo: String,
    onLogin: (String, String) -> Unit
) {
    var registerNo by remember { mutableStateOf(initialRegisterNo) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SIS Attendance",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Sign in with your student portal credentials",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedForeground
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                OutlinedTextField(
                    value = registerNo,
                    onValueChange = { registerNo = it },
                    label = { Text("Register Number") },
                    placeholder = { Text("e.g. 992x004xxxx") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (registerNo.isNotBlank() && password.isNotBlank()) {
                                onLogin(registerNo, password)
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = MutedForeground
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onLogin(registerNo, password)
                    },
                    enabled = registerNo.isNotBlank() && password.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign In to SIS")
                }
            }
        }

        Text(
            text = "Your credentials are stored securely on this device only.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedForeground
        )
    }
}

@Composable
private fun SisLoadingScreen(
    useMaterialColors: Boolean
) {
    val letters = "PROXIMA"
    val offsets = remember { letters.map { Animatable(80f) } }
    val alphas = remember { letters.map { Animatable(0f) } }

    val bg = if (useMaterialColors) MaterialTheme.colorScheme.background else Color(0xFF09090B)
    val title = if (useMaterialColors) MaterialTheme.colorScheme.primary else Color.White

    LaunchedEffect(Unit) {
        while (true) {
            letters.indices.forEach { i ->
                offsets[i].snapTo(80f)
                alphas[i].snapTo(0f)
            }

            letters.indices.forEach { index ->
                launch {
                    delay(index * 60L)
                    launch {
                        offsets[index].animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    launch {
                        alphas[index].animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 400)
                        )
                    }
                }
            }

            delay(letters.length * 60L + 1400L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            "PROXIMA".forEachIndexed { index, letter ->
                Text(
                    text = letter.toString(),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = title.copy(alpha = alphas[index].value),
                    letterSpacing = 3.sp,
                    modifier = Modifier.graphicsLayer {
                        translationY = offsets[index].value
                    }
                )
            }
        }
    }
}

@Composable
private fun SisErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    showDeveloperTools: Boolean,
    onCopyLog: () -> Unit,
    onClearLog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "SIS Attendance",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Error", fontWeight = FontWeight.SemiBold, color = DangerRed)
                }
                Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", color = MaterialTheme.colorScheme.onSurface)
                    }
                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Re-login", color = DangerRed)
                    }
                }
                if (showDeveloperTools) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onCopyLog,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Log", color = MaterialTheme.colorScheme.onSurface)
                        }
                        OutlinedButton(
                            onClick = onClearLog,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Log", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SisAttendanceScreen(
    attendance: List<SisAttendance>,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    showDeveloperTools: Boolean,
    onCopyLog: () -> Unit,
    onClearLog: () -> Unit,
    onOpenPortalWeb: () -> Unit
) {
    val overall = if (attendance.isNotEmpty()) {
        val totalPresent = attendance.sumOf { it.present + it.onDuty + it.medicalLeave }
        val totalClasses = attendance.sumOf { it.total }
        if (totalClasses > 0) (totalPresent.toDouble() / totalClasses * 100) else 0.0
    } else 0.0

    val belowThreshold = attendance.count { it.percentage < 75.0 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SIS Attendance",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${attendance.size} subjects",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedForeground
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showDeveloperTools) {
                        IconButton(onClick = onCopyLog) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy SIS Log", tint = MutedForeground)
                        }
                        IconButton(onClick = onClearLog) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear SIS Log", tint = MutedForeground)
                        }
                    }
                    IconButton(onClick = onOpenPortalWeb) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Web", tint = MutedForeground)
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MutedForeground)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MutedForeground)
                    }
                }
            }
        }

        // Summary card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Border),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem(
                        label = "Overall",
                        value = "${"%.1f".format(overall)}%",
                        color = if (overall >= 75.0) AttendanceGreen else AttendanceRed
                    )
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Border)
                    SummaryItem(
                        label = "Subjects",
                        value = "${attendance.size}",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    VerticalDivider(modifier = Modifier.height(40.dp), color = Border)
                    SummaryItem(
                        label = "Below 75%",
                        value = "$belowThreshold",
                        color = if (belowThreshold > 0) AttendanceRed else AttendanceGreen
                    )
                }
            }
        }

        // Attendance cards
        items(attendance, key = { it.courseCode }) { subject ->
            SisAttendanceCard(subject)
        }
    }
}


@Composable
private fun SummaryItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MutedForeground)
    }
}

@Composable
private fun SisAttendanceCard(subject: SisAttendance) {
    val percentage = subject.percentage
    val isBelow = percentage < 75.0
    val percentColor = if (percentage >= 75.0) AttendanceGreen else AttendanceRed
    // SIS treats OD as attended classes for eligibility calculations.
    val effectivePresent = subject.present + subject.onDuty

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isBelow) DangerRed.copy(alpha = 0.4f) else Border),
        colors = CardDefaults.cardColors(
            containerColor = if (isBelow)
                DangerRed.copy(alpha = 0.04f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.courseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${subject.courseCode} · ${subject.credits} credits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedForeground
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${"%.1f".format(percentage)}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = percentColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = percentColor,
                trackColor = Border,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AttendanceStat("Present", subject.present.toString(), AttendanceGreen)
                AttendanceStat("Absent", subject.absent.toString(), AttendanceRed)
                AttendanceStat("Total", subject.total.toString(), MutedForeground)
                if (subject.onDuty > 0) AttendanceStat("OD", subject.onDuty.toString(), MutedForeground)
                if (subject.medicalLeave > 0) AttendanceStat("ML", subject.medicalLeave.toString(), MutedForeground)
            }

            // Warning / info message
            if (isBelow) {
                Spacer(modifier = Modifier.height(8.dp))
                val needed = Math.ceil((0.75 * subject.total - effectivePresent) / 0.25).toInt().coerceAtLeast(0)
                Text(
                    text = "⚠ Need $needed more class${if (needed == 1) "" else "es"} to reach 75%",
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed
                )
            } else if (percentage >= 75.0) {
                val canMiss = ((effectivePresent - 0.75 * subject.total) / 0.75).toInt().coerceAtLeast(0)
                if (canMiss > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ Can miss $canMiss more class${if (canMiss == 1) "" else "es"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AttendanceGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MutedForeground)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun SisAttendanceScreenPreview() {
    ProximaTheme {
        SisAttendanceScreen(
            attendance = listOf(
                SisAttendance("CSE101", "Data Structures", "4", 40, 32, 6, 2, 0, 80.0, ""),
                SisAttendance("MAT201", "Discrete Mathematics", "3", 30, 18, 10, 2, 0, 66.6, "")
            ),
            onRefresh = {},
            onLogout = {},
            showDeveloperTools = false,
            onCopyLog = {},
            onClearLog = {},
            onOpenPortalWeb = {}
        )
    }
}

