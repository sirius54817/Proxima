package com.sirius.proxima.data.sis

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.CookieManager
import java.net.CookiePolicy
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

sealed class SisResult<out T> {
    data class Success<T>(val data: T) : SisResult<T>()
    data class Error(val message: String) : SisResult<Nothing>()
}

class SISScraper {

    private val TAG = "SISScraper"

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(okhttp3.JavaNetCookieJar(cookieManager))
        .followRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private fun getCsrfToken(): String {
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
        Log.d(TAG, "CSRF token: ${token.take(10)}...")
        return token
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

