package com.gymlist.app.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymlist.app.data.model.SessionSummary
import com.gymlist.app.ui.common.EmptyState
import com.gymlist.app.ui.common.SectionTitle
import com.gymlist.app.ui.common.StatPill
import com.gymlist.app.util.SPAIN
import com.gymlist.app.util.formatMinutes
import com.gymlist.app.util.formatVolume
import com.gymlist.app.util.millisToLocalTimeText
import com.gymlist.app.util.relativeLabel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

/**
 * Seguimiento: un mes de un vistazo, con los días entrenados marcados, y debajo el detalle del
 * día que se toque.
 */
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onOpenSession: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }

    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    val daysWithTraining = remember(state.byDay) { state.byDay.keys }
    val selected = selectedDay
    val sessionsForDetail = when {
        selected != null -> state.sessionsOn(selected)
        else -> state.byDay
            .filterKeys { YearMonth.from(it) == visibleMonth }
            .values
            .flatten()
            .sortedByDescending { it.startedAt }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(
                    value = state.stats.thisWeek.toString(),
                    label = "esta semana",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    value = state.stats.thisMonth.toString(),
                    label = "este mes",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    value = state.stats.total.toString(),
                    label = "en total",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            MonthCard(
                month = visibleMonth,
                today = today,
                selectedDay = selected,
                daysWithTraining = daysWithTraining,
                onPreviousMonth = {
                    visibleMonth = visibleMonth.minusMonths(1)
                    selectedDay = null
                },
                onNextMonth = {
                    visibleMonth = visibleMonth.plusMonths(1)
                    selectedDay = null
                },
                onDayClick = { day ->
                    selectedDay = if (selectedDay == day) null else day
                }
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(
                    text = selected?.relativeLabel(today) ?: "Entrenamientos del mes",
                    modifier = Modifier.weight(1f)
                )
                if (selected != null) {
                    TextButton(onClick = { selectedDay = null }) { Text("Ver el mes") }
                }
            }
        }

        if (sessionsForDetail.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.CalendarMonth,
                    title = if (selected != null) "Ese día no entrenaste" else "Mes sin entrenamientos",
                    message = if (selected != null) {
                        "Los días que entrenes aparecerán marcados en verde en el calendario."
                    } else {
                        "Cuando termines un entrenamiento se guardará aquí con las series que hayas hecho."
                    }
                )
            }
        }

        items(sessionsForDetail, key = { it.id }) { session ->
            SessionCard(session = session, onClick = { onOpenSession(session.id) })
        }
    }
}

@Composable
private fun MonthCard(
    month: YearMonth,
    today: LocalDate,
    selectedDay: LocalDate?,
    daysWithTraining: Set<LocalDate>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                }
                Text(
                    month.monthTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
                }
            }

            Spacer(Modifier.height(4.dp))

            // Semana que empieza en lunes, como el calendario de aquí.
            Row(Modifier.fillMaxWidth()) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { label ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            month.toWeekRows().forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (day == null) {
                                Spacer(Modifier.size(42.dp))
                            } else {
                                DayCell(
                                    day = day,
                                    trained = day in daysWithTraining,
                                    isToday = day == today,
                                    isSelected = day == selectedDay,
                                    onClick = { onDayClick(day) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 6.dp)
            ) {
                Box(
                    Modifier
                        .size(11.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "día entrenado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(14.dp))
                Box(
                    Modifier
                        .size(11.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "hoy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    trained: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        trained -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val contentColor = when {
        trained -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        contentColor = contentColor,
        // El día seleccionado y el día de hoy se distinguen con un aro, no con el relleno,
        // para que no choque con la marca de "día entrenado".
        border = when {
            isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
            isToday -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            else -> null
        },
        modifier = Modifier
            .padding(2.dp)
            .size(40.dp)
            .aspectRatio(1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SessionCard(session: SessionSummary, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(session.routineName, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${session.startedAt.millisToLocalTimeText()} · " +
                        "${session.doneSets}/${session.totalSets} series · " +
                        formatMinutes(session.durationMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.volume > 0) {
                    Text(
                        "Volumen: ${formatVolume(session.volume)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun YearMonth.monthTitle(): String {
    val name = month.getDisplayName(TextStyle.FULL, SPAIN).replaceFirstChar { it.uppercase() }
    return "$name $year"
}

/**
 * Convierte el mes en filas de 7 huecos empezando en lunes. Los huecos de antes del día 1 y
 * después del último van a null.
 */
private fun YearMonth.toWeekRows(): List<List<LocalDate?>> {
    val firstDay = atDay(1)
    // DayOfWeek.value: lunes = 1 … domingo = 7.
    val leading = firstDay.dayOfWeek.value - 1
    val cells = buildList<LocalDate?> {
        repeat(leading) { add(null) }
        (1..lengthOfMonth()).forEach { add(atDay(it)) }
        while (size % 7 != 0) add(null)
    }
    return cells.chunked(7)
}
