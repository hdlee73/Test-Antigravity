package io.github.hdlee73.etftracker.data.log

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 마켓 외 배포 환경에서 치명적 오류·크래시 원인을 로컬 텍스트 파일에 기록하는 방어 로거.
 */
object CrashLogger {
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "etf_tracker_crash.log"
    private const val MAX_BYTES = 512 * 1024L
    private val lock = Any()

    @Volatile
    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logFatal("UNCAUGHT[${thread.name}]", throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun logFatal(tag: String, throwable: Throwable) {
        val ctx = appContext ?: return
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        appendLine(ctx, "FATAL", tag, sw.toString())
    }

    fun logWarning(tag: String, message: String, throwable: Throwable? = null) {
        val ctx = appContext ?: return
        val detail = if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            "$message\n$sw"
        } else {
            message
        }
        appendLine(ctx, "WARN", tag, detail)
    }

    fun readLog(context: Context): String {
        val file = logFile(context)
        return synchronized(lock) {
            if (!file.exists()) "기록된 오류 로그가 없습니다." else file.readText(Charsets.UTF_8)
        }
    }

    fun clearLog(context: Context) {
        val file = logFile(context)
        synchronized(lock) {
            if (file.exists()) file.writeText("", Charsets.UTF_8)
        }
    }

    fun getLogFile(context: Context): File = logFile(context)

    private fun logFile(context: Context): File {
        val dir = File(context.filesDir, LOG_DIR).apply { if (!exists()) mkdirs() }
        return File(dir, LOG_FILE)
    }

    private fun appendLine(context: Context, level: String, tag: String, body: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = buildString {
            appendLine("=== [$timestamp] $level / $tag ===")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})")
            appendLine(body.trimEnd())
            appendLine()
        }
        synchronized(lock) {
            val file = logFile(context)
            if (file.exists() && file.length() > MAX_BYTES) {
                val tail = file.readText(Charsets.UTF_8).takeLast((MAX_BYTES / 2).toInt())
                file.writeText("[...이전 로그 절삭됨...]\n$tail", Charsets.UTF_8)
            }
            file.appendText(entry, Charsets.UTF_8)
        }
    }
}
