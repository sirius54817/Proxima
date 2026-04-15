package com.sirius.proxima.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.sirius.proxima.data.model.PlannerEventType
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
    val examCountdowns by viewModel.examCountdowns.collectAsStateWithLifecycle()
    val semesterProgress by viewModel.semesterProgress.collectAsStateWithLifecycle()
    val weekOverview by viewModel.weekOverview.collectAsStateWithLifecycle()
    val semesterStartDate by viewModel.semesterStartDate.collectAsStateWithLifecycle()
    val semesterEndDate by viewModel.semesterEndDate.collectAsStateWithLifecycle()
    val showSemesterProgressCard by viewModel.showSemesterProgressCard.collectAsStateWithLifecycle()
    val showExamCountdownCard by viewModel.showExamCountdownCard.collectAsStateWithLifecycle()
    val showWeekOverviewCard by viewModel.showWeekOverviewCard.collectAsStateWithLifecycle()
    val showWeeklyGoalProgress by viewModel.showWeeklyGoalProgress.collectAsStateWithLifecycle()
    val tomorrowHolidayText by viewModel.tomorrowHolidayText.collectAsStateWithLifecycle()
    val weeklyGoalMinutes by viewModel.weeklyGoalMinutes.collectAsStateWithLifecycle()
    val weeklyStudiedMinutes by viewModel.weeklyStudiedMinutes.collectAsStateWithLifecycle()

    // Refresh date when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshDate()
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var deletingSubject by remember { mutableStateOf<Subject?>(null) }
    var showCustomEventDialog by remember { mutableStateOf(false) }
    var isSubjectEditMode by remember { mutableStateOf(false) }

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
            onHide = { viewModel.hideSubject(it) },
            onUnhide = { viewModel.unhideSubject(it) },
            isSubjectEditMode = isSubjectEditMode,
            onEnterSubjectEditMode = {
                isSubjectEditMode = true
            },
            onExitSubjectEditMode = {
                isSubjectEditMode = false
            },
            onEdit = { editingSubject = it },
            onDelete = { deletingSubject = it },
            onSubjectClick = { onNavigateToSubjectHistory(it.id) },
            examCountdowns = examCountdowns,
            semesterProgress = semesterProgress,
            weekOverview = weekOverview,
            onAddCustomEvent = { showCustomEventDialog = true },
            onDeleteCustomEvent = { it.plannerEvent?.let(viewModel::deleteCustomEvent) },
            semesterStartDate = semesterStartDate,
            semesterEndDate = semesterEndDate,
            showSemesterProgressCard = showSemesterProgressCard,
            showExamCountdownCard = showExamCountdownCard,
            showWeekOverviewCard = showWeekOverviewCard,
            showWeeklyGoalProgress = showWeeklyGoalProgress,
            tomorrowHolidayText = tomorrowHolidayText,
            weeklyGoalMinutes = weeklyGoalMinutes,
            weeklyStudiedMinutes = weeklyStudiedMinutes,
            onSetSemesterDates = viewModel::setSemesterDates
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

    if (showCustomEventDialog) {
        AddCustomEventDialog(
            onDismiss = { showCustomEventDialog = false },
            onSave = { title, date, type ->
                viewModel.addCustomEvent(title, date, type)
                showCustomEventDialog = false
            }
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
    onHide: (Int) -> Unit,
    onUnhide: (Int) -> Unit,
    isSubjectEditMode: Boolean,
    onEnterSubjectEditMode: () -> Unit,
    onExitSubjectEditMode: () -> Unit,
    onEdit: (Subject) -> Unit,
    onDelete: (Subject) -> Unit,
    onSubjectClick: (Subject) -> Unit,
    examCountdowns: List<HomeViewModel.ExamCountdownItem>,
    semesterProgress: Float,
    weekOverview: List<HomeViewModel.HomeWeekEvent>,
    onAddCustomEvent: () -> Unit,
    onDeleteCustomEvent: (HomeViewModel.HomeWeekEvent) -> Unit,
    semesterStartDate: String,
    semesterEndDate: String,
    showSemesterProgressCard: Boolean,
    showExamCountdownCard: Boolean,
    showWeekOverviewCard: Boolean,
    showWeeklyGoalProgress: Boolean,
    tomorrowHolidayText: String?,
    weeklyGoalMinutes: Int,
    weeklyStudiedMinutes: Long,
    onSetSemesterDates: (String, String) -> Unit
) {
    var showSemesterDateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    val examCountdownText = examCountdowns
                        .minByOrNull { it.daysRemaining }
                        ?.let { "Exam: ${it.subject} in ${it.daysRemaining} day(s) (${it.examDate})" }
                    val weekOverviewText = weekOverview
                        .sortedBy { it.date }
                        .firstOrNull()
                        ?.let { "Week: ${it.date} - ${it.title}" }

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
                    if (!tomorrowHolidayText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tomorrowHolidayText,
                            style = MaterialTheme.typography.bodySmall,
                            color = AttendanceGreen
                        )
                    }
                    if (showExamCountdownCard && !examCountdownText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = examCountdownText,
                            style = MaterialTheme.typography.bodySmall,
                            color = AttendanceRed
                        )
                    }
                    if (showWeekOverviewCard && !weekOverviewText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = weekOverviewText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (showSemesterProgressCard) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(modifier = Modifier.clickable { showSemesterDateDialog = true }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Semester", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${(semesterProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedForeground
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { semesterProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Semester progress based on your selected start/end dates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedForeground
                            )
                        }
                    }
                    if (showWeeklyGoalProgress) {
                        val weeklyProgress = (weeklyStudiedMinutes.toFloat() / weeklyGoalMinutes.toFloat()).coerceIn(0f, 1f)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Weekly goal", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${weeklyStudiedMinutes / 60}h ${(weeklyStudiedMinutes % 60)}m / ${weeklyGoalMinutes / 60}h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedForeground
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { weeklyProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "This progress bar shows your weekly goal progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedForeground
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Spacer(modifier = Modifier.height(14.dp))

            Spacer(modifier = Modifier.height(6.dp))
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subjects",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isSubjectEditMode) {
                    TextButton(onClick = onExitSubjectEditMode) { Text("Done") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        val visibleSubjects = if (isSubjectEditMode) subjects else subjects.filter { !it.isHidden }

        if (visibleSubjects.isEmpty()) {
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
                            text = if (isSubjectEditMode) "No subjects found." else "No subjects added yet. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedForeground
                        )
                    }
                }
            }
        } else {
            items(visibleSubjects, key = { "subject_${it.id}" }) { subject ->
                SubjectCard(
                    subject = subject,
                    onMarkPresent = { onMarkPresent(subject.id) },
                    onMarkAbsent = { onMarkAbsent(subject.id) },
                    onMarkOnDuty = { onMarkOnDuty(subject.id) },
                    onHide = {
                        onHide(subject.id)
                    },
                    onUnhide = {
                        onUnhide(subject.id)
                    },
                    onDelete = { onDelete(subject) },
                    onEdit = { onEdit(subject) },
                    onClick = { onSubjectClick(subject) },
                    onLongPress = {
                        if (!isSubjectEditMode) onEnterSubjectEditMode()
                    },
                    isInEditMode = isSubjectEditMode
                )
            }
        }
    }

    if (showSemesterDateDialog) {
        var startDate by remember(semesterStartDate) { mutableStateOf(semesterStartDate.ifBlank { java.time.LocalDate.now().withMonth(1).withDayOfMonth(1).toString() }) }
        var endDate by remember(semesterEndDate) { mutableStateOf(semesterEndDate.ifBlank { java.time.LocalDate.now().withMonth(6).withDayOfMonth(30).toString() }) }
        AlertDialog(
            onDismissRequest = { showSemesterDateDialog = false },
            title = { Text("Semester dates") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetSemesterDates(startDate, endDate)
                    showSemesterDateDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSemesterDateDialog = false }) { Text("Cancel") }
            }
        )
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

