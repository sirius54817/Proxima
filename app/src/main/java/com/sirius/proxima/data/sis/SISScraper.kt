package com.sirius.proxima.data.sis

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.CookieManager
import java.net.CookiePolicy
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.concurrent.TimeUnit

data class SisAttendance(
    val courseCode: String,
    val courseName: String,
    val credits: String,
    val total: Int,
    val present: Int,
    val absent: Int,
    val onDuty: Int,
    val medicalLeave: Int,
    val percentage: Double,
    val detailsUrl: String
)

data class SisAttendanceHistory(
    val date: String,
    val status: String,
    val slotName: String? = null
)

sealed class SisResult<out T> {
    data class Success<T>(val data: T) : SisResult<T>()
    data class Error(val message: String) : SisResult<Nothing>()
}

const val SIS_NETWORK_UNAVAILABLE = "SIS_NETWORK_UNAVAILABLE"

class SISScraper(private val context: Context) {

    private val TAG = "SISScraper"

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val client: OkHttpClient
        get() = buildClient()

    private fun buildClient(): OkHttpClient {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork

        return OkHttpClient.Builder()
            .cookieJar(okhttp3.JavaNetCookieJar(cookieManager))
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .apply {
                network?.let { socketFactory(it.socketFactory) }
            }
            .build()
    }

    private val gson = Gson()

