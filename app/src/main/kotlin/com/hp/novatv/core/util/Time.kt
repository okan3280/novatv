package com.hp.novatv.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val TR = Locale("tr", "TR")
private val tz: TimeZone get() = TimeZone.getDefault()

private fun fmt(pattern: String) = SimpleDateFormat(pattern, TR).apply { timeZone = tz }

fun Long.asTime(): String = fmt("HH:mm").format(Date(this))

fun Long.asTimeSec(): String = fmt("HH:mm:ss").format(Date(this))

fun Long.asDateTime(): String = fmt("d MMMM yyyy, HH:mm").format(Date(this))

fun Long.asDate(): String = fmt("d MMM yyyy").format(Date(this))

fun Long.asShortDate(): String = fmt("dd.MM.yyyy").format(Date(this))

/** "Bugün / Dün / 3 gün önce" gibi goreli etiket. */
fun Long.relativeDayLabel(now: Long = System.currentTimeMillis()): String {
    val days = daysBetween(now, this)
    return when {
        days == 0L -> "Bugün"
        days == 1L -> "Dün"
        days == -1L -> "Yarın"
        days > 1 -> "$days gün önce"
        else -> "${-days} gün sonra"
    }
}

/** Iki zaman damgasi arasindaki gun farki (takvim gunu bazli). */
fun daysBetween(from: Long, to: Long): Long {
    val a = Calendar.getInstance(tz).apply {
        timeInMillis = from
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val b = Calendar.getInstance(tz).apply {
        timeInMillis = to
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return ((b.timeInMillis - a.timeInMillis) / 86_400_000L)
}

/** Gunun 00:00 epoch degeri. */
fun startOfDay(epoch: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance(tz).apply {
        timeInMillis = epoch
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** 23:59:59 epoch degeri. */
fun endOfDay(epoch: Long = System.currentTimeMillis()): Long = startOfDay(epoch) + 86_400_000L - 1

/** Boyut bicimlendirme. */
fun Long.asFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = this.toDouble()
    var i = 0
    while (value >= 1024 && i < units.lastIndex) {
        value /= 1024
        i++
    }
    return if (i == 0) "${value.toInt()} ${units[i]}"
    else String.format(TR, "%.1f %s", value, units[i])
}

/** sure bicimlendirme: 1:05:03 veya 05:03. */
fun Long.asDuration(): String {
    val total = this / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(TR, "%d:%02d:%02d", h, m, s)
    else String.format(TR, "%02d:%02d", m, s)
}

/** Yayin suresi tahmini: simdi - baslangic. */
fun programElapsed(now: Long, start: Long, end: Long): Float {
    val total = (end - start).toFloat()
    if (total <= 0f) return 0f
    return ((now - start).toFloat() / total).coerceIn(0f, 1f)
}

/** Kanal adi temizleme: "TR: | Spor TV | HD" -> "Spor TV". */
fun String.cleanChannelName(): String {
    var s = this.trim()
    val prefixes = listOf("TR: ", "TR:", "UK: ", "DE: ", "FR: ", "NL: ", "US: ")
    for (p in prefixes) {
        if (s.startsWith(p, ignoreCase = true)) {
            s = s.substring(p.length).trim()
            break
        }
    }
    return s.split("|").joinToString(" | ") { it.trim() }.ifBlank { this.trim() }
}
