package com.sally.zlauncher.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Log sederhana di memori untuk proses peluncuran. */
object LaunchLog {
    private val lines = ArrayList<String>()
    private val format = SimpleDateFormat("HH:mm:ss", Locale.US)

    @Synchronized
    fun add(message: String) {
        lines.add("${format.format(Date())}  $message")
        if (lines.size > 500) lines.removeAt(0)
    }

    @Synchronized
    fun clear() {
        lines.clear()
    }

    @Synchronized
    fun dump(): String = if (lines.isEmpty()) "(kosong)" else lines.joinToString("\n")
}