    private fun getCsrfToken(): String {
        var lastError: Exception? = null
        repeat(3) { attempt ->
            try {
                val request = Request.Builder()
                    .url("https://student.kalasalingam.ac.in/login")
                    .get()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .build()
                val response = client.newCall(request).execute()
                val html = response.body?.string() ?: ""
                val match = Regex("""name="csrf-token"\s+content="(.+?)"""").find(html)
                    ?: Regex("""name="_token".*?value="(.+?)"""").find(html)
                val token = match?.groupValues?.get(1) ?: ""
                Log.d(TAG, "CSRF token attempt ${attempt + 1}: ${token.take(10)}...")
                if (token.isNotEmpty()) return token
            } catch (e: Exception) {
                lastError = e
                Thread.sleep(500L * (attempt + 1))
            }
        }
        throw lastError ?: Exception("Failed to get CSRF token")
    }

    private fun resolveSisUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        val normalized = if (url.startsWith("/")) url else "/$url"
        return "https://student.kalasalingam.ac.in$normalized"
    }

    private fun stripHtml(text: String): String {
        return text
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeStatus(raw: String): String? {
        val value = raw.trim().lowercase(Locale.getDefault())
        return when {
            value == "p" || value == "pr" -> "Present"
            value == "a" || value == "ab" -> "Absent"
            value == "od" || value == "o" -> "On Duty"
            value == "ml" -> "On Duty"
            value.contains("present") -> "Present"
            value.contains("absent") -> "Absent"
            value.contains("on duty") || Regex("\\bod\\b").containsMatchIn(value) -> "On Duty"
            value.contains("medical") -> "On Duty"
            else -> null
        }
    }

    private fun normalizeDate(raw: String): String? {
        val value = raw.trim()
        if (value.isBlank()) return null

        val candidates = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-uuuu"),
            DateTimeFormatter.ofPattern("dd/MM/uuuu"),
            DateTimeFormatter.ofPattern("dd.MM.uuuu"),
            DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMMM uuuu", Locale.ENGLISH)
        )

        candidates.forEach { formatter ->
            try {
                return LocalDate.parse(value, formatter).toString()
            } catch (_: DateTimeParseException) {
            }
        }

        return null
    }

    private fun extractJsonHistoryRecords(root: JsonElement): List<SisAttendanceHistory> {
        val rows = mutableListOf<JsonObject>()
        when {
            root.isJsonArray -> {
                root.asJsonArray.forEach { if (it.isJsonObject) rows.add(it.asJsonObject) }
            }
            root.isJsonObject -> {
                val obj = root.asJsonObject
                val data = obj.get("data")
                if (data != null && data.isJsonArray) {
                    data.asJsonArray.forEach { if (it.isJsonObject) rows.add(it.asJsonObject) }
                }
            }
        }

        return rows.mapNotNull { row ->
            val dateRaw = listOf("date", "attendance_date", "class_date", "attendanceDate")
                .firstNotNullOfOrNull { key -> row.get(key)?.asString?.trim()?.takeIf { it.isNotBlank() } }
                ?: listOf("entry_date", "entryDate")
                    .firstNotNullOfOrNull { key -> row.get(key)?.asString?.trim()?.takeIf { it.isNotBlank() } }
            val statusRaw = listOf("status", "attendance_status", "state", "attendance")
                .firstNotNullOfOrNull { key -> row.get(key)?.asString?.trim()?.takeIf { it.isNotBlank() } }
            val slotRaw = listOf("slot_name", "slotName", "slot", "time_slot")
                .firstNotNullOfOrNull { key -> row.get(key)?.asString?.trim()?.takeIf { it.isNotBlank() } }

            val date = dateRaw?.let(::normalizeDate)
            val status = statusRaw?.let(::normalizeStatus)
            if (date != null && status != null) SisAttendanceHistory(date = date, status = status, slotName = slotRaw) else null
        }
    }

    private fun extractHtmlHistoryRecords(html: String): List<SisAttendanceHistory> {
        val rowRegex = Regex("(?is)<tr[^>]*>(.*?)</tr>")
        val cellRegex = Regex("(?is)<t[dh][^>]*>(.*?)</t[dh]>")
        val dateRegex = Regex("\\b(\\d{4}-\\d{2}-\\d{2}|\\d{2}[-/.]\\d{2}[-/.]\\d{4})\\b")

        return rowRegex.findAll(html).mapNotNull { rowMatch ->
            val rowHtml = rowMatch.groupValues.getOrNull(1).orEmpty()
            val cells = cellRegex.findAll(rowHtml)
                .map { stripHtml(it.groupValues.getOrNull(1).orEmpty()) }
                .filter { it.isNotBlank() }
                .toList()

            if (cells.isEmpty()) return@mapNotNull null

            val dateCandidate = cells.firstNotNullOfOrNull { cell ->
                val hit = dateRegex.find(cell)?.value ?: return@firstNotNullOfOrNull null
                normalizeDate(hit)
            }

            val statusCandidate = cells.firstNotNullOfOrNull { cell ->
                normalizeStatus(cell)
            }
            val slotCandidate = cells.firstOrNull { cell ->
                cell.contains(":") && (cell.contains("AM", true) || cell.contains("PM", true))
            }

            if (dateCandidate != null && statusCandidate != null) {
                SisAttendanceHistory(date = dateCandidate, status = statusCandidate, slotName = slotCandidate)
            } else {
                null
            }
        }.toList()
    }

    fun getSubjectAttendanceHistory(detailsUrl: String): SisResult<List<SisAttendanceHistory>> {
        return try {
            if (detailsUrl.isBlank()) return SisResult.Success(emptyList())

            val resolvedUrl = resolveSisUrl(detailsUrl)
            val request = Request.Builder()
                .url(resolvedUrl)
                .get()
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Referer", "https://student.kalasalingam.ac.in/semester/attendance")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.code == 302 || body.contains("/login")) {
                return SisResult.Error("Session expired. Please login again.")
            }

            var bodyToParse = body
            val initialObj = runCatching { gson.fromJson(body, JsonObject::class.java) }.getOrNull()
            val initialDataSize = initialObj?.getAsJsonArray("data")?.size() ?: 0
            val totalRecords = initialObj?.get("recordsTotal")?.asInt ?: initialDataSize
            if (totalRecords > initialDataSize && totalRecords > 0) {
                val expandedUrl = response.request.url.newBuilder()
                    .setQueryParameter("start", "0")
                    .setQueryParameter("length", totalRecords.toString())
                    .build()

                val expandedRequest = Request.Builder()
                    .url(expandedUrl)
                    .get()
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .header("Referer", "https://student.kalasalingam.ac.in/semester/attendance")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .build()

                val expandedResponse = client.newCall(expandedRequest).execute()
                val expandedBody = expandedResponse.body?.string() ?: ""
                if (expandedResponse.isSuccessful && expandedBody.isNotBlank() && !expandedBody.contains("/login")) {
                    bodyToParse = expandedBody
                }
            }

            val jsonRecords = try {
                val root = gson.fromJson(bodyToParse, JsonElement::class.java)
                extractJsonHistoryRecords(root)
            } catch (_: Exception) {
                emptyList()
            }

            val records = if (jsonRecords.isNotEmpty()) jsonRecords else extractHtmlHistoryRecords(bodyToParse)
            SisResult.Success(records.distinctBy { "${it.date}|${it.status}" })
        } catch (e: Exception) {
            Log.e(TAG, "History fetch error for URL: $detailsUrl", e)
            SisResult.Error("Failed to fetch subject history: ${e.message}")
        }
    }

