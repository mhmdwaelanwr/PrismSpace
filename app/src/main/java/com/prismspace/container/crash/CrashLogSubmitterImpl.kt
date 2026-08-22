package com.prismspace.container.crash

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object CrashLogSubmitterImpl {
    private const val ENGINE_CRASH_DIR = "engine_crash_logs"
    private const val ENGINE_CRASH_PENDING_DIR = "pending"
    private const val MAX_SUBMISSION_CHARS = 12000

    @JvmStatic
    fun submitPendingCrashLogs(context: Context): Int {
        val pendingDir = File(File(context.filesDir, ENGINE_CRASH_DIR), ENGINE_CRASH_PENDING_DIR)
        if (!pendingDir.exists() || !pendingDir.isDirectory) {
            return 0
        }

        val allFiles = pendingDir.listFiles() ?: return 0
        if (allFiles.isEmpty()) {
            return 0
        }

        allFiles.sortBy { it.lastModified() }

        var submittedCount = 0
        val crashlytics = FirebaseCrashlytics.getInstance()

        for (logFile in allFiles) {
            if (!logFile.isFile || !logFile.name.endsWith(".log")) {
                continue
            }
            val raw = readTextFile(logFile)
            if (raw.isNullOrBlank()) {
                continue
            }

            val packageName = parseField(raw, "package", "unknown")
            val timestamp = parseField(raw, "timestamp", "0")
            val scrubbed = scrub(raw)

            try {
                crashlytics.setCustomKey("engine_crash_file", logFile.name)
                crashlytics.setCustomKey("engine_crash_package", packageName)
                crashlytics.setCustomKey("engine_crash_timestamp", timestamp)
                crashlytics.recordException(CustomEngineException(scrubbed))
                if (logFile.delete()) {
                    submittedCount++
                }
            } catch (_: Throwable) {
                // Keep file for retry on next startup.
            }
        }

        return submittedCount
    }

    private fun readTextFile(file: File): String? {
        val builder = StringBuilder()
        return try {
            BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    builder.append(line).append('\n')
                    line = reader.readLine()
                }
            }
            builder.toString()
        } catch (_: IOException) {
            null
        }
    }

    private fun parseField(content: String, key: String, defaultValue: String): String {
        val prefix = "$key="
        val lines = content.split("\\n")
        for (line in lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length).trim { it <= ' ' }
            }
        }
        return defaultValue
    }

    private fun scrub(raw: String): String {
        var sanitized = raw
        sanitized = sanitized.replace(Regex("(?i)(bearer\\s+)[a-z0-9._\\-]+"), "$1[REDACTED]")
        sanitized = sanitized.replace(Regex("(?i)(token|secret|password|authorization)\\s*[=:]\\s*[^\\s\\n]+"), "$1=[REDACTED]")
        sanitized = sanitized.replace(Regex("/data/user/\\d+/[^\\s:]+"), "[APP_PATH]")
        sanitized = sanitized.replace(Regex("/storage/[^\\s:]+"), "[STORAGE_PATH]")
        sanitized = sanitized.replace(Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+"), "[EMAIL]")
        sanitized = sanitized.replace('\r', ' ')
        if (sanitized.length > MAX_SUBMISSION_CHARS) {
            sanitized = sanitized.substring(0, MAX_SUBMISSION_CHARS)
        }
        return sanitized
    }

    class CustomEngineException(logContent: String) : RuntimeException(logContent)
}

