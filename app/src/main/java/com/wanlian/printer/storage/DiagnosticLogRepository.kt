package com.wanlian.printer.storage

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small persistent log intended for field diagnosis without requiring Android Logcat. */
class DiagnosticLogRepository internal constructor(private val filesDir: File) {
    constructor(context: Context) : this(context.applicationContext.filesDir)
    private val logDirectory = File(filesDir, "diagnostic-logs")
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
    private fun fileFor(date: String): File {
        require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        return File(logDirectory, "$date.log")
    }

    init {
        // Preserve old records by their original date; keep the original as a backup.
        val legacy = File(filesDir, LOG_FILE_NAME)
        if (legacy.exists()) runCatching {
            logDirectory.mkdirs()
            legacy.readLines(Charsets.UTF_8).filter { it.isNotBlank() }
                .groupBy { it.take(10).takeIf { d -> d.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) } ?: today() }
                .forEach { (date, lines) ->
                    val target = fileFor(date)
                    val existing = if (target.exists()) target.readLines(Charsets.UTF_8) else emptyList()
                    target.writeText((existing + lines).distinct().sorted().joinToString("\n", postfix = "\n"), Charsets.UTF_8)
                }
            legacy.renameTo(File(filesDir, "$LOG_FILE_NAME.migrated"))
        }
    }

    @Synchronized
    fun dates(): List<String> = (logDirectory.listFiles().orEmpty()
        .filter { it.name.matches(Regex("\\d{4}-\\d{2}-\\d{2}\\.log")) }
        .map { it.nameWithoutExtension } + today()).distinct().sortedDescending()

    @Synchronized
    fun append(category: String, message: String, error: Throwable? = null) {
        runCatching {
            val logFile = fileFor(today())
            logFile.parentFile?.mkdirs()
            val timestamp = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.CHINA).format(Date())
            val normalizedMessage = message.replace("\r", " ").replace("\n", " | ")
            val errorSuffix = error?.let { " | exception=${describeThrowable(it)}" }.orEmpty()
            logFile.appendText("$timestamp | $category | $normalizedMessage$errorSuffix\n", Charsets.UTF_8)
            trimIfNeeded(logFile)
        }
    }

    @Synchronized
    fun read(date: String = today()): String = runCatching {
        val logFile = fileFor(date)
        if (logFile.exists()) logFile.readLines(Charsets.UTF_8).filter { it.isNotBlank() }.asReversed().joinToString("\n") else ""
    }.getOrDefault("")

    @Synchronized
    fun clear(date: String = today()) {
        runCatching {
            val logFile = fileFor(date)
            if (logFile.exists()) logFile.writeText("", Charsets.UTF_8)
        }
    }

    private fun trimIfNeeded(logFile: File) {
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
