package com.sirius.proxima.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.sirius.proxima.ui.components.AddTimetableEntryDialog
import com.sirius.proxima.ui.components.TimetableSlot
import com.sirius.proxima.ui.theme.*
import com.sirius.proxima.viewmodel.TimetableViewModel

private val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel = viewModel(
        factory = TimetableViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val expandedDay by viewModel.expandedDay.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingDayHour by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var editingEntry by remember { mutableStateOf<TimetableEntry?>(null) }

    TimetableScreenContent(
        subjects = subjects,
        allEntries = allEntries,
        expandedDay = expandedDay,
        onToggleDay = { viewModel.toggleDay(it) },
        onAddSlot = { day, hour ->
            editingDayHour = Pair(day, hour)
            editingEntry = null
            showAddDialog = true
        },
        onEditSlot = { day, hour, entry ->
            editingDayHour = Pair(day, hour)
            editingEntry = entry
            showAddDialog = true
        },
        onDeleteEntry = { viewModel.deleteEntry(it) }
    )

    if (showAddDialog && editingDayHour != null) {
        val (day, hour) = editingDayHour!!
        AddTimetableEntryDialog(
            subjects = subjects,
            existingSubjectName = editingEntry?.subjectName,
            existingClassNumber = editingEntry?.classNumber,
            onSave = { subject, classNumber ->
                viewModel.addOrUpdateEntry(day, hour, subject, classNumber)
                showAddDialog = false
                editingDayHour = null
                editingEntry = null
            },
            onDismiss = {
                showAddDialog = false
                editingDayHour = null
                editingEntry = null
            }
        )
    }
}

@Composable
fun TimetableScreenContent(
    subjects: List<Subject> = emptyList(),
    allEntries: List<TimetableEntry> = emptyList(),
    expandedDay: Int = -1,
    onToggleDay: (Int) -> Unit = {},
    onAddSlot: (Int, Int) -> Unit = { _, _ -> },
    onEditSlot: (Int, Int, TimetableEntry) -> Unit = { _, _, _ -> },
    onDeleteEntry: (TimetableEntry) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Timetable",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage your weekly class schedule",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedForeground
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(7) { index ->
            val dayNumber = index + 1
            val dayName = dayNames[index]
            val isExpanded = expandedDay == dayNumber
            val dayEntries = allEntries.filter { it.dayOfWeek == dayNumber }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isExpanded) Border else Border.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column {
                    // Day header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleDay(dayNumber) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${dayEntries.size} class${if (dayEntries.size != 1) "es" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedForeground
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MutedForeground
                        )
                    }

                    // Expanded content - 24 hour slots
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            HorizontalDivider(color = Border, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            (0..23).forEach { hour ->
                                val entry = dayEntries.find { it.hourSlot == hour }
                                TimetableSlot(
                                    hourSlot = hour,
                                    entry = entry,
                                    onAdd = { onAddSlot(dayNumber, hour) },
                                    onEdit = {
                                        if (entry != null) onEditSlot(dayNumber, hour, entry)
                                    },
                                    onDelete = {
                                        if (entry != null) onDeleteEntry(entry)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
fun TimetableScreenContentPreview() {
    ProximaTheme {
        TimetableScreenContent(
            subjects = listOf(
                Subject(1, "Mathematics", 40, 35),
                Subject(2, "Physics", 30, 20)
            ),
            allEntries = listOf(
                TimetableEntry(1, 1, 9, 1, "Mathematics", "Room 301"),
                TimetableEntry(2, 1, 11, 2, "Physics", "Lab 2"),
            ),
            expandedDay = 1
        )
    }
}


