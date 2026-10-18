package com.gymlist.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val SPAIN: Locale = Locale.forLanguageTag("es-ES")

/** Segundos a "M:SS" (o "H:MM:SS" si pasa de una hora). Pensado para el cronómetro. */
fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val hours = s / 3600
    val minutes = (s % 3600) / 60
    val seconds = s % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** Duración en texto corto para tarjetas de histórico: "48 min", "1 h 12 min". */
fun formatMinutes(minutes: Long): String = when {
    minutes <= 0L -> "—"
    minutes < 60L -> "$minutes min"
    minutes % 60L == 0L -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

/** Descanso configurado, en lenguaje natural: "90 s" o "2 min". */
fun formatRest(seconds: Int): String = when {
    seconds <= 0 -> "sin descanso"
    seconds % 60 == 0 && seconds >= 60 -> "${seconds / 60} min"
    else -> "$seconds s"
}

/** Quita el ".0" de los pesos enteros: 20.0 -> "20", 22.5 -> "22,5". */
fun formatWeight(weight: Double?): String {
    if (weight == null) return ""
    return if (weight % 1.0 == 0.0) weight.toInt().toString()
    else String.format(SPAIN, "%.1f", weight)
}

fun formatVolume(volume: Double): String = when {
    volume <= 0.0 -> "—"
    volume >= 1000 -> String.format(SPAIN, "%.1f t", volume / 1000.0)
    else -> "${volume.toInt()} kg"
}

private val dayMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d 'de' MMMM", SPAIN)

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", SPAIN)

fun LocalDate.toReadable(): String {
    val weekday = dayOfWeek.getDisplayName(TextStyle.FULL, SPAIN).replaceFirstChar { it.uppercase() }
    return "$weekday, ${format(dayMonthFormatter)}"
}

fun Long.epochDayToLocalDate(): LocalDate = LocalDate.ofEpochDay(this)

fun Long.millisToLocalTimeText(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormatter)

fun Long.millisToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

/** "Hoy" / "Ayer" / fecha, para encabezados del histórico. */
fun LocalDate.relativeLabel(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Hoy"
    today.minusDays(1) -> "Ayer"
    else -> toReadable()
}
