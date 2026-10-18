package com.gymlist.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymlist.app.data.model.RoutineSummary
import com.gymlist.app.data.model.SessionSummary
import com.gymlist.app.ui.common.EmptyState
import com.gymlist.app.ui.common.SectionTitle
import com.gymlist.app.ui.common.StatPill
import com.gymlist.app.ui.common.rememberElapsedText
import com.gymlist.app.util.epochDayToLocalDate
import com.gymlist.app.util.formatMinutes
import com.gymlist.app.util.formatVolume
import com.gymlist.app.util.relativeLabel

/**
 * Pantalla de inicio: o retomas el entrenamiento que dejaste a medias, o eliges con qué rutina
 * empezar. Debajo, un resumen de cómo vas y los últimos entrenamientos.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onContinueWorkout: () -> Unit,
    onStartRoutine: (Long) -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenSession: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val active = state.activeSession
        if (active != null) {
            item {
                ActiveWorkoutCard(
                    routineName = active.routineName,
                    elapsed = rememberElapsedText(active.startedAt),
                    onContinue = onContinueWorkout
                )
            }
        }

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
                    value = state.stats.weekStreak.toString(),
                    label = if (state.stats.weekStreak == 1) "semana seguida" else "semanas seguidas",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (active == null) {
            item {
                Spacer(Modifier.height(4.dp))
                SectionTitle("Empezar entrenamiento")
            }

            if (state.routines.isEmpty() && !state.loading) {
                item {
                    EmptyState(
                        icon = Icons.Default.FitnessCenter,
                        title = "Todavía no tienes rutinas",
                        message = "Crea tu primera rutina eligiendo ejercicios del catálogo y diciendo cuántas series, repeticiones y descanso quieres en cada uno.",
                        action = {
                            Button(onClick = onOpenRoutines) { Text("Crear rutina") }
                        }
                    )
                }
            } else {
                // Las claves llevan prefijo: rutinas y sesiones conviven en la misma LazyColumn y
                // sus id son números independientes, así que "1" y "1" chocarían.
                items(state.routines, key = { "routine-${it.id}" }) { routine ->
                    RoutineStartCard(routine = routine, onStart = { onStartRoutine(routine.id) })
                }
            }
        }

        if (state.recent.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle("Últimos entrenamientos")
            }
            items(state.recent, key = { "session-${it.id}" }) { session ->
                RecentSessionRow(session = session, onClick = { onOpenSession(session.id) })
            }
        }
    }
}

@Composable
private fun ActiveWorkoutCard(
    routineName: String,
    elapsed: String,
    onContinue: () -> Unit
) {
    Card(
        onClick = onContinue,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Entrenamiento en curso", style = MaterialTheme.typography.labelMedium)
                Text(
                    routineName,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text("Llevas $elapsed", style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Continuar",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun RoutineStartCard(
    routine: RoutineSummary,
    onStart: () -> Unit
) {
    Card(
        onClick = onStart,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${routine.exerciseCount} ejercicios · ${routine.totalSets} series",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Empezar", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun RecentSessionRow(
    session: SessionSummary,
    onClick: () -> Unit
) {
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
                Text(
                    buildString {
                        append(session.dayEpoch.epochDayToLocalDate().relativeLabel())
                        append(" · ")
                        append("${session.doneSets}/${session.totalSets} series")
                        append(" · ")
                        append(formatMinutes(session.durationMinutes))
                        if (session.volume > 0) {
                            append(" · ")
                            append(formatVolume(session.volume))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
