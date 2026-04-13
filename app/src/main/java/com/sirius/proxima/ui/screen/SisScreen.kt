package com.sirius.proxima.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.data.sis.SisAttendance
import com.sirius.proxima.ui.theme.*
import com.sirius.proxima.viewmodel.SisUiState
import com.sirius.proxima.viewmodel.SisViewModel

@Composable
fun SisScreen(
    viewModel: SisViewModel = viewModel(
        factory = SisViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedRegisterNo by viewModel.savedRegisterNo.collectAsStateWithLifecycle()

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
                SisLoadingScreen("Logging in...")
            }
            is SisUiState.LoadingAttendance -> {
                SisLoadingScreen("Fetching attendance...")
            }
            is SisUiState.Loaded -> {
                SisAttendanceScreen(
                    attendance = state.attendance,
                    onRefresh = { viewModel.refresh() },
                    onLogout = { viewModel.logout() }
                )
            }
            is SisUiState.Error -> {
                SisErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.refresh() },
                    onLogout = { viewModel.logout() }
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
                    placeholder = { Text("e.g. 99220041611") },
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
private fun SisLoadingScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(text = message, color = MutedForeground)
        }
    }
}

@Composable
private fun SisErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onLogout: () -> Unit
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
            }
        }
    }
}

@Composable
private fun SisAttendanceScreen(
    attendance: List<SisAttendance>,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
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

