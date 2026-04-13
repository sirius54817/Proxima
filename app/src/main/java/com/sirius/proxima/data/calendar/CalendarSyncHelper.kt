package com.sirius.proxima.data.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.sirius.proxima.data.model.TimetableEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private const val PROXIMA_MARKER = "[ProximaTimetable]"
private const val SLOT_KEY_PREFIX = "PROXIMA_SLOT="
private const val CLASS_KEY_PREFIX = "PROXIMA_CLASS="

data class CalendarSyncStats(
    val calendarId: Long,
    val calendarName: String,
    val upserted: Int = 0,
    val deleted: Int = 0,
    val imported: Int = 0
)

data class CalendarEventSnapshot(
    val dayOfWeek: Int,
    val hourSlot: Int,
    val subjectName: String,
    val classNumber: String
)

object CalendarSyncHelper {

    suspend fun findWritableGoogleCalendar(context: Context): Pair<Long, String>? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val selection = "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val args = arrayOf("com.google", CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                val name = cursor.getString(1) ?: "Google Calendar"
                return@withContext id to name
            }
        }
        null
    }

    suspend fun syncToCalendar(
        context: Context,
        calendarId: Long,
        calendarName: String,
        entries: List<TimetableEntry>
    ): CalendarSyncStats = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val existingBySlot = mutableMapOf<String, Long>()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.DESCRIPTION
        )
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DESCRIPTION} LIKE ?"
        val args = arrayOf(calendarId.toString(), "%$PROXIMA_MARKER%")

        resolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val eventId = cursor.getLong(0)
                val description = cursor.getString(1).orEmpty()
                val slotKey = parseMeta(description, SLOT_KEY_PREFIX)
                if (!slotKey.isNullOrBlank()) {
                    existingBySlot[slotKey] = eventId
                }
            }
        }

        val activeSlotKeys = mutableSetOf<String>()
        var upserted = 0

        entries.forEach { entry ->
            if (entry.subjectName.isBlank() || entry.subjectName == "[Deleted Subject]") return@forEach

            val slotKey = "${entry.dayOfWeek}-${entry.hourSlot}"
            activeSlotKeys += slotKey

            val startMillis = nextOccurrenceMillis(entry.dayOfWeek, entry.hourSlot) ?: return@forEach
            val endMillis = startMillis + (50 * 60 * 1000)

            val description = buildDescription(entry)
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, entry.subjectName)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.EVENT_TIMEZONE, ZoneId.systemDefault().id)
                put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;BYDAY=${byDay(entry.dayOfWeek)}")
            }

            val existingId = existingBySlot[slotKey]
            if (existingId != null) {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingId)
                resolver.update(uri, values, null, null)
            } else {
                resolver.insert(CalendarContract.Events.CONTENT_URI, values)
            }
            upserted += 1
        }

        var deleted = 0
        existingBySlot.forEach { (slotKey, eventId) ->
            if (slotKey !in activeSlotKeys) {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
                deleted += resolver.delete(uri, null, null)
            }
        }

        CalendarSyncStats(
            calendarId = calendarId,
            calendarName = calendarName,
            upserted = upserted,
            deleted = deleted
        )
    }

    suspend fun loadAppEventsFromCalendar(
        context: Context,
        calendarId: Long
    ): List<CalendarEventSnapshot> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART
        )
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DESCRIPTION} LIKE ?"
        val args = arrayOf(calendarId.toString(), "%$PROXIMA_MARKER%")

        val snapshots = mutableListOf<CalendarEventSnapshot>()
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val title = cursor.getString(0).orEmpty()
                val description = cursor.getString(1).orEmpty()
                val dtStart = cursor.getLong(2)

                val slotKey = parseMeta(description, SLOT_KEY_PREFIX)
                val classNumber = parseMeta(description, CLASS_KEY_PREFIX).orEmpty()

                val dayAndHour = parseSlot(slotKey) ?: run {
                    val zoned = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(dtStart), ZoneId.systemDefault())
                    val day = dayOfWeekToInt(zoned.dayOfWeek)
                    day to zoned.hour
                }

                snapshots += CalendarEventSnapshot(
                    dayOfWeek = dayAndHour.first,
                    hourSlot = dayAndHour.second,
                    subjectName = title.ifBlank { "Untitled" },
                    classNumber = classNumber
                )
            }
        }
        snapshots
    }

    suspend fun clearAppEvents(context: Context, calendarId: Long): Int = withContext(Dispatchers.IO) {
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DESCRIPTION} LIKE ?"
        val args = arrayOf(calendarId.toString(), "%$PROXIMA_MARKER%")
        context.contentResolver.delete(CalendarContract.Events.CONTENT_URI, selection, args)
    }

    private fun buildDescription(entry: TimetableEntry): String {
        val classValue = entry.classNumber.ifBlank { "" }
        return """
            $PROXIMA_MARKER
            ${SLOT_KEY_PREFIX}${entry.dayOfWeek}-${entry.hourSlot}
            ${CLASS_KEY_PREFIX}$classValue
        """.trimIndent()
    }

    private fun parseMeta(description: String, key: String): String? {
        return description
            .lineSequence()
            .firstOrNull { it.startsWith(key) }
            ?.substringAfter(key)
            ?.trim()
    }

    private fun parseSlot(slotKey: String?): Pair<Int, Int>? {
        if (slotKey.isNullOrBlank()) return null
        val parts = slotKey.split("-")
        if (parts.size != 2) return null
        val day = parts[0].toIntOrNull() ?: return null
        val hour = parts[1].toIntOrNull() ?: return null
        if (day !in 1..7 || hour !in 0..23) return null
        return day to hour
    }

    private fun byDay(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "MO"
        2 -> "TU"
        3 -> "WE"
        4 -> "TH"
        5 -> "FR"
        6 -> "SA"
        else -> "SU"
    }

    private fun nextOccurrenceMillis(dayOfWeek: Int, hourSlot: Int): Long? {
        if (dayOfWeek !in 1..7 || hourSlot !in 0..23) return null

        val today = LocalDate.now()
        val targetDay = intToDayOfWeek(dayOfWeek)
        var date = today
        while (date.dayOfWeek != targetDay) {
            date = date.plusDays(1)
        }

        val zoned = ZonedDateTime.of(date.year, date.monthValue, date.dayOfMonth, hourSlot, 0, 0, 0, ZoneId.systemDefault())
        return zoned.toInstant().toEpochMilli()
    }

    private fun intToDayOfWeek(dayOfWeek: Int): DayOfWeek = when (dayOfWeek) {
        1 -> DayOfWeek.MONDAY
        2 -> DayOfWeek.TUESDAY
        3 -> DayOfWeek.WEDNESDAY
        4 -> DayOfWeek.THURSDAY
        5 -> DayOfWeek.FRIDAY
        6 -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }

    private fun dayOfWeekToInt(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }
}

