package com.gymlist.app.util

import com.gymlist.app.data.model.SessionSummary
import java.time.DayOfWeek
import java.time.LocalDate

data class TrainingStats(
    val thisWeek: Int = 0,
    val thisMonth: Int = 0,
    val total: Int = 0,
    /** Semanas seguidas, hacia atrás, con al menos un entrenamiento. */
    val weekStreak: Int = 0
)

fun computeStats(
    sessions: List<SessionSummary>,
    today: LocalDate = LocalDate.now()
): TrainingStats {
    if (sessions.isEmpty()) return TrainingStats()

    val dates = sessions.map { it.dayEpoch.epochDayToLocalDate() }
    val mondayOfThisWeek = today.with(DayOfWeek.MONDAY)

    val weeksWithTraining = dates.map { it.with(DayOfWeek.MONDAY) }.toHashSet()

    // La racha no se rompe por estar a principio de semana sin haber ido todavía: si esta
    // semana aún está vacía, se cuenta desde la anterior.
    var cursor = if (mondayOfThisWeek in weeksWithTraining) mondayOfThisWeek
    else mondayOfThisWeek.minusWeeks(1)

    var streak = 0
    while (cursor in weeksWithTraining) {
        streak++
        cursor = cursor.minusWeeks(1)
    }

    return TrainingStats(
        thisWeek = dates.count { !it.isBefore(mondayOfThisWeek) },
        thisMonth = dates.count { it.year == today.year && it.month == today.month },
        total = sessions.size,
        weekStreak = streak
    )
}
