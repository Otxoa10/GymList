package com.gymlist.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymlist.app.data.model.Exercise
import com.gymlist.app.data.plan.Focus
import com.gymlist.app.data.plan.GeneratedPlan
import com.gymlist.app.data.plan.PlanDay
import com.gymlist.app.data.plan.PlanGenerator
import com.gymlist.app.data.plan.PlannedExercise
import com.gymlist.app.ui.common.EmptyState
import com.gymlist.app.ui.common.ExerciseInfoButton
import com.gymlist.app.ui.common.Tag
import com.gymlist.app.ui.library.ExerciseThumbnail
import com.gymlist.app.util.formatRestLabel

/** El plan propuesto, día a día, antes de guardarlo. Cada ejercicio se puede cambiar por otro. */
@Composable
internal fun PlanReview(
    plan: GeneratedPlan,
    catalog: Map<String, Exercise>,
    onSwap: (dayIndex: Int, exerciseIndex: Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PlanHeader(plan) }

        item {
            Column(
                Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                plan.tips.forEach { tip -> InfoLine(icon = Icons.Default.Lightbulb, text = tip) }
                InfoLine(
                    icon = Icons.Default.SwapHoriz,
                    text = "¿No te convence algún ejercicio? Toca ⇄ para cambiarlo por otro que trabaje lo mismo."
                )
            }
        }

        if (plan.days.all { it.exercises.isEmpty() }) {
            item {
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No hay ejercicios cargados",
                    message = "El catálogo de ejercicios no está disponible en esta instalación, así que no se puede montar el plan."
                )
            }
        }

        itemsIndexed(plan.days, key = { index, _ -> "day-$index" }) { dayIndex, day ->
            PlanDayCard(
                day = day,
                catalog = catalog,
                onSwap = { exerciseIndex -> onSwap(dayIndex, exerciseIndex) }
            )
        }
    }
}

@Composable
private fun PlanHeader(plan: GeneratedPlan) {
    val answers = plan.answers
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(plan.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(plan.summary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Sugerencia: ${plan.schedule}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tag(answers.goal.title)
                Tag(answers.experience.title)
                Tag(answers.length.title)
                Tag(answers.place.title)
                if (answers.focus != Focus.BALANCED) Tag("Prioridad: ${answers.focus.title.lowercase()}")
            }
        }
    }
}

@Composable
private fun PlanDayCard(
    day: PlanDay,
    catalog: Map<String, Exercise>,
    onSwap: (exerciseIndex: Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(start = 14.dp, end = 4.dp, top = 14.dp, bottom = 8.dp)) {
            Text(day.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${day.exercises.size} ejercicios · ${day.exercises.sumOf { it.sets }} series",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            day.exercises.forEachIndexed { index, planned ->
                PlanExerciseRow(
                    planned = planned,
                    exercise = catalog[planned.exerciseId],
                    canSwap = PlanGenerator.canSwap(day, index),
                    onSwap = { onSwap(index) }
                )
            }
        }
    }
}

@Composable
private fun PlanExerciseRow(
    planned: PlannedExercise,
    exercise: Exercise?,
    canSwap: Boolean,
    onSwap: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (exercise != null) {
            ExerciseThumbnail(exercise, size = 48.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                planned.pattern.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                exercise?.name ?: planned.exerciseId,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${planned.sets} × ${planned.reps} · ${formatRestLabel(planned.restSeconds)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ExerciseInfoButton(exercise = exercise)
        IconButton(onClick = onSwap, enabled = canSwap) {
            Icon(Icons.Default.SwapHoriz, contentDescription = "Cambiar por otro ejercicio")
        }
    }
}

@Composable
internal fun ReviewActions(
    dayCount: Int,
    canSave: Boolean,
    onSave: () -> Unit,
    onChangeAnswers: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
        ) {
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Guardar plan ($dayCount rutinas)")
            }
            TextButton(onClick = onChangeAnswers, modifier = Modifier.fillMaxWidth()) {
                Text("Cambiar respuestas")
            }
        }
    }
}

@Composable
internal fun PlanSaved(
    routineCount: Int,
    canStart: Boolean,
    onStart: () -> Unit,
    onOpenRoutines: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("¡Tu plan está listo!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tus $routineCount rutinas ya están al principio de «Mis rutinas». Puedes cambiar " +
                "ejercicios, series o descansos cuando quieras.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        if (canStart) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Empezar el día 1 ahora")
            }
            Spacer(Modifier.height(10.dp))
        }
        OutlinedButton(
            onClick = onOpenRoutines,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Ver mis rutinas")
        }
        if (!canStart && routineCount > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Tienes un entrenamiento sin terminar. Finalízalo o descártalo antes de empezar el plan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