    fun login(registerNo: String, password: String): SisResult<Unit> {
        return try {
            // Clear existing cookies for fresh session
            cookieManager.cookieStore.removeAll()

            val token = getCsrfToken()
            if (token.isEmpty()) {
                return SisResult.Error("Could not get login token. Check your internet connection.")
            }

            val formBody = FormBody.Builder()
                .add("_token", token)
                .add("register_no", registerNo)
                .add("password", password)
                .build()

            val request = Request.Builder()
                .url("https://student.kalasalingam.ac.in/login")
                .post(formBody)
                .header("Referer", "https://student.kalasalingam.ac.in/login")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val finalUrl = response.request.url.toString()
            Log.d(TAG, "Login redirect URL: $finalUrl")

            if (finalUrl.contains("login")) {
                SisResult.Error("Invalid register number or password.")
            } else {
                SisResult.Success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            SisResult.Error("Login failed: ${e.message}")
        }
    }

    fun getAttendance(): SisResult<List<SisAttendance>> {
        return try {
            val request = Request.Builder()
                .url("https://student.kalasalingam.ac.in/attendance-details?draw=1&start=0&length=200&search[value]=&search[regex]=false")
                .get()
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Referer", "https://student.kalasalingam.ac.in/semester/attendance")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            Log.d(TAG, "Attendance response code: ${response.code}")

            if (response.code == 302 || body.contains("<!DOCTYPE html>")) {
                return SisResult.Error("Session expired. Please login again.")
            }

            val json = gson.fromJson(body, JsonObject::class.java)
            val data: JsonArray = json.getAsJsonArray("data") ?: return SisResult.Error("No attendance data found.")

            val subjects = mutableListOf<SisAttendance>()
            for (element in data) {
                val item = element.asJsonObject
                subjects.add(
                    SisAttendance(
                        courseCode = item.get("course_code")?.asString ?: "",
                        courseName = item.get("course_name")?.asString ?: "",
                        credits = item.get("credits")?.asString ?: "0",
                        total = item.get("total")?.asString?.toIntOrNull() ?: 0,
                        present = item.get("present")?.asString?.toIntOrNull() ?: 0,
                        absent = item.get("absent")?.asString?.toIntOrNull() ?: 0,
                        onDuty = item.get("on_duty")?.asString?.toIntOrNull() ?: 0,
                        medicalLeave = item.get("medical_leave")?.asString?.toIntOrNull() ?: 0,
                        percentage = item.get("percentage")?.asString?.toDoubleOrNull() ?: 0.0,
                        detailsUrl = item.get("details_url")?.asString ?: ""
                    )
                )
            }
            Log.d(TAG, "Fetched ${subjects.size} subjects")
            SisResult.Success(subjects)
        } catch (e: Exception) {
            Log.e(TAG, "Attendance fetch error", e)
            SisResult.Error("Failed to fetch attendance: ${e.message}")
        }
    }
}
