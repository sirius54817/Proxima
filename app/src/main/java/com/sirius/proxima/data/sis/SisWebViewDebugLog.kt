package com.sirius.proxima.data.sis

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SisWebViewDebugLog {
    private const val MAX_LINES = 500
    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val lines = ArrayDeque<String>()

    @Synchronized
    fun add(message: String) {
        val entry = "${formatter.format(Date())}  $message"
        if (lines.size >= MAX_LINES) {
            lines.removeFirst()
        }
        lines.addLast(entry)
    }

    @Synchronized
    fun snapshot(): String {
        return if (lines.isEmpty()) {
            "No SIS WebView debug logs yet."
        } else {
            lines.joinToString(separator = "\n")
        }
    }

    @Synchronized
    fun clear() {
        lines.clear()
    }
}

