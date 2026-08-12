package com.wanlian.printer.storage

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small persistent log intended for field diagnosis without requiring Android Logcat. */
class DiagnosticLogRepository(context: Context) {
    private val logFile = File(context.applicationContext.filesDir, LOG_FILE_NAME)

    @Synchronized
    fun append(category: String, message: String, error: Throwable? = null) {
        runCatching {
            logFile.parentFile?.mkdirs()
            val timestamp = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.CHINA).format(Date())
            val normalizedMessage = message.replace("\r", " ").replace("\n", " | ")
            val errorSuffix = error?.let { " | exception=${describeThrowable(it)}" }.orEmpty()
            logFile.appendText("$timestamp | $category | $normalizedMessage$errorSuffix\n", Charsets.UTF_8)
            trimIfNeeded()
        }
    }

    @Synchronized
    fun read(): String = runCatching {
        if (logFile.exists()) logFile.readText(Charsets.UTF_8) else ""
    }.getOrDefault("")

    @Synchronized
    fun clear() {
        runCatching {
            if (logFile.exists()) logFile.writeText("", Charsets.UTF_8)
        }
    }

    private fun trimIfNeeded() {
        if (logFile.length() <= MAX_LOG_BYTES) return
        val bytes = logFile.readBytes()
        val keepFrom = (bytes.size - RETAINED_LOG_BYTES).coerceAtLeast(0)
        var firstCompleteLine = keepFrom
        while (firstCompleteLine < bytes.size && bytes[firstCompleteLine] != '\n'.code.toByte()) {
            firstCompleteLine++
        }
        if (firstCompleteLine < bytes.size) firstCompleteLine++
        logFile.writeBytes(bytes.copyOfRange(firstCompleteLine, bytes.size))
    }

    companion object {
        private const val LOG_FILE_NAME = "print-diagnostics.log"
        private const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"
        private const val MAX_LOG_BYTES = 512 * 1024L
        private const val RETAINED_LOG_BYTES = 384 * 1024

        fun describeThrowable(error: Throwable): String {
            val parts = mutableListOf<String>()
            val seen = mutableSetOf<Throwable>()
            var current: Throwable? = error
            while (current != null && current !in seen && parts.size < 8) {
                seen += current
                val type = current::class.java.simpleName.ifBlank { current::class.java.name }
                val detail = current.message?.replace("\r", " ")?.replace("\n", " ")
                parts += if (detail.isNullOrBlank()) type else "$type: $detail"
                current = current.cause
            }
            return parts.joinToString(" <- ")
        }

        fun rootCauseMessage(error: Throwable): String {
            val seen = mutableSetOf<Throwable>()
            var current = error
            while (current.cause != null && current.cause !in seen) {
                seen += current
                current = current.cause!!
            }
            return current.message?.takeIf { it.isNotBlank() }
                ?: current::class.java.simpleName
                ?: "未知异常"
        }
    }
}
