package com.sirius.proxima.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.viewmodel.StudyViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(
    onBack: () -> Unit,
    viewModel: StudyViewModel = viewModel(
        factory = StudyViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val subjectTotals by viewModel.subjectTotals.collectAsStateWithLifecycle()

    var selectedSubjectId by remember(subjects) { mutableStateOf(subjects.firstOrNull()?.id) }
    var runningStartMillis by remember { mutableLongStateOf(0L) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(runningStartMillis) {
        while (runningStartMillis > 0L) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                StudyTimerCard(
                    subjects = subjects,
                    selectedSubjectId = selectedSubjectId,
                    onSelectSubject = { selectedSubjectId = it },
                    elapsedSeconds = if (runningStartMillis > 0L) (nowMillis - runningStartMillis) / 1000 else 0L,
                    isRunning = runningStartMillis > 0L,
                    onToggle = {
                        val activeSubject = selectedSubjectId ?: return@StudyTimerCard
                        if (runningStartMillis > 0L) {
                            viewModel.addSession(
                                subjectId = activeSubject,
                                startedAtMillis = runningStartMillis,
                                durationSeconds = ((System.currentTimeMillis() - runningStartMillis) / 1000).coerceAtLeast(1)
                            )
                            runningStartMillis = 0L
                        } else {
                            runningStartMillis = System.currentTimeMillis()
                        }
                    }
                )
            }

            item { Text("Per Subject Study Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (subjectTotals.isEmpty()) {
                item { Text("No sessions yet", color = MutedForeground) }
            } else {
                items(subjectTotals.take(8), key = { it.first.id }) { (subject, seconds) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Border),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(subject.name)
                            Text(formatDuration(seconds), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item { Text("Session History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (sessions.isEmpty()) {
                item { Text("No sessions logged", color = MutedForeground) }
            } else {
                items(sessions.take(30), key = { it.id }) { session ->
                    val name = subjects.firstOrNull { it.id == session.subjectId }?.name ?: "Unknown"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(name, fontWeight = FontWeight.SemiBold)
                            Text("${session.sessionDate} - ${formatDuration(session.durationSeconds)}", color = MutedForeground)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyTimerCard(
    subjects: List<Subject>,
    selectedSubjectId: Int?,
    onSelectSubject: (Int) -> Unit,
    elapsedSeconds: Long,
    isRunning: Boolean,
    onToggle: () -> Unit
) {
    Card(
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Study Timer", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(subjects.firstOrNull { it.id == selectedSubjectId }?.name ?: "Select Subject")
                    }
                    androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        subjects.forEach { subject ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(subject.name) },
                                onClick = {
                                    onSelectSubject(subject.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Text(formatDuration(elapsedSeconds), fontWeight = FontWeight.Bold)
            }
            Button(onClick = onToggle, enabled = selectedSubjectId != null && subjects.isNotEmpty()) {
                Text(if (isRunning) "Stop Session" else "Start Session")
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun FocusModeScreenPreview() {
    ProximaTheme {
        FocusModeScreen(onBack = {})
    }
}