@Composable
private fun WeekEventRow(
    event: HomeViewModel.HomeWeekEvent,
    onDelete: () -> Unit
) {
    val typeColor = when (event.type) {
        PlannerEventType.ASSIGNMENT -> AttendanceBlue
        PlannerEventType.EXAM -> AttendanceRed
        PlannerEventType.HOLIDAY -> AttendanceGreen
        PlannerEventType.INTERNAL -> MaterialTheme.colorScheme.tertiary
        else -> MutedForeground
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(typeColor, RoundedCornerShape(10.dp))
            )
            Column {
                Text(event.title, style = MaterialTheme.typography.bodySmall)
                Text("${event.date} - ${event.type}", style = MaterialTheme.typography.bodySmall, color = MutedForeground)
            }
        }
        if (event.plannerEvent != null) {
            TextButton(onClick = onDelete) { Text("Del") }
        }
    }
}

@Composable
private fun AddCustomEventDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, date: String, type: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var type by remember { mutableStateOf(PlannerEventType.INTERNAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        PlannerEventType.ASSIGNMENT,
                        PlannerEventType.EXAM,
                        PlannerEventType.HOLIDAY,
                        PlannerEventType.INTERNAL
                    ).forEach { option ->
                        OutlinedButton(onClick = { type = option }) {
                            Text(if (option == type) "$option *" else option)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, date, type) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
            onHide = {},
            onUnhide = {},
            isSubjectEditMode = false,
            onEnterSubjectEditMode = {},
            onExitSubjectEditMode = {},
            onEdit = {},
            onDelete = {},
            onSubjectClick = {},
            examCountdowns = listOf(
                HomeViewModel.ExamCountdownItem(1, "Math", "2026-04-18", 5),
                HomeViewModel.ExamCountdownItem(2, "Physics", "2026-04-22", 9)
            ),
            semesterProgress = 0.52f,
            weekOverview = listOf(
                HomeViewModel.HomeWeekEvent("p_1", "Internal Viva", "2026-04-15", PlannerEventType.INTERNAL),
                HomeViewModel.HomeWeekEvent("p_2", "Holiday", "2026-04-17", PlannerEventType.HOLIDAY)
            ),
            onAddCustomEvent = {},
            onDeleteCustomEvent = {},
            semesterStartDate = "2026-01-10",
            semesterEndDate = "2026-05-20",
            showSemesterProgressCard = true,
            showExamCountdownCard = true,
            showWeekOverviewCard = true,
            showWeeklyGoalProgress = true,
            tomorrowHolidayText = "Tomorrow holiday: Monday, 2026-04-14 - Tamil New Year",
            weeklyGoalMinutes = 600,
            weeklyStudiedMinutes = 220,
            onSetSemesterDates = { _, _ -> }
        )
    }
}




