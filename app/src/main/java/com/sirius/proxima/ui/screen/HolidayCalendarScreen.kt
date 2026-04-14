package com.sirius.proxima.ui.screen

import android.Manifest
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.runtime.LaunchedEffect
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
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.Muted
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private data class AcademicDay(val date: String, val type: String, val note: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayCalendarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var days by remember { mutableStateOf(loadSavedDays(context)) }

    var showAddDialog by remember { mutableStateOf(false) }
    var dateInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var typeInput by remember { mutableStateOf("Holiday") }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var syncMessage by remember { mutableStateOf("") }
    var autoSyncAttempted by remember { mutableStateOf(false) }

    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCalendarPermission = granted
    }

    fun syncHolidays(isAutoSync: Boolean) {
        val synced = loadPublicHolidays(context)
        val (merged, added) = mergeDays(days, synced)
        days = merged
        saveDays(context, days)
        syncMessage = if (isAutoSync) {
            "Auto sync complete: ${synced.size} found, $added saved"
        } else {
            "Synced ${synced.size} holidays, saved $added new"
        }
    }

    LaunchedEffect(days) {
        saveDays(context, days)
    }

    LaunchedEffect(hasCalendarPermission, days.isEmpty(), autoSyncAttempted) {
        if (!autoSyncAttempted && hasCalendarPermission && days.isEmpty()) {
            autoSyncAttempted = true
            syncHolidays(isAutoSync = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Holiday & Working Days") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        val monthHolidayDays = days
            .filter { it.type.contains("holiday", ignoreCase = true) }
            .mapNotNull { day ->
                val parsed = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@mapNotNull null
                if (YearMonth.from(parsed) == currentMonth) day else null
            }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            HolidayMonthCalendar(
                days = days,
                month = currentMonth,
                onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (!hasCalendarPermission) {
                            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        } else {
                            syncHolidays(isAutoSync = false)
                        }
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

            if (syncMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(syncMessage, color = MutedForeground)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (monthHolidayDays.isEmpty()) {
                Text("No holidays for this month", color = MutedForeground)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(monthHolidayDays.sortedBy { it.date }) { day ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Border),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(day.date, fontWeight = FontWeight.SemiBold)
                                    if (day.note.isNotBlank()) {
                                        Text(day.note, style = MaterialTheme.typography.bodySmall, color = MutedForeground)
                                    }
                                }
                                Text(day.type)
                            }
                        }
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
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = typeInput,
                        onValueChange = { typeInput = it },
                        label = { Text("Type (Holiday/Working Day)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Note") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsedDate = runCatching { LocalDate.parse(dateInput.trim()) }.getOrNull()
                        if (parsedDate != null && typeInput.isNotBlank() && noteInput.isNotBlank()) {
                            val (merged, _) = mergeDays(
                                days,
                                listOf(
                                    AcademicDay(
                                date = parsedDate.toString(),
                                type = typeInput.trim(),
                                note = noteInput.trim()
                                    )
                                )
                            )
                            days = merged
                            dateInput = ""
                            noteInput = ""
                            typeInput = "Holiday"
                            showAddDialog = false
                        }
                    }
                ) {
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
private fun HolidayMonthCalendar(
    days: List<AcademicDay>,
    month: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val dayMap = days.mapNotNull { item ->
        val date = runCatching { LocalDate.parse(item.date) }.getOrNull() ?: return@mapNotNull null
        date to item
    }.toMap()

    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val total = month.lengthOfMonth()
    val cells = mutableListOf<Int?>()
    repeat(offset) { cells.add(null) }
    for (d in 1..total) cells.add(d)
    while (cells.size % 7 != 0) cells.add(null)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                }
                Text(
                    text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                    fontWeight = FontWeight.SemiBold
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
                        val dayInfo = date?.let { dayMap[it] }
                        val isToday = date == LocalDate.now()
                        val isWorkingDay = dayInfo?.type?.contains("working", ignoreCase = true) == true
                        val dotColor = when {
                            dayInfo == null -> null
                            isWorkingDay -> com.sirius.proxima.ui.theme.AttendanceGreen
                            else -> com.sirius.proxima.ui.theme.AttendanceRed
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .let {
                                    if (isToday) it.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    else it
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNumber?.toString() ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (dayInfo == null) Muted else MaterialTheme.colorScheme.onSurface
                            )
                            if (dotColor != null) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 3.dp)
                                        .background(dotColor, androidx.compose.foundation.shape.CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            val monthItems = days.filter {
                it.type.contains("holiday", ignoreCase = true) &&
                    it.date.startsWith(month.format(DateTimeFormatter.ofPattern("yyyy-MM")))
            }
            if (monthItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                monthItems.sortedBy { it.date }.take(3).forEach {
                    Text("${it.date} - ${it.note.ifBlank { it.type }}", style = MaterialTheme.typography.bodySmall, color = MutedForeground)
                }
            }
        }
    }
}

private fun loadPublicHolidays(context: android.content.Context): List<AcademicDay> {
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

    val holidays = mutableListOf<AcademicDay>()
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
            val localDate = Instant.ofEpochMilli(dtStart).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            holidays += AcademicDay(date = localDate, type = "Holiday", note = title)
        }
    }

    return holidays
}

private fun mergeDays(existing: List<AcademicDay>, incoming: List<AcademicDay>): Pair<List<AcademicDay>, Int> {
    val keys = existing.map { dayKey(it) }.toMutableSet()
    var merged = existing
    var added = 0
    incoming.forEach { day ->
        val key = dayKey(day)
        if (key !in keys) {
            merged = merged + day
            keys += key
            added++
        }
    }
    return merged to added
}

private fun dayKey(day: AcademicDay): String {
    return "${day.date.trim()}|${day.type.trim().lowercase()}|${day.note.trim().lowercase()}"
}

private fun saveDays(context: android.content.Context, days: List<AcademicDay>) {
    val array = JSONArray()
    days.forEach { day ->
        array.put(
            JSONObject()
                .put("date", day.date)
                .put("type", day.type)
                .put("note", day.note)
        )
    }
    context.getSharedPreferences(HOLIDAY_PREFS, android.content.Context.MODE_PRIVATE)
        .edit()
        .putString(HOLIDAY_DATA_KEY, array.toString())
        .apply()
}

private fun loadSavedDays(context: android.content.Context): List<AcademicDay> {
    val raw = context.getSharedPreferences(HOLIDAY_PREFS, android.content.Context.MODE_PRIVATE)
        .getString(HOLIDAY_DATA_KEY, null)
        ?: return emptyList()
    val result = mutableListOf<AcademicDay>()
    runCatching {
        val array = JSONArray(raw)
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val date = item.optString("date")
            val type = item.optString("type")
            val note = item.optString("note")
            if (date.isNotBlank() && type.isNotBlank()) {
                result += AcademicDay(date = date, type = type, note = note)
            }
        }
    }
    return result
}

private const val HOLIDAY_PREFS = "holiday_calendar_storage"
private const val HOLIDAY_DATA_KEY = "holiday_days_json"

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun HolidayCalendarScreenPreview() {
    ProximaTheme {
        HolidayCalendarScreen(onBack = {})
    }
}

