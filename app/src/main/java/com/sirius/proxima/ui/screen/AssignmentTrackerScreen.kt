package com.sirius.proxima.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.Muted
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme
import com.sirius.proxima.viewmodel.AcademicToolsViewModel
import com.sirius.proxima.viewmodel.millisToLocalDateText
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private data class SyncedHoliday(val date: String, val title: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentTrackerScreen(
    onBack: () -> Unit,
    viewModel: AcademicToolsViewModel = viewModel(
        factory = AcademicToolsViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val context = LocalContext.current
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var syncMessage by remember { mutableStateOf("") }
    var month by remember { mutableStateOf(YearMonth.now()) }

    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCalendarPermission = granted
        syncMessage = if (granted) "Calendar permission granted. Tap Sync Holidays again."
        else "Calendar permission is needed to sync holidays."
    }

    val markedDates = assignments.mapNotNull {
        runCatching { LocalDate.parse(millisToLocalDateText(it.dueAtMillis)) }.getOrNull()
    }.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assignment Tracker") },
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
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Border),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { month = month.minusMonths(1) }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = { month = month.plusMonths(1) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                            }
                        }
                        CompactMonthCalendar(month = month, markedDates = markedDates)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (!hasCalendarPermission) {
                                permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                return@Button
                            }

                            val synced = loadGoogleHolidays(context)
                            if (synced.isEmpty()) {
                                syncMessage = "No Google holidays found to sync."
                                return@Button
                            }

                            val existing = assignments
                                .map { "${millisToLocalDateText(it.dueAtMillis)}|${it.title}" }
                                .toMutableSet()

                            var added = 0
                            synced.forEach { holiday ->
                                val normalizedTitle = holidayAssignmentTitle(holiday.title)
                                val key = "${holiday.date}|$normalizedTitle"
                                if (key !in existing) {
                                    viewModel.addAssignment(normalizedTitle, holiday.date)
                                    existing += key
                                    added++
                                }
                            }
                            syncMessage = "Synced ${synced.size} holidays, added $added new."
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sync Holidays")
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Day")
                    }
                }
            }
            item {
                if (syncMessage.isNotBlank()) {
                    Text(syncMessage, color = MutedForeground)
                }
            }

            if (assignments.isEmpty()) {
                item {
                    Text("No assignments yet", color = MutedForeground)
                }
            }

            items(assignments, key = { it.id }) { item ->
                val dueText = millisToLocalDateText(item.dueAtMillis)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$dueText (${daysLeft(dueText)})")
                    }
                    TextButton(onClick = { viewModel.deleteAssignment(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Day") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Assignment title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() && dueDate.isNotBlank()) {
                        viewModel.addAssignment(title.trim(), dueDate.trim())
                        title = ""
                        dueDate = ""
                        showAddDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CompactMonthCalendar(month: YearMonth, markedDates: Set<LocalDate>) {
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val totalDays = month.lengthOfMonth()
    val cells = mutableListOf<Int?>()
    repeat(offset) { cells += null }
    for (d in 1..totalDays) cells += d
    while (cells.size % 7 != 0) cells += null

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(day, modifier = Modifier.weight(1f), color = MutedForeground)
            }
        }

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { dayNumber ->
                    val date = dayNumber?.let { month.atDay(it) }
                    val isToday = date == LocalDate.now()
                    val hasEvent = date in markedDates
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .let {
                                if (isToday) it.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                else it
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayNumber?.toString() ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (dayNumber == null) Muted else MaterialTheme.colorScheme.onSurface
                        )
                        if (hasEvent) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 3.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun AssignmentTrackerScreenPreview() {
    ProximaTheme {
        AssignmentTrackerScreen(onBack = {})
    }
}

private fun daysLeft(date: String): String {
    val due = runCatching { LocalDate.parse(date) }.getOrNull() ?: return "Invalid date"
    val diff = Duration.between(LocalDate.now().atStartOfDay(), due.atStartOfDay()).toDays()
    return when {
        diff < 0 -> "Overdue"
        diff == 0L -> "Due today"
        else -> "$diff day(s) left"
    }
}

private fun holidayAssignmentTitle(title: String): String {
    return "[Holiday] ${title.trim().ifBlank { "Public Holiday" }}"
}

private fun loadGoogleHolidays(context: Context): List<SyncedHoliday> {
    val resolver = context.contentResolver
    val calendarIds = mutableListOf<Long>()

    resolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.ACCOUNT_TYPE
        ),
        "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
        arrayOf("com.google"),
        null
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val name = cursor.getString(1).orEmpty().lowercase()
            val owner = cursor.getString(2).orEmpty().lowercase()
            if ("holiday" in name || "holiday" in owner) {
                calendarIds += id
            }
        }
    }

    if (calendarIds.isEmpty()) return emptyList()

    val start = LocalDate.now().minusDays(30)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val end = LocalDate.now().plusDays(365)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val placeholders = calendarIds.joinToString(",") { "?" }
    val selection = "${CalendarContract.Events.CALENDAR_ID} IN ($placeholders) AND ${CalendarContract.Events.ALL_DAY} = 1 AND ${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
    val args = calendarIds.map { it.toString() } + listOf(start.toString(), end.toString())

    val holidays = mutableListOf<SyncedHoliday>()
    resolver.query(
        CalendarContract.Events.CONTENT_URI,
        arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART),
        selection,
        args.toTypedArray(),
        "${CalendarContract.Events.DTSTART} ASC"
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            val title = cursor.getString(0).orEmpty().ifBlank { "Public Holiday" }
            val dtStart = cursor.getLong(1)
            val date = Instant.ofEpochMilli(dtStart).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            holidays += SyncedHoliday(date = date, title = title)
        }
    }

    return holidays
}

