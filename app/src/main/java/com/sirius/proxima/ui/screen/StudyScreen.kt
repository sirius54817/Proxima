package com.sirius.proxima.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    onOpenNotes: () -> Unit,
    onOpenStudyPdfs: () -> Unit,
    onOpenFocusMode: () -> Unit,
    viewModel: StudyViewModel = viewModel(
        factory = StudyViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val weeklyGoalMinutes by viewModel.weeklyGoalMinutes.collectAsStateWithLifecycle()
    val weeklyStudiedMinutes by viewModel.weeklyStudiedMinutes.collectAsStateWithLifecycle()
    val streakDays by viewModel.studyStreakDays.collectAsStateWithLifecycle()

    var showGoalDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Study") }) }) { innerPadding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val progress = (weeklyStudiedMinutes.toFloat() / weeklyGoalMinutes.toFloat()).coerceIn(0f, 1f)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { showGoalDialog = true }),
                    border = BorderStroke(1.dp, Border),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Weekly Goal", fontWeight = FontWeight.SemiBold)
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${weeklyStudiedMinutes / 60}h ${(weeklyStudiedMinutes % 60)}m / ${weeklyGoalMinutes / 60}h")
                            Text("Streak $streakDays d", color = MutedForeground)
                        }
                        Text("Tap to change goal", color = MutedForeground, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                DashboardCard(
                    title = "Focus Mode",
                    subtitle = "Start session, timer, and session history",
                    onClick = onOpenFocusMode
                )
            }

            item {
                DashboardCard(
                    title = "Notes",
                    subtitle = "Open categorized notes page",
                    onClick = onOpenNotes
                )
            }

            item {
                DashboardCard(
                    title = "Study PDF",
                    subtitle = "Store and open PDFs by subject",
                    onClick = onOpenStudyPdfs
                )
            }
        }
    }

    if (showGoalDialog) {
        var goalInput by remember(weeklyGoalMinutes) { mutableStateOf((weeklyGoalMinutes / 60).toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set weekly goal") },
            text = {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it.filter(Char::isDigit) },
                    label = { Text("Hours per week") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    goalInput.toIntOrNull()?.let { viewModel.saveWeeklyGoalHours(it.coerceAtLeast(1)) }
                    showGoalDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DashboardCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MutedForeground, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = title)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun StudyScreenPreview() {
    ProximaTheme {
        StudyScreen(onOpenNotes = {}, onOpenStudyPdfs = {}, onOpenFocusMode = {})
    }
}
