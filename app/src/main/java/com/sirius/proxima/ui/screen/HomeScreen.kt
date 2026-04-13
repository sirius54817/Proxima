package com.sirius.proxima.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.data.model.TimetableEntry
import com.sirius.proxima.ui.components.AddEditSubjectDialog
import com.sirius.proxima.ui.components.ConfirmDialog
import com.sirius.proxima.ui.components.SubjectCard
import com.sirius.proxima.ui.components.formatHour
import com.sirius.proxima.ui.theme.*
import com.sirius.proxima.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigateToSubjectHistory: (Int) -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val todayEntries by viewModel.todayEntries.collectAsStateWithLifecycle()
    val todayFormatted by viewModel.todayFormatted.collectAsStateWithLifecycle()

    // Refresh date when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshDate()
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var deletingSubject by remember { mutableStateOf<Subject?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        HomeScreenContent(
            modifier = Modifier.padding(padding),
            todayFormatted = todayFormatted,
            todayEntries = todayEntries,
            subjects = subjects,
            onGetPercentage = { viewModel.getSubjectPercentage(it) },
            onMarkPresent = { viewModel.markPresent(it) },
            onMarkAbsent = { viewModel.markAbsent(it) },
            onMarkOnDuty = { viewModel.markOnDuty(it) },
            onEdit = { editingSubject = it },
            onDelete = { deletingSubject = it },
            onSubjectClick = { onNavigateToSubjectHistory(it.id) }
        )
    }

    // Add dialog
    if (showAddDialog) {
        AddEditSubjectDialog(
            onSave = { name, total, attended ->
                viewModel.addSubject(name, total, attended)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Edit dialog
    editingSubject?.let { subject ->
        AddEditSubjectDialog(
            subject = subject,
            onSave = { name, total, attended ->
                viewModel.updateSubject(
                    subject.copy(name = name, totalClasses = total, attendedClasses = attended)
                )
                editingSubject = null
            },
            onDismiss = { editingSubject = null }
        )
    }

    // Delete confirmation
    deletingSubject?.let { subject ->
        ConfirmDialog(
            title = "Delete Subject",
            message = "Are you sure you want to delete \"${subject.name}\"? Timetable entries for this subject will show as [Deleted Subject].",
            confirmText = "Delete",
            isDangerous = true,
            onConfirm = {
                viewModel.deleteSubject(subject)
                deletingSubject = null
            },
            onDismiss = { deletingSubject = null }
        )
    }

}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    todayFormatted: String,
    todayEntries: List<TimetableEntry>,
    subjects: List<Subject>,
    onGetPercentage: (Int?) -> Float,
    onMarkPresent: (Int) -> Unit,
    onMarkAbsent: (Int) -> Unit,
    onMarkOnDuty: (Int) -> Unit,
    onEdit: (Subject) -> Unit,
    onDelete: (Subject) -> Unit,
    onSubjectClick: (Subject) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Text(
                text = "Proxima",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = todayFormatted,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedForeground
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Today's Timetable Section
        item {
            Text(
                text = "Today's Classes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (todayEntries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Border),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No classes today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedForeground
                        )
                    }
                }
            }
        } else {
            items(todayEntries, key = { "entry_${it.id}" }) { entry ->
                TodayEntryCard(
                    entry = entry,
                    percentage = onGetPercentage(entry.subjectId)
                )
            }
        }

        // Subjects Section
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Subjects",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (subjects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Border),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No subjects added yet. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedForeground
                        )
                    }
                }
            }
        } else {
            items(subjects, key = { "subject_${it.id}" }) { subject ->
                SubjectCard(
                    subject = subject,
                    onMarkPresent = { onMarkPresent(subject.id) },
                    onMarkAbsent = { onMarkAbsent(subject.id) },
                    onMarkOnDuty = { onMarkOnDuty(subject.id) },
                    onEdit = { onEdit(subject) },
                    onClick = { onSubjectClick(subject) }
                )
            }
        }
    }
}


@Composable
fun TodayEntryCard(
    entry: TimetableEntry,
    percentage: Float,
    modifier: Modifier = Modifier
) {
    val isDeleted = entry.subjectName == "[Deleted Subject]"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time
            Text(
                text = formatHour(entry.hourSlot),
                style = MaterialTheme.typography.bodySmall,
                color = MutedForeground,
                modifier = Modifier.width(72.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.subjectName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDeleted) Muted else MaterialTheme.colorScheme.onSurface
                )
                if (entry.classNumber.isNotBlank()) {
                    Text(
                        text = entry.classNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedForeground
                    )
                }
            }

            if (!isDeleted) {
                val percentColor = if (percentage >= 75f) AttendanceGreen else AttendanceRed
                Text(
                    text = "${"%.0f".format(percentage)}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = percentColor
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
fun HomeScreenContentPreview() {
    ProximaTheme {
        HomeScreenContent(
            todayFormatted = "Friday, March 6, 2026",
            todayEntries = listOf(
                TimetableEntry(1, 5, 9, 1, "Mathematics", "Room 301"),
                TimetableEntry(2, 5, 11, 2, "Physics", "Lab 2"),
            ),
            subjects = listOf(
                Subject(1, "Mathematics", 40, 35),
                Subject(2, "Physics", 40, 25),
                Subject(3, "Chemistry", 30, 28),
            ),
            onGetPercentage = { if (it == 1) 87.5f else if (it == 2) 62.5f else 93.3f },
            onMarkPresent = {},
            onMarkAbsent = {},
            onMarkOnDuty = {},
            onEdit = {},
            onDelete = {},
            onSubjectClick = {}
        )
    }
}




