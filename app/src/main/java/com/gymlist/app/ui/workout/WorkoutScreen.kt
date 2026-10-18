package com.gymlist.app.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymlist.app.data.model.Exercise
import com.gymlist.app.data.model.SetLog
import com.gymlist.app.ui.common.CompactNumberField
import com.gymlist.app.ui.common.ExerciseInfoButton
import com.gymlist.app.ui.common.rememberElapsedText
import com.gymlist.app.util.formatRest
import com.gymlist.app.util.formatRestLabel
import com.gymlist.app.util.formatWeight
import com.gymlist.app.util.playRestFinishedAlert

/**
 * El entrenamiento en curso: un bloque por ejercicio, y dentro una fila por serie con sus
 * repeticiones, su peso y el botón para marcarla. Marcar una serie arranca su descanso.
 */
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // El aviso lo lanza la pantalla, no el ViewModel, para no meterle un Context dentro.
    LaunchedEffect(viewModel) {
        viewModel.restFinished.collect { context.playRestFinishedAlert() }
    }

    val session = state.session
    if (session == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay ningún entrenamiento en curso")
        }
        return
    }

    // null = se despliega solo el ejercicio en curso. -1 = el usuario lo ha plegado todo.
    // Cualquier otro valor = la posición que ha abierto a mano.
    var manuallyExpanded by remember { mutableStateOf<Int?>(null) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val expandedPosition = manuallyExpanded ?: state.currentPosition

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            WorkoutHeader(
                routineName = session.routineName,
                elapsed = rememberElapsedText(session.startedAt),
                doneSets = state.doneSets,
                totalSets = state.totalSets,
                progress = state.progress
            )
        }

        items(state.blocks, key = { it.position }) { block ->
            ExerciseBlockCard(
                block = block,
                exercise = state.catalog[block.exerciseId],
                expanded = block.position == expandedPosition,
                onToggleExpand = {
                    manuallyExpanded = if (block.position == expandedPosition) -1 else block.position
                },
                onToggleSet = viewModel::toggleSet,
                onRepsChange = viewModel::setReps,
                onWeightChange = viewModel::setWeight,
                onAddSet = { viewModel.addSet(block) },
                onRemoveSet = { viewModel.removeLastSet(block) },
                onStartRest = { viewModel.startRest(block.restSeconds, block.name) }
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showFinishDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Finalizar entrenamiento")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { showDiscardDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Descartar entrenamiento",
                    color = MaterialTheme.colorScheme.error
                )
            }
            // Hueco para que el último botón no quede debajo de la barra de descanso.
            Spacer(Modifier.height(120.dp))
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("¿Finalizar entrenamiento?") },
            text = {
                Text(
                    if (state.allDone) {
                        "Has completado las ${state.totalSets} series. Se guardará en el calendario."
                    } else {
                        "Llevas ${state.doneSets} de ${state.totalSets} series. " +
                            "Se guardará tal cual, con lo que has hecho."
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    showFinishDialog = false
                    viewModel.finishWorkout(onFinished)
                }) { Text("Finalizar") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Seguir entrenando") }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("¿Descartar entrenamiento?") },
            text = { Text("Se borrará sin guardar nada en el calendario. No se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardWorkout(onFinished)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun WorkoutHeader(
    routineName: String,
    elapsed: String,
    doneSets: Int,
    totalSets: Int,
    progress: Float
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Entrenando", style = MaterialTheme.typography.labelMedium)
                    Text(
                        routineName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(elapsed, style = MaterialTheme.typography.headlineSmall)
                    Text("tiempo", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$doneSets de $totalSets series completadas",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ExerciseBlockCard(
    block: ExerciseBlock,
    exercise: Exercise?,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleSet: (SetLog) -> Unit,
    onRepsChange: (SetLog, String) -> Unit,
    onWeightChange: (SetLog, String) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: () -> Unit,
    onStartRest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (block.complete) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (expanded) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.animateContentSize()
    ) {
        Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompletionBadge(done = block.doneSets, total = block.sets.size)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        block.name,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (block.complete) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${block.sets.size} × ${block.targetReps} · ${formatRestLabel(block.restSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ExerciseInfoButton(exercise = exercise)
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Plegar" else "Desplegar"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(end = 8.dp)) {
                    Spacer(Modifier.height(8.dp))
                    block.sets.forEach { log ->
                        SetRow(
                            log = log,
                            onToggle = { onToggleSet(log) },
                            onRepsChange = { onRepsChange(log, it) },
                            onWeightChange = { onWeightChange(log, it) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onAddSet) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Serie")
                        }
                        Spacer(Modifier.width(8.dp))
                        if (block.sets.size > 1) {
                            IconButton(onClick = onRemoveSet) {
                                Icon(Icons.Default.Remove, contentDescription = "Quitar la última serie")
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (block.restSeconds > 0) {
                            OutlinedButton(onClick = onStartRest) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(formatRest(block.restSeconds))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Contador "2/3" que se vuelve un visto cuando el ejercicio está terminado. */
@Composable
private fun CompletionBadge(done: Int, total: Int) {
    val complete = total > 0 && done == total
    val color by animateColorAsState(
        if (complete) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "badge"
    )
    Surface(
        shape = CircleShape,
        color = color,
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (complete) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Ejercicio completado",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    "$done/$total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Una serie: número, objetivo de repes, lo que has hecho y el peso usado.
 *
 * Los dos campos guardan su texto localmente (`remember(log.id)`) para que escribir no se pelee
 * con lo que va llegando de la base de datos; el valor se envía en cada pulsación.
 */
@Composable
private fun SetRow(
    log: SetLog,
    onToggle: () -> Unit,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit
) {
    var repsText by remember(log.id) { mutableStateOf(log.reps?.toString().orEmpty()) }
    var weightText by remember(log.id) { mutableStateOf(formatWeight(log.weight)) }

    // Al marcar la serie el ViewModel puede rellenar las repes; eso hay que reflejarlo aquí.
    LaunchedEffect(log.reps) {
        val fromDb = log.reps?.toString().orEmpty()
        if (fromDb != repsText && repsText.isEmpty()) repsText = fromDb
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (log.done) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.width(56.dp)) {
                Text("Serie ${log.setNumber}", style = MaterialTheme.typography.labelMedium)
                Text(
                    "obj. ${log.targetReps}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            CompactNumberField(
                value = repsText,
                onValueChange = {
                    repsText = it
                    onRepsChange(it)
                },
                placeholder = "–",
                suffix = "reps"
            )
            Spacer(Modifier.width(8.dp))
            CompactNumberField(
                value = weightText,
                onValueChange = {
                    weightText = it
                    onWeightChange(it)
                },
                placeholder = "–",
                suffix = "kg",
                decimal = true
            )
            Spacer(Modifier.width(8.dp))
            FilledIconToggleButton(
                checked = log.done,
                onCheckedChange = { onToggle() },
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = if (log.done) {
                        "Desmarcar serie ${log.setNumber}"
                    } else {
                        "Marcar serie ${log.setNumber} como hecha"
                    }
                )
            }
        }
    }
}
