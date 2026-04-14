package com.sirius.proxima.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.data.model.SubjectAttendanceRecord
import com.sirius.proxima.ui.theme.AttendanceBlue
import com.sirius.proxima.ui.theme.AttendanceGreen
import com.sirius.proxima.ui.theme.AttendanceRed
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.Muted
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.data.model.AttendanceStatus
import com.sirius.proxima.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class HistoryViewMode { CALENDAR, LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectHistoryScreen(
    subjectId: Int,
    onBack: () -> Unit,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val history by viewModel.getAttendanceHistory(subjectId).collectAsStateWithLifecycle(initialValue = emptyList())
    val loadingSubjectId by viewModel.historyPortalLoadingSubjectId.collectAsStateWithLifecycle()
    val portalError by viewModel.historyPortalError.collectAsStateWithLifecycle()
    val sisUnlocked by viewModel.sisFeaturesUnlocked.collectAsStateWithLifecycle()

    val subject = subjects.find { it.id == subjectId }
    var visibleCount by remember(subjectId) { mutableStateOf(15) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(HistoryViewMode.CALENDAR) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    LaunchedEffect(history.size) {
        if (history.size < visibleCount) {
            visibleCount = history.size.coerceAtLeast(15)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = subject?.name ?: "Subject History",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewMode = if (viewMode == HistoryViewMode.CALENDAR) HistoryViewMode.LIST else HistoryViewMode.CALENDAR
                    }) {
                        Icon(
                            imageVector = if (viewMode == HistoryViewMode.CALENDAR) Icons.AutoMirrored.Filled.ViewList else Icons.Default.CalendarMonth,
                            contentDescription = if (viewMode == HistoryViewMode.CALENDAR) "Show list" else "Show calendar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            val errorMessage = portalError
            if (sisUnlocked && errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AttendanceRed.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = AttendanceRed
                        )
                        TextButton(onClick = { viewModel.clearHistoryPortalError() }) {
                            Text("Dismiss")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (viewMode == HistoryViewMode.LIST) {
                    OutlinedButton(
                        onClick = { visibleCount += 15 },
                        enabled = visibleCount < history.size,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Load Next 15")
                    }
                }

                if (sisUnlocked) {
                    Button(
                        onClick = { if (subject != null) viewModel.loadMoreHistoryFromPortal(subject) },
                        enabled = loadingSubjectId != subjectId && subject != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (loadingSubjectId == subjectId) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(if (loadingSubjectId == subjectId) "Loading..." else "Load from Portal")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (history.isEmpty()) "No data available" else "Showing ${history.take(visibleCount).size} of ${history.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedForeground
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (history.isEmpty()) {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedForeground
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showManualAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add your own entry")
                }
            } else if (viewMode == HistoryViewMode.CALENDAR) {
                AttendanceHistoryCalendar(
                    history = history,
                    month = currentMonth,
                    onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history.take(visibleCount), key = { it.id }) { record ->
                        val slotText = record.slotName?.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""
                        val timestampText = Instant.ofEpochMilli(record.recordedAtMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a"))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${record.date} - ${record.status}$slotText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Recorded: $timestampText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedForeground
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualAddDialog) {
        var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
        var slotText by remember { mutableStateOf("") }
        var selectedStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }

        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("Add attendance entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = slotText,
                        onValueChange = { slotText = it },
                        label = { Text("Slot (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT, AttendanceStatus.ON_DUTY).forEach { status ->
                            OutlinedButton(onClick = { selectedStatus = status }) {
                                Text(if (selectedStatus == status) "$status *" else status)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (subject != null && dateText.isNotBlank()) {
                            viewModel.addManualAttendanceRecord(
                                subjectId = subject.id,
                                status = selectedStatus,
                                date = dateText.trim(),
                                slotName = slotText.trim().ifBlank { null }
                            )
                            showManualAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AttendanceHistoryCalendar(
    history: List<SubjectAttendanceRecord>,
    month: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val statusByDate = history
        .mapNotNull { record ->
            val date = runCatching { LocalDate.parse(record.date) }.getOrNull() ?: return@mapNotNull null
            date to record.status
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, statuses) ->
            when {
                statuses.contains(AttendanceStatus.ABSENT) -> AttendanceStatus.ABSENT
                statuses.contains(AttendanceStatus.ON_DUTY) -> AttendanceStatus.ON_DUTY
                else -> AttendanceStatus.PRESENT
            }
        }

    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val total = month.lengthOfMonth()
    val cells = mutableListOf<Int?>()
    repeat(offset) { cells.add(null) }
    for (d in 1..total) cells.add(d)
    while (cells.size % 7 != 0) cells.add(null)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                }
                Text(
                    text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                    Text(label, modifier = Modifier.weight(1f), color = MutedForeground)
                }
            }

            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { dayNumber ->
                        val date = dayNumber?.let { month.atDay(it) }
                        val isToday = date == LocalDate.now()
                        val status = date?.let { statusByDate[it] }
                        val dotColor = when (status) {
                            AttendanceStatus.PRESENT -> AttendanceGreen
                            AttendanceStatus.ON_DUTY -> AttendanceBlue
                            AttendanceStatus.ABSENT -> AttendanceRed
                            else -> null
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .let {
                                    if (isToday) it.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                    else it
                                },
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = dayNumber?.toString() ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (status == null) Muted else MaterialTheme.colorScheme.onSurface
                            )
                            if (dotColor != null) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .align(androidx.compose.ui.Alignment.BottomCenter)
                                        .padding(bottom = 3.dp)
                                        .background(dotColor, androidx.compose.foundation.shape.CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendDot("Present", AttendanceGreen)
                LegendDot("On Duty", AttendanceBlue)
                LegendDot("Absent", AttendanceRed)
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedForeground)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun SubjectHistoryScreenPreview() {
    ProximaTheme {
        SubjectHistoryScreen(subjectId = 1, onBack = {})
    }
}


